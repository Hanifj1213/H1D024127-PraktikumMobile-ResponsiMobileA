package com.hajun.layar.data.remote

import com.hajun.layar.model.Show
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class ShowDto(
    val id: Int? = null,
    val name: String? = null,
    val premiered: String? = null,
    val rating: RatingDto? = null,
    val genres: List<String?>? = null,
    val summary: String? = null,
    val language: String? = null,
    val status: String? = null,
    val type: String? = null,
    val runtime: Int? = null,
    val network: NetworkDto? = null,
    val webChannel: NetworkDto? = null,
    val url: String? = null,
    val image: ImageDto? = null,
) {
    fun toShow(): Show? {
        val showId = id?.takeIf { it > 0 } ?: return null
        return Show(
            id = showId,
            title = name.clean() ?: "Tanpa judul",
            year = premiered?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }?.take(4),
            rating = rating?.average?.takeIf { it.isFinite() && it in 0.0..10.0 },
            genres = genres.orEmpty().mapNotNull { it.clean() },
            summary = summary.clean(),
            language = language.clean(),
            status = status.clean(),
            type = type.clean(),
            runtime = runtime?.takeIf { it >= 0 },
            network = network?.name.clean() ?: webChannel?.name.clean(),
            url = url.clean(),
            imageMediumUrl = image?.medium.cleanHttpsUrl(),
            imageOriginalUrl = image?.original.cleanHttpsUrl(),
        )
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun String?.cleanHttpsUrl(): String? {
        val value = clean() ?: return null
        // Reject malformed input before OkHttp's lenient parser can repair it.
        if (!value.startsWith("https://", ignoreCase = true) ||
            value.substringAfter("://").startsWith('/') ||
            value.any { it == '\\' || it.isWhitespace() || it.isISOControl() }
        ) {
            return null
        }
        return value.toHttpUrlOrNull()?.takeIf { it.isHttps }?.toString()
    }
}

@Serializable
data class RatingDto(val average: Double? = null)

@Serializable
data class NetworkDto(val name: String? = null)

@Serializable
data class ImageDto(
    val medium: String? = null,
    val original: String? = null,
)
