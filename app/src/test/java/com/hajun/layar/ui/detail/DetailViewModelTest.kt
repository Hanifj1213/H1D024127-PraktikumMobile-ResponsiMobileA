package com.hajun.layar.ui.detail

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
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun failedDetailCanRetryTheSameId() = runTest {
        val repository = FakeShowRepository().apply {
            detail = { throw java.net.SocketTimeoutException("private detail") }
        }
        val viewModel = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.show)
        assertEquals("Koneksi terlalu lama. Coba lagi sebentar lagi.", viewModel.uiState.value.error)

        repository.detail = { Show(it, "Recovered") }
        viewModel.retry()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        runCurrent()

        assertEquals(listOf(73, 73), repository.detailIds)
        assertEquals("Recovered", viewModel.uiState.value.show!!.title)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun missingOrInvalidIdShowsErrorWithoutCallingRepository() = runTest {
        val handles = listOf(SavedStateHandle(), SavedStateHandle(mapOf("showId" to -1)), SavedStateHandle(mapOf("showId" to "73")))
        for (handle in handles) {
            val repository = FakeShowRepository()
            val viewModel = DetailViewModel(repository, handle)
            runCurrent()
            assertTrue(repository.detailIds.isEmpty())
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.show)
            assertEquals("Tayangan tidak ditemukan. Silakan kembali dan pilih tayangan lain.", viewModel.uiState.value.error)
            viewModel.retry()
            runCurrent()
            assertTrue(repository.detailIds.isEmpty())
        }
    }

    @Test
    fun retryCancelsPreviousDetailLoad() = runTest {
        var cancelled = false
        val repository = FakeShowRepository().apply {
            detail = {
                try { kotlinx.coroutines.awaitCancellation() }
                finally { cancelled = true }
            }
        }
        val viewModel = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))
        runCurrent()
        repository.detail = { Show(it, "New detail") }
        viewModel.retry()
        runCurrent()

        assertTrue(cancelled)
        assertEquals(listOf(73, 73), repository.detailIds)
        assertEquals("New detail", viewModel.uiState.value.show!!.title)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun lateDetailResponseCannotOverwriteRetryResult() = runTest {
        val repository = FakeShowRepository().apply {
            detail = { id -> kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                kotlinx.coroutines.delay(1000)
                Show(id, "Stale")
            } }
        }
        val viewModel = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))
        runCurrent()
        repository.detail = { Show(it, "Latest") }
        viewModel.retry()
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        assertEquals("Latest", viewModel.uiState.value.show!!.title)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun recreatedViewModelLoadsSavedIdAgain() = runTest {
        val repository = FakeShowRepository()
        val first = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))
        runCurrent()
        val restored = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))
        runCurrent()

        assertEquals(listOf(73, 73), repository.detailIds)
        assertEquals(first.uiState.value.show, restored.uiState.value.show)
    }

    @Test
    fun factoryRequiresSavedStateCreationExtras() {
        assertThrows(IllegalArgumentException::class.java) {
            DetailViewModel.factory(FakeShowRepository()).create(
                DetailViewModel::class.java,
                androidx.lifecycle.viewmodel.CreationExtras.Empty,
            )
        }
    }

    @Test
    fun savedIntegerIdLoadsDetailWithNullableFields() = runTest {
        val response = CompletableDeferred<Show>()
        val repository = FakeShowRepository().apply { detail = { response.await() } }
        val viewModel = DetailViewModel(repository, SavedStateHandle(mapOf("showId" to 73)))

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.show)
        runCurrent()
        assertEquals(listOf(73), repository.detailIds)
        response.complete(Show(73, "Restored"))
        runCurrent()

        assertEquals(Show(73, "Restored"), viewModel.uiState.value.show)
        assertNull(viewModel.uiState.value.show!!.summary)
        assertNull(viewModel.uiState.value.show!!.rating)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }
}
