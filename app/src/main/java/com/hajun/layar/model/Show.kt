package com.hajun.layar.model

data class Show(
    val id: Int,
    val title: String,
    val year: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val summary: String? = null,
    val language: String? = null,
    val status: String? = null,
    val type: String? = null,
    val runtime: Int? = null,
    val network: String? = null,
    val url: String? = null,
    val imageMediumUrl: String? = null,
    val imageOriginalUrl: String? = null,
)
