package com.hajun.layar.ui.home

import com.hajun.layar.model.Show

data class HomeUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val shows: List<Show> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val loadedCount: Int = 0,
    val filteredCount: Int = 0,
    val availableGenres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val suggestions: List<String> = emptyList(),
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false,
    val isPageLoading: Boolean = false,
    val pageError: String? = null,
    val pageNotice: String? = null,
    val isCatalogComplete: Boolean = false,
    val pageChangeToken: Int = 0,
)
