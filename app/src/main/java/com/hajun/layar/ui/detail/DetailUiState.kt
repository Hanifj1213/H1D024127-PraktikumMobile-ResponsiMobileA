package com.hajun.layar.ui.detail

import com.hajun.layar.model.Show

data class DetailUiState(
    val show: Show? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
