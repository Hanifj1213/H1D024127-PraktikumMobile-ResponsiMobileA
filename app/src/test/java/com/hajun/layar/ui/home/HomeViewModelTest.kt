package com.hajun.layar.ui.home

import androidx.lifecycle.SavedStateHandle
import com.hajun.layar.model.Show
import com.hajun.layar.testing.FakeShowRepository
import com.hajun.layar.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun initialBrowseDisplaysOnlyTwentyLoadedShows() = runTest {
        val repository = FakeShowRepository().apply {
            browse = { (1..45).map { Show(it, "Show $it") } }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()

        assertEquals((1..20).toList(), viewModel.uiState.value.shows.map { it.id })
    }

    @Test
    fun initialBrowseDeduplicatesIdsBeforeTakingTheVisiblePage() = runTest {
        val repository = FakeShowRepository().apply {
            browse = { listOf(Show(1, "First"), Show(1, "Duplicate")) + (2..30).map { Show(it, "Show $it") } }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()

        assertEquals((1..20).toList(), viewModel.uiState.value.shows.map { it.id })
        assertEquals("First", viewModel.uiState.value.shows.first().title)
    }

    @Test
    fun typingSearchWaitsExactlyFourHundredFiftyMilliseconds() = runTest {
        val repository = FakeShowRepository()
        val handle = SavedStateHandle()
        val viewModel = HomeViewModel(repository, handle)
        runCurrent()

        viewModel.onQueryChanged("  dark  ")
        runCurrent()
        advanceTimeBy(449)
        runCurrent()
        assertTrue(repository.searchQueries.isEmpty())
        assertEquals("  dark  ", handle.get<String>("query"))
        assertEquals("  dark  ", viewModel.uiState.value.query)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(listOf("dark"), repository.searchQueries)
        assertEquals("dark", viewModel.uiState.value.submittedQuery)
        assertEquals("dark", viewModel.uiState.value.shows.single().title)
    }

    @Test
    fun submitSkipsDebounceWithoutDuplicatingScheduledSearch() = runTest {
        val repository = FakeShowRepository()
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("dark")
        runCurrent()
        viewModel.searchNow()
        runCurrent()

        assertEquals(listOf("dark"), repository.searchQueries)
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("dark"), repository.searchQueries)
    }

    @Test
    fun savedQueryIsSearchedImmediatelyAfterRecreation() = runTest {
        val repository = FakeShowRepository()
        val viewModel = HomeViewModel(repository, SavedStateHandle(mapOf("query" to "  restored  ")))
        assertEquals("  restored  ", viewModel.uiState.value.query)
        runCurrent()

        assertEquals(0, repository.browseCalls)
        assertEquals(listOf("restored"), repository.searchQueries)
        assertEquals("restored", viewModel.uiState.value.submittedQuery)
    }

    @Test
    fun clearingQueryReturnsToBrowseWithEmptySubmittedQuery() = runTest {
        val repository = FakeShowRepository()
        val viewModel = HomeViewModel(repository, SavedStateHandle(mapOf("query" to "dark")))
        runCurrent()
        viewModel.onQueryChanged(" \t ")
        viewModel.searchNow()
        runCurrent()

        assertEquals(1, repository.browseCalls)
        assertEquals(listOf("dark"), repository.searchQueries)
        assertEquals("", viewModel.uiState.value.submittedQuery)
        assertEquals("Browse", viewModel.uiState.value.shows.single().title)
    }

    @Test
    fun inputChangeCancelsActiveSearchBeforeNextDebounce() = runTest {
        var cancelled = false
        val repository = FakeShowRepository().apply {
            search = {
                try { kotlinx.coroutines.awaitCancellation() }
                finally { cancelled = true }
            }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("old")
        viewModel.searchNow()
        runCurrent()
        viewModel.onQueryChanged("new")
        runCurrent()

        assertTrue(cancelled)
        assertEquals(listOf("old"), repository.searchQueries)
        assertNull(viewModel.uiState.value.error)
        viewModel.onQueryChanged("")
        viewModel.searchNow()
        runCurrent()
    }

    @Test
    fun lateNonCooperativeResponseCannotOverwriteLatestResults() = runTest {
        val repository = FakeShowRepository().apply {
            search = { query ->
                if (query == "old") kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.delay(1000)
                    listOf(Show(3, "Stale"))
                } else listOf(Show(4, "Latest"))
            }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("old")
        viewModel.searchNow()
        runCurrent()
        viewModel.onQueryChanged("new")
        viewModel.searchNow()
        runCurrent()
        assertEquals("Latest", viewModel.uiState.value.shows.single().title)
        advanceTimeBy(1000)
        runCurrent()

        assertEquals("new", viewModel.uiState.value.submittedQuery)
        assertEquals("Latest", viewModel.uiState.value.shows.single().title)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedSearchShowsFriendlyOfflineErrorAndRetryRunsImmediately() = runTest {
        val repository = FakeShowRepository().apply { search = { throw java.net.UnknownHostException("secret host") } }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("dark")
        viewModel.searchNow()
        runCurrent()

        assertEquals("Koneksi tidak tersedia. Periksa internet kamu, lalu coba lagi.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        repository.search = { emptyList() }
        viewModel.retry()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        runCurrent()

        assertEquals(listOf("dark", "dark"), repository.searchQueries)
        assertEquals(emptyList<Show>(), viewModel.uiState.value.shows)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun latestEditResetsDebounceWindow() = runTest {
        val repository = FakeShowRepository()
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("da")
        runCurrent()
        advanceTimeBy(300)
        viewModel.onQueryChanged("dark")
        runCurrent()
        advanceTimeBy(449)
        runCurrent()
        assertTrue(repository.searchQueries.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("dark"), repository.searchQueries)
    }

    @Test
    fun lateFailureCannotReplaceNewerSuccessfulSearch() = runTest {
        val repository = FakeShowRepository().apply {
            search = { query ->
                if (query == "old") kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.delay(1000)
                    throw java.io.IOException("stale error")
                } else listOf(Show(4, "Latest"))
            }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        viewModel.onQueryChanged("old")
        viewModel.searchNow()
        runCurrent()
        viewModel.onQueryChanged("new")
        viewModel.searchNow()
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        assertEquals("Latest", viewModel.uiState.value.shows.single().title)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedInitialBrowseCanRetryWithoutSearchingEmptyQuery() = runTest {
        val repository = FakeShowRepository().apply { browse = { throw java.io.IOException("offline") } }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        repository.browse = { emptyList() }
        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.browseCalls)
        assertTrue(repository.searchQueries.isEmpty())
        assertTrue(viewModel.uiState.value.shows.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun clearingViewModelCancelsActiveRequest() = runTest {
        var cancelled = false
        val repository = FakeShowRepository().apply {
            browse = {
                try { kotlinx.coroutines.awaitCancellation() }
                finally { cancelled = true }
            }
        }
        val viewModel = HomeViewModel(repository, SavedStateHandle())
        val store = androidx.lifecycle.ViewModelStore()
        store.put("home", viewModel)
        runCurrent()
        store.clear()
        runCurrent()

        assertTrue(cancelled)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun factoryRequiresSavedStateCreationExtras() {
        assertThrows(IllegalArgumentException::class.java) {
            HomeViewModel.factory(FakeShowRepository()).create(
                HomeViewModel::class.java,
                androidx.lifecycle.viewmodel.CreationExtras.Empty,
            )
        }
    }

    @Test
    fun initialBrowseTransitionsFromLoadingToSuccess() = runTest {
        val response = CompletableDeferred<List<Show>>()
        val repository = FakeShowRepository().apply { browse = { response.await() } }
        val viewModel = HomeViewModel(repository, SavedStateHandle())

        assertTrue(viewModel.uiState.value.isLoading)
        runCurrent()
        assertEquals(1, repository.browseCalls)
        assertTrue(viewModel.uiState.value.isLoading)
        response.complete(listOf(Show(8, "Fresh")))
        runCurrent()

        assertEquals(listOf(Show(8, "Fresh")), viewModel.uiState.value.shows)
        assertEquals("", viewModel.uiState.value.submittedQuery)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }
}
