package com.hajun.layar.data

import com.hajun.layar.model.Show

// Only an HTTP 404 from the TVmaze index marks its end, not a short batch.
data class BrowsePage(
    val shows: List<Show>,
    val endReached: Boolean = false,
)
