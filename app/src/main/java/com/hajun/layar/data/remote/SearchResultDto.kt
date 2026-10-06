package com.hajun.layar.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDto(val show: ShowDto? = null)
