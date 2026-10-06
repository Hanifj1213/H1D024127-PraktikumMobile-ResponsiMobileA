package com.hajun.layar.ui.home

import androidx.lifecycle.SavedStateHandle
import com.hajun.layar.data.BrowsePage
import com.hajun.layar.model.Show
import com.hajun.layar.testing.FakeShowRepository
import com.hajun.layar.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HomePaginationTest {
    @get:Rule val main = MainDispatcherRule()

    private fun shows(range: IntRange, genre: String = "Drama") =
        range.map { Show(it, "Title $it", genres = listOf(genre)) }

    @Test fun cachedPagesHaveTwentyItemsAndPreviousDoesNotFetch() = runTest {
        val repo = FakeShowRepository().apply { browse = { shows(1..45) } }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        assertEquals(45, vm.uiState.value.loadedCount)
        assertEquals(3, vm.uiState.value.pageCount)
        assertFalse(vm.uiState.value.hasPreviousPage)
        vm.nextPage()
        assertEquals((21..40).toList(), vm.uiState.value.shows.map { it.id })
        vm.nextPage()
        assertEquals((41..45).toList(), vm.uiState.value.shows.map { it.id })
        vm.previousPage()
        assertEquals(1, vm.uiState.value.pageIndex)
        assertEquals(listOf(0), repo.browsePages)
    }

    @Test fun partialPageIsFilledBeforeAdvancingAndDuplicatesAreRemoved() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { page -> BrowsePage(if (page == 0) shows(1..25) else shows(25..50)) }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        vm.nextPage()
        vm.nextPage() // Must not start a second overlapping request.
        runCurrent()
        assertEquals(listOf(0, 1), repo.browsePages)
        assertEquals(50, vm.uiState.value.loadedCount)
        assertEquals(1, vm.uiState.value.pageIndex)
        assertEquals((21..40).toList(), vm.uiState.value.shows.map { it.id })
        vm.nextPage()
        assertEquals((41..50).toList(), vm.uiState.value.shows.map { it.id })
    }

    @Test fun fullLastPageAdvancesWhenNextBatchArrives() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { page -> BrowsePage(shows(if (page == 0) 1..20 else 21..40)) }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertEquals(1, vm.uiState.value.pageIndex)
        assertEquals(21, vm.uiState.value.shows.first().id)
        assertEquals(2, repo.browsePages.size)
    }

    @Test fun filterUsesAllLoadedShowsAndResetsPageWithoutNetwork() = runTest {
        val handle = SavedStateHandle()
        val repo = FakeShowRepository().apply { browse = { shows(1..25) + shows(26..50, "Comedy") } }
        val vm = HomeViewModel(repo, handle)
        runCurrent()
        vm.nextPage()
        vm.onGenreSelected("Comedy")
        assertEquals(0, vm.uiState.value.pageIndex)
        assertEquals(25, vm.uiState.value.filteredCount)
        assertEquals(50, vm.uiState.value.loadedCount)
        assertEquals((26..45).toList(), vm.uiState.value.shows.map { it.id })
        assertEquals("Comedy", handle.get<String>("genre"))
        assertEquals(listOf("Comedy", "Drama"), vm.uiState.value.availableGenres)
        vm.onGenreSelected(null)
        assertEquals(50, vm.uiState.value.filteredCount)
        assertEquals(listOf(0), repo.browsePages)
    }

    @Test fun absentGenreAndEmptyApiBatchNeverTriggerAutomaticScanning() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { page -> BrowsePage(if (page == 0) shows(1..20) else emptyList()) }
        }
        val vm = HomeViewModel(repo, SavedStateHandle(mapOf("genre" to "Comedy")))
        runCurrent()
        assertTrue(vm.uiState.value.shows.isEmpty())
        assertTrue(vm.uiState.value.availableGenres.contains("Comedy"))
        assertEquals(listOf(0), repo.browsePages)
        vm.nextPage()
        runCurrent()
        assertEquals(listOf(0, 1), repo.browsePages)
        assertTrue(vm.uiState.value.hasNextPage)
        assertNotNull(vm.uiState.value.pageNotice)
        vm.nextPage()
        runCurrent()
        assertEquals(listOf(0, 1, 2), repo.browsePages)
    }

    @Test fun endOfCatalogKeepsDataAndDisablesNext() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { page -> if (page == 0) BrowsePage(shows(1..20)) else BrowsePage(emptyList(), true) }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertEquals(20, vm.uiState.value.shows.size)
        assertTrue(vm.uiState.value.isCatalogComplete)
        assertFalse(vm.uiState.value.hasNextPage)
        vm.nextPage()
        runCurrent()
        assertEquals(listOf(0, 1), repo.browsePages)
    }

    @Test fun failedPagePreservesResultsAndRetryUsesSameApiPage() = runTest {
        var fail = true
        val repo = FakeShowRepository().apply {
            browsePageResult = { page ->
                if (page == 0) BrowsePage(shows(1..20))
                else if (fail) throw IOException("offline") else BrowsePage(shows(21..35))
            }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertNotNull(vm.uiState.value.pageError)
        assertNull(vm.uiState.value.error)
        assertEquals(20, vm.uiState.value.shows.size)
        fail = false
        vm.retryPage()
        runCurrent()
        assertEquals(listOf(0, 1, 1), repo.browsePages)
        assertNull(vm.uiState.value.pageError)
        assertEquals(21, vm.uiState.value.shows.first().id)
    }

    @Test fun searchPagesKeepRelevanceAndNeverFetchBrowsePages() = runTest {
        val repo = FakeShowRepository().apply { search = { shows(1..45).reversed() } }
        val vm = HomeViewModel(repo, SavedStateHandle(mapOf("query" to "title")))
        runCurrent()
        assertEquals((26..45).reversed().toList(), vm.uiState.value.shows.map { it.id })
        vm.nextPage()
        vm.nextPage()
        assertEquals((1..5).reversed().toList(), vm.uiState.value.shows.map { it.id })
        assertFalse(vm.uiState.value.hasNextPage)
        vm.nextPage()
        assertTrue(repo.browsePages.isEmpty())
        assertEquals(listOf("title"), repo.searchQueries)
    }

    @Test fun latePageSuccessAndFailureCannotOverwriteNewSearch() = runTest {
        for (shouldFail in listOf(false, true)) {
            val repo = FakeShowRepository().apply {
                browsePageResult = { page ->
                    if (page == 0) BrowsePage(shows(1..20)) else withContext(NonCancellable) {
                        delay(1000)
                        if (shouldFail) throw IOException("stale")
                        BrowsePage(shows(21..40))
                    }
                }
                search = { listOf(Show(99, "New result")) }
            }
            val vm = HomeViewModel(repo, SavedStateHandle())
            runCurrent()
            vm.nextPage()
            runCurrent()
            vm.onQueryChanged("new")
            vm.searchNow()
            runCurrent()
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(listOf(99), vm.uiState.value.shows.map { it.id })
            assertNull(vm.uiState.value.pageError)
            assertFalse(vm.uiState.value.isPageLoading)
        }
    }

    @Test fun genreChangeDuringPageRequestKeepsFirstFilteredPage() = runTest {
        val response = CompletableDeferred<BrowsePage>()
        val repo = FakeShowRepository().apply {
            browsePageResult = { if (it == 0) BrowsePage(shows(1..20)) else response.await() }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        vm.onGenreSelected("Comedy")
        response.complete(BrowsePage(shows(21..60, "Comedy")))
        runCurrent()
        assertEquals(0, vm.uiState.value.pageIndex)
        assertEquals("Comedy", vm.uiState.value.selectedGenre)
        assertTrue(vm.uiState.value.shows.all { "Comedy" in it.genres })
    }

    @Test fun suggestionsAreRealUniqueStableAndSeededWithNoNetworkOnShuffle() = runTest {
        val repo = FakeShowRepository().apply { browse = { shows(1..45) + Show(99, " ") } }
        val vm = HomeViewModel(repo, SavedStateHandle(), Random(10))
        val other = HomeViewModel(FakeShowRepository().apply { browse = repo.browse }, SavedStateHandle(), Random(10))
        runCurrent()
        val initial = vm.uiState.value.suggestions
        assertEquals(3, initial.size)
        assertEquals(initial, other.uiState.value.suggestions)
        assertEquals(3, initial.distinct().size)
        assertTrue(initial.all { title -> shows(1..45).any { it.title == title } })
        vm.nextPage()
        vm.onGenreSelected("Drama")
        assertEquals(initial, vm.uiState.value.suggestions)
        vm.shuffleSuggestions()
        assertNotEquals(initial.toSet(), vm.uiState.value.suggestions.toSet())
        assertEquals(listOf(0), repo.browsePages)
    }

    @Test fun searchResetAndClearReuseLoadedCatalogWithoutFetchingAgain() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { BrowsePage(shows(if (it == 0) 1..20 else 21..45)) }
            search = { shows(100..130) }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        assertEquals(1, vm.uiState.value.pageIndex)
        vm.onQueryChanged("new")
        vm.searchNow()
        runCurrent()
        assertEquals(0, vm.uiState.value.pageIndex)
        assertEquals(31, vm.uiState.value.loadedCount)
        vm.onQueryChanged("")
        vm.searchNow()
        runCurrent()
        assertEquals(45, vm.uiState.value.loadedCount)
        assertEquals(listOf(0, 1), repo.browsePages)
        assertEquals(1, vm.uiState.value.shows.first().id)
    }

    @Test fun inputChangeImmediatelyCancelsPendingPageLoad() = runTest {
        var cancelled = false
        val repo = FakeShowRepository().apply {
            browsePageResult = { page ->
                if (page == 0) BrowsePage(shows(1..20)) else {
                    try { kotlinx.coroutines.awaitCancellation() }
                    finally { cancelled = true }
                }
            }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.nextPage()
        runCurrent()
        vm.onQueryChanged("new")
        runCurrent()
        assertTrue(cancelled)
        assertFalse(vm.uiState.value.isPageLoading)
        assertNull(vm.uiState.value.pageError)
        vm.searchNow()
        runCurrent()
    }

    @Test fun newMatchingBatchIsIncludedInWholeLoadedGenreFilter() = runTest {
        val repo = FakeShowRepository().apply {
            browsePageResult = { page ->
                BrowsePage(if (page == 0) shows(1..25) + shows(26..30, "Comedy")
                else shows(31..40, "Comedy") + shows(41..50))
            }
        }
        val vm = HomeViewModel(repo, SavedStateHandle())
        runCurrent()
        vm.onGenreSelected("Comedy")
        vm.nextPage()
        runCurrent()
        assertEquals(15, vm.uiState.value.filteredCount)
        assertEquals(50, vm.uiState.value.loadedCount)
        assertEquals((26..40).toList(), vm.uiState.value.shows.map { it.id })
        assertEquals(0, vm.uiState.value.pageIndex)
        assertEquals(listOf(0, 1), repo.browsePages)
    }

    @Test fun suggestionsHandleSmallDuplicateAndEmptyPools() = runTest {
        for (titles in listOf(emptyList(), listOf("Only", "Only", " "), listOf("One", "Two"))) {
            val repo = FakeShowRepository().apply { browse = { titles.mapIndexed { i, title -> Show(i + 1, title) } } }
            val vm = HomeViewModel(repo, SavedStateHandle(), Random(5))
            runCurrent()
            vm.shuffleSuggestions()
            assertEquals(titles.filter { it.isNotBlank() }.toSet(), vm.uiState.value.suggestions.toSet())
        }
    }
}
