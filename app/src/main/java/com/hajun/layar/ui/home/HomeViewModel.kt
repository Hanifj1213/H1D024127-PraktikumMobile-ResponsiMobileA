package com.hajun.layar.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hajun.layar.data.ShowRepository
import com.hajun.layar.data.userMessage
import com.hajun.layar.model.Show
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel(
    private val repository: ShowRepository,
    private val savedStateHandle: SavedStateHandle,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState(
        query = savedStateHandle["query"] ?: "",
        selectedGenre = savedStateHandle["genre"],
    ))
    val uiState: StateFlow<HomeUiState> = state.asStateFlow()
    private var requestJob: Job? = null
    private var requestVersion = 0L
    private var genreVersion = 0L
    private var catalog: List<Show>? = null
    private var activeShows: List<Show> = emptyList()
    private var nextApiPage = 0
    private var catalogComplete = false

    init { searchNow() }

    fun onQueryChanged(query: String) {
        savedStateHandle["query"] = query
        state.value = state.value.copy(query = query)
        load(debounce = true)
    }

    fun searchNow() { load(debounce = false) }
    fun retry() { searchNow() }

    fun onGenreSelected(genre: String?) {
        val normalized = genre?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == state.value.selectedGenre) return
        genreVersion++
        savedStateHandle["genre"] = normalized
        state.value = state.value.copy(selectedGenre = normalized, pageError = null, pageNotice = null)
        publish(pageIndex = 0, moveToResults = true)
    }

    fun previousPage() {
        val current = state.value
        if (current.isLoading || current.isPageLoading || !current.hasPreviousPage) return
        state.value = current.copy(pageError = null, pageNotice = null)
        publish(current.pageIndex - 1, moveToResults = true)
    }

    fun nextPage() {
        val current = state.value
        if (current.isLoading || current.isPageLoading || !current.hasNextPage) return
        if (current.pageIndex + 1 < current.pageCount) {
            state.value = current.copy(pageError = null, pageNotice = null)
            publish(current.pageIndex + 1, moveToResults = true)
        } else if (current.submittedQuery.isEmpty() && !catalogComplete) {
            fetchNextBatch()
        }
    }

    fun retryPage() {
        if (state.value.pageError != null) nextPage()
    }

    fun shuffleSuggestions() {
        if (state.value.isLoading) return
        state.value = state.value.copy(suggestions = chooseSuggestions(state.value.suggestions))
    }

    private fun load(debounce: Boolean) {
        val version = ++requestVersion
        requestJob?.cancel()
        val query = state.value.query.trim()
        activeShows = emptyList()
        state.value = HomeUiState(
            query = state.value.query,
            submittedQuery = query,
            selectedGenre = state.value.selectedGenre,
            isLoading = true,
            pageChangeToken = state.value.pageChangeToken,
        )
        requestJob = viewModelScope.launch {
            try {
                if (debounce) delay(450)
                if (query.isEmpty()) {
                    if (catalog == null) {
                        val page = repository.browsePage(0)
                        coroutineContext.ensureActive()
                        if (version != requestVersion) return@launch
                        catalog = page.shows.distinctBy { it.id }
                        catalogComplete = page.endReached
                        nextApiPage = 1
                    }
                    activeShows = catalog.orEmpty()
                } else {
                    val shows = repository.searchShows(query)
                    coroutineContext.ensureActive()
                    if (version != requestVersion) return@launch
                    activeShows = shows.distinctBy { it.id }
                }
                state.value = state.value.copy(isLoading = false, suggestions = chooseSuggestions())
                publish(pageIndex = 0)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                coroutineContext.ensureActive()
                if (version == requestVersion) {
                    state.value = state.value.copy(isLoading = false, error = failure.userMessage())
                }
            }
        }
    }

    private fun fetchNextBatch() {
        val version = ++requestVersion
        val filterVersion = genreVersion
        val oldCount = state.value.filteredCount
        val oldIndex = state.value.pageIndex
        val pageNumber = nextApiPage
        state.value = state.value.copy(isPageLoading = true, pageError = null, pageNotice = null)
        requestJob = viewModelScope.launch {
            try {
                // One explicit action fetches one API batch; never scan until a genre matches.
                val page = repository.browsePage(pageNumber)
                coroutineContext.ensureActive()
                if (version != requestVersion) return@launch
                catalog = (catalog.orEmpty() + page.shows).distinctBy { it.id }
                activeShows = catalog.orEmpty()
                catalogComplete = page.endReached
                nextApiPage = pageNumber + 1
                val newCount = filteredShows().size
                val sameFilter = filterVersion == genreVersion
                val advance = sameFilter && oldCount > 0 && oldCount % PAGE_SIZE == 0 && newCount > oldCount
                val notice = when {
                    page.endReached -> "Akhir katalog TVmaze sudah tercapai."
                    sameFilter && newCount == oldCount -> "Belum ada tambahan yang cocok. Tekan Berikutnya untuk memeriksa bagian katalog selanjutnya."
                    sameFilter && !advance -> "Halaman ini dilengkapi dengan data baru. Tekan Berikutnya untuk melanjutkan."
                    else -> null
                }
                state.value = state.value.copy(
                    isPageLoading = false,
                    pageNotice = notice,
                    suggestions = state.value.suggestions.ifEmpty { chooseSuggestions() },
                )
                publish(
                    pageIndex = if (!sameFilter) 0 else if (advance) oldIndex + 1 else oldIndex,
                    moveToResults = sameFilter && newCount > oldCount,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                coroutineContext.ensureActive()
                if (version == requestVersion) {
                    state.value = state.value.copy(isPageLoading = false, pageError = failure.userMessage())
                }
            }
        }
    }

    private fun filteredShows(): List<Show> {
        val genre = state.value.selectedGenre ?: return activeShows
        return activeShows.filter { show -> show.genres.any { it.trim().equals(genre, ignoreCase = true) } }
    }

    private fun publish(pageIndex: Int, moveToResults: Boolean = false) {
        val current = state.value
        val filtered = filteredShows()
        val pageCount = ((filtered.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val index = pageIndex.coerceIn(0, pageCount - 1)
        val genres = (activeShows.flatMap { it.genres } + listOfNotNull(current.selectedGenre))
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(java.util.Locale.ROOT) }.sorted()
        state.value = current.copy(
            shows = filtered.drop(index * PAGE_SIZE).take(PAGE_SIZE),
            pageIndex = index,
            pageCount = pageCount,
            loadedCount = activeShows.size,
            filteredCount = filtered.size,
            availableGenres = genres,
            hasPreviousPage = index > 0,
            hasNextPage = index + 1 < pageCount || (current.submittedQuery.isEmpty() && !catalogComplete),
            isCatalogComplete = current.submittedQuery.isEmpty() && catalogComplete,
            pageChangeToken = current.pageChangeToken + if (moveToResults) 1 else 0,
        )
    }

    private fun chooseSuggestions(previous: List<String> = emptyList()): List<String> {
        val titles = activeShows.map { it.title.trim() }.filter { it.isNotEmpty() }.distinct()
        val chosen = titles.shuffled(random).take(3).toMutableList()
        if (titles.size > chosen.size && chosen.toSet() == previous.toSet()) {
            chosen[chosen.lastIndex] = titles.filterNot { it in chosen }.random(random)
        }
        return chosen
    }

    companion object {
        const val PAGE_SIZE = 20

        fun factory(repository: ShowRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository, createSavedStateHandle()) }
        }
    }
}
