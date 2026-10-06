package com.hajun.layar.data

import com.hajun.layar.data.remote.TvMazeApi
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DefaultShowRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultShowRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
        repository = DefaultShowRepository(TvMazeApi.create(server.url("/").newBuilder().host("127.0.0.1").build()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun browseUsesFirstPageWithoutDiscardingRecordsBeyondThirty() = runTest {
        val entries = (1..35).map {
            """{"id":$it,"name":"Show $it","image":{"medium":" https://static.tvmaze.com/medium/$it.jpg ","original":"https://static.tvmaze.com/original/$it.jpg"}}"""
        }
        server.enqueue(MockResponse().setBody("[null,{\"name\":\"No ID\"},${entries.joinToString(",")}]"))

        val shows = repository.browseShows()

        assertEquals((1..35).toList(), shows.map { it.id })
        assertEquals((1..35).map { "https://static.tvmaze.com/medium/$it.jpg" }, shows.map { it.imageMediumUrl })
        assertEquals((1..35).map { "https://static.tvmaze.com/original/$it.jpg" }, shows.map { it.imageOriginalUrl })
        assertEquals("/shows?page=0", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun browseRetainsAnEntireTwoHundredFiftyRecordApiBatch() = runTest {
        val entries = (1..250).joinToString(",") { """{"id":$it,"name":"Show $it"}""" }
        server.enqueue(MockResponse().setBody("[$entries]"))

        assertEquals((1..250).toList(), repository.browseShows().map { it.id })
        assertEquals("/shows?page=0", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun browsePageSendsPageParameterAndShortOrEmptySuccessIsNotTheEnd() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":501,"name":"Page two"}]"""))
        server.enqueue(MockResponse().setBody("[]"))
        val short = repository.browsePage(2)
        assertEquals(501, short.shows.single().id)
        org.junit.Assert.assertFalse(short.endReached)
        assertEquals("/shows?page=2", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        org.junit.Assert.assertFalse(repository.browsePage(3).endReached)
        assertEquals("/shows?page=3", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun onlyBrowse404MeansEndAndOtherFailuresPropagate() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(BrowsePage(emptyList(), true), repository.browsePage(9))
        assertEquals("/shows?page=9", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        server.enqueue(MockResponse().setResponseCode(500))
        try {
            repository.browsePage(10)
            fail("Server error must propagate")
        } catch (error: retrofit2.HttpException) {
            assertEquals(500, error.code())
        }
        server.enqueue(MockResponse().setResponseCode(404))
        try {
            repository.searchShows("missing")
            fail("Search 404 is not end of catalog")
        } catch (error: retrofit2.HttpException) {
            assertEquals(404, error.code())
        }
    }

    @Test
    fun negativeBrowsePageIsRejectedWithoutRequest() = runTest {
        try {
            repository.browsePage(-1)
            fail("Negative pages must be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun blankSearchBrowsesInsteadOfSendingEmptyQuery() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":1,"name":"Browse"}]"""))

        assertEquals("Browse", repository.searchShows(" \n\t ").single().title)
        assertEquals("/shows?page=0", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun detailLoadsIntegerIdWithNullableMetadata() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":73,"name":"Detail","rating":{"average":null},"genres":null,"summary":null,"image":{"medium":"https://static.tvmaze.com/medium/73.jpg","original":" https://static.tvmaze.com/original/73.jpg "}}"""))

        val show = repository.getShow(73)

        assertEquals(73, show.id)
        assertEquals("Detail", show.title)
        assertEquals("https://static.tvmaze.com/medium/73.jpg", show.imageMediumUrl)
        assertEquals("https://static.tvmaze.com/original/73.jpg", show.imageOriginalUrl)
        org.junit.Assert.assertNull(show.summary)
        org.junit.Assert.assertNull(show.rating)
        assertEquals(emptyList<String>(), show.genres)
        assertEquals("/shows/73", server.takeRequest(2, TimeUnit.SECONDS)?.path)
    }

    @Test
    fun insecureRemoteEndpointIsRejectedBeforeNetworkAccess() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            TvMazeApi.create("http://api.tvmaze.com/".toHttpUrl())
        }
    }

    @Test
    fun appContainerRetainsOneRepositoryInstance() {
        val container = AppContainer()
        org.junit.Assert.assertSame(container.repository, container.repository)
        org.junit.Assert.assertTrue(container.repository is DefaultShowRepository)
    }

    @Test
    fun invalidDetailIdFailsBeforeNetworkAccess() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":1,"name":"Unexpected"}"""))
        try {
            repository.getShow(-1)
            fail("Invalid IDs must not be requested")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun detailResponseMustMatchRequestedId() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":74,"name":"Wrong show"}"""))
        try {
            repository.getShow(73)
            fail("A mismatched response must not become a detail")
        } catch (_: kotlinx.serialization.SerializationException) {
            assertEquals("/shows/73", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        }
    }

    @Test
    fun httpFailureIsPropagatedForViewModelHandling() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Unavailable"))
        try {
            repository.browseShows()
            fail("HTTP errors must not become successful empty lists")
        } catch (failure: retrofit2.HttpException) {
            assertEquals(503, failure.code())
        }
    }

    @Test
    fun cancellationIsPreservedInsteadOfConvertedToAnEmptyResult() = runTest {
        val cancellation = kotlinx.coroutines.CancellationException("cancelled")
        val api = object : TvMazeApi {
            override suspend fun browseShows(page: Int): kotlinx.serialization.json.JsonArray = throw cancellation
            override suspend fun searchShows(query: String): kotlinx.serialization.json.JsonArray = throw cancellation
            override suspend fun getShow(id: Int): com.hajun.layar.data.remote.ShowDto = throw cancellation
        }
        val cancellableRepository = DefaultShowRepository(api)
        val calls: List<suspend () -> Any> = listOf(
            { cancellableRepository.browseShows() },
            { cancellableRepository.browsePage(2) },
            { cancellableRepository.searchShows("dark") },
            { cancellableRepository.getShow(73) },
        )
        for (call in calls) {
            try {
                call()
                fail("Cancellation must propagate")
            } catch (actual: kotlinx.coroutines.CancellationException) {
                org.junit.Assert.assertSame(cancellation, actual)
            }
        }
    }

    @Test
    fun searchMapsWrappedShowsInServerRelevanceOrderWithEncodedQuery() = runTest {
        server.enqueue(MockResponse().setBody(
            """[null,{"score":0.8,"show":{"id":9,"name":"First","extra":"ignored","image":{"medium":" https://static.tvmaze.com/medium/9.jpg ","original":"https://static.tvmaze.com/original/9.jpg"}}},{"score":0.9,"show":{"id":2,"name":"Second","image":{"original":"https://static.tvmaze.com/original/2.jpg"}}},{"show":null},{"show":{"name":"No ID"}}]""",
        ))

        val shows = repository.searchShows("  dark & light/日本  ")

        assertEquals(listOf(9, 2), shows.map { it.id })
        assertEquals(listOf("First", "Second"), shows.map { it.title })
        assertEquals(listOf("https://static.tvmaze.com/medium/9.jpg", null), shows.map { it.imageMediumUrl })
        assertEquals(
            listOf("https://static.tvmaze.com/original/9.jpg", "https://static.tvmaze.com/original/2.jpg"),
            shows.map { it.imageOriginalUrl },
        )
        val request = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        assertEquals("GET", request.method)
        assertEquals("/search/shows", request.requestUrl!!.encodedPath)
        assertEquals("dark & light/日本", request.requestUrl!!.queryParameter("q"))
        assertEquals("/search/shows?q=dark%20%26%20light%2F%E6%97%A5%E6%9C%AC", request.path)
    }
}
