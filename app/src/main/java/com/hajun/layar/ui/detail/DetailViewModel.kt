package com.hajun.layar.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hajun.layar.data.ShowRepository
import com.hajun.layar.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: ShowRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val showId = (savedStateHandle.get<Any>("showId") as? Int)?.takeIf { it > 0 }
    private val state = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = state.asStateFlow()
    private var requestJob: Job? = null
    private var requestVersion = 0L

    init { retry() }

    companion object {
        fun factory(repository: ShowRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DetailViewModel(repository, createSavedStateHandle()) }
        }
    }

    fun retry() {
        val version = ++requestVersion
        requestJob?.cancel()
        val id = showId
        if (id == null) {
            state.value = DetailUiState(error = "Tayangan tidak ditemukan. Silakan kembali dan pilih tayangan lain.")
            return
        }
        state.value = DetailUiState(isLoading = true)
        requestJob = viewModelScope.launch {
            try {
                val show = repository.getShow(id)
                coroutineContext.ensureActive()
                if (version == requestVersion) state.value = DetailUiState(show = show)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                coroutineContext.ensureActive()
                if (version == requestVersion) state.value = DetailUiState(error = failure.userMessage())
            }
        }
    }
}
