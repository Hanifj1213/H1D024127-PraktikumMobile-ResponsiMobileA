package com.hajun.layar.data.remote

import com.hajun.layar.model.Show
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShowDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun metadataMapsWithoutLosingHtmlOrFallbackChannel() {
        val dto = json.decodeFromString<ShowDto>(
            """{"id":7,"name":" Dark ","premiered":"2017-12-01","rating":{"average":8.7},"genres":["Drama",null," ","Science-Fiction"],"summary":"<p>Time travel.</p>","language":"German","status":"Ended","type":"Scripted","runtime":60,"network":null,"webChannel":{"name":"Netflix"},"url":"https://www.tvmaze.com/shows/7/dark"}""",
        )

        val show = requireNotNull(dto.toShow())

        assertEquals("Dark", show.title)
        assertEquals("2017", show.year)
        assertEquals(8.7, show.rating!!, 0.0)
        assertEquals(listOf("Drama", "Science-Fiction"), show.genres)
        assertEquals("<p>Time travel.</p>", show.summary)
        assertEquals("German", show.language)
        assertEquals("Ended", show.status)
        assertEquals("Scripted", show.type)
        assertEquals(60, show.runtime)
        assertEquals("Netflix", show.network)
        assertEquals("https://www.tvmaze.com/shows/7/dark", show.url)
    }

    @Test
    fun showImageUrlsDefaultToNull() {
        val show = Show(id = 1, title = "No poster")

        assertNull(show.imageMediumUrl)
        assertNull(show.imageOriginalUrl)
    }

    @Test
    fun absentOrNullImageMapsToNullUrls() {
        val responses = listOf("""{"id":1}""", """{"id":1,"image":null}""")
        for (response in responses) {
            val show = requireNotNull(json.decodeFromString<ShowDto>(response).toShow())

            assertNull(show.imageMediumUrl)
            assertNull(show.imageOriginalUrl)
        }
    }

    @Test
    fun emptyImageAndNullOrBlankSizesMapToNullUrls() {
        val images = listOf(
            "{}",
            """{"medium":null,"original":null}""",
            """{"medium":"","original":""}""",
            """{"medium":"  ","original":"\t\n "}""",
        )
        for (image in images) {
            val show = requireNotNull(json.decodeFromString<ShowDto>(
                """{"id":1,"image":$image}""",
            ).toShow())

            assertNull(show.imageMediumUrl)
            assertNull(show.imageOriginalUrl)
        }
    }

    @Test
    fun mediumImageMapsWithoutInventingOriginalFallback() {
        val show = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":1,"image":{"medium":"https://static.tvmaze.com/medium.jpg"}}""",
        ).toShow())

        assertEquals("https://static.tvmaze.com/medium.jpg", show.imageMediumUrl)
        assertNull(show.imageOriginalUrl)
    }

    @Test
    fun originalImageMapsWithoutInventingMediumFallback() {
        val show = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":1,"image":{"original":"https://static.tvmaze.com/original.jpg"}}""",
        ).toShow())

        assertNull(show.imageMediumUrl)
        assertEquals("https://static.tvmaze.com/original.jpg", show.imageOriginalUrl)
    }

    @Test
    fun bothImageUrlsAreTrimmedAndPreserved() {
        val show = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":1,"image":{"medium":"  https://static.tvmaze.com/medium.jpg  ","original":" https://static.tvmaze.com/original.jpg "}}""",
        ).toShow())

        assertEquals("https://static.tvmaze.com/medium.jpg", show.imageMediumUrl)
        assertEquals("https://static.tvmaze.com/original.jpg", show.imageOriginalUrl)
    }

    @Test
    fun malformedRelativeAndNonHttpsImageUrlsAreIgnored() {
        val invalidUrls = listOf(
            "not a URL",
            "https://",
            "https://[invalid]/poster.jpg",
            "https://bad host/poster.jpg",
            "/poster.jpg",
            "//static.tvmaze.com/poster.jpg",
            "http://static.tvmaze.com/poster.jpg",
            "ftp://static.tvmaze.com/poster.jpg",
            "javascript:alert(1)",
            "data:image/png;base64,AAAA",
        )
        for (url in invalidUrls) {
            val show = requireNotNull(json.decodeFromString<ShowDto>(
                """{"id":1,"image":{"medium":"$url","original":"$url"}}""",
            ).toShow())

            assertNull("Medium must reject $url", show.imageMediumUrl)
            assertNull("Original must reject $url", show.imageOriginalUrl)
        }
    }

    @Test
    fun malformedHttpsImageUrlsAreNotRepairedIntoValidUrls() {
        val invalidUrls = listOf(
            "https:static.tvmaze.com/poster.jpg",
            "https:/static.tvmaze.com/poster.jpg",
            "https:///static.tvmaze.com/poster.jpg",
            "https://\\\\static.tvmaze.com/poster.jpg",
            "https://static.tvmaze.com/po\nster.jpg",
        )
        for (url in invalidUrls) {
            val show = requireNotNull(ShowDto(
                id = 1,
                image = ImageDto(medium = url, original = url),
            ).toShow())

            assertNull("Medium must reject $url", show.imageMediumUrl)
            assertNull("Original must reject $url", show.imageOriginalUrl)
        }
    }

    @Test
    fun invalidImageSizeDoesNotDiscardOtherValidSize() {
        val mediumOnly = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":1,"image":{"medium":"https://static.tvmaze.com/medium.jpg","original":"http://static.tvmaze.com/original.jpg"}}""",
        ).toShow())
        val originalOnly = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":2,"image":{"medium":"not a URL","original":"https://static.tvmaze.com/original.jpg"}}""",
        ).toShow())

        assertEquals("https://static.tvmaze.com/medium.jpg", mediumOnly.imageMediumUrl)
        assertNull(mediumOnly.imageOriginalUrl)
        assertNull(originalOnly.imageMediumUrl)
        assertEquals("https://static.tvmaze.com/original.jpg", originalOnly.imageOriginalUrl)
    }

    @Test
    fun incompleteIdentityIsSkipped() {
        assertNull(json.decodeFromString<ShowDto>("""{"name":"Missing ID"}""").toShow())
        assertNull(json.decodeFromString<ShowDto>("""{"id":-1}""").toShow())
    }

    @Test
    fun malformedOptionalValuesBecomeNull() {
        val show = requireNotNull(json.decodeFromString<ShowDto>(
            """{"id":2,"name":" ","premiered":"unknown","rating":{"average":12.0},"summary":" ","runtime":-1,"network":{"name":" "}}""",
        ).toShow())

        assertEquals("Tanpa judul", show.title)
        assertNull(show.year)
        assertNull(show.rating)
        assertNull(show.summary)
        assertNull(show.runtime)
        assertNull(show.network)
    }

    @Test
    fun nullableMetadataMapsToSafeShowDefaults() {
        val dto = json.decodeFromString<ShowDto>(
            """{"id":42,"name":null,"premiered":null,"rating":null,"genres":null,"summary":null,"unknown":"ignored"}""",
        )

        val show = requireNotNull(dto.toShow())

        assertEquals(42, show.id)
        assertEquals("Tanpa judul", show.title)
        assertNull(show.year)
        assertNull(show.rating)
        assertEquals(emptyList<String>(), show.genres)
        assertNull(show.summary)
        assertNull(show.language)
        assertNull(show.status)
        assertNull(show.type)
        assertNull(show.runtime)
        assertNull(show.network)
        assertNull(show.url)
    }
}
