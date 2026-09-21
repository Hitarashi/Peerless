package org.shilpo.peerless.home

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedRepositoryTest {
    @Test
    fun feedShelvesDoNotRepeatRecentOrSimilarTracks() = runTest {
        val (repository, client) = runTestWithFeed { request ->
            when (request.url.parameters["method"]) {
                "user.getrecenttracks" -> jsonResponse(
                    """{"recenttracks":{"track":[
                    {"name":"Recent One","artist":{"#text":"Artist A"}},
                    {"name":"Recent Two","artist":{"#text":"Artist B"}}
                ]}}"""
                )

                "user.gettoptracks" -> jsonResponse(
                    """{"toptracks":{"track":[
                    {"name":"Recent One","artist":{"name":"Artist A"},"playcount":"20"},
                    {"name":"Rotation Track","artist":{"name":"Artist C"},"playcount":"16"}
                ]}}"""
                )

                "track.getsimilar" -> jsonResponse(
                    """{"similartracks":{"track":[
                    {"name":"Similar One","artist":{"name":"Artist D"},"match":"0.95"},
                    {"name":"Similar One","artist":{"name":"Artist D"},"match":"0.90"},
                    {"name":"Similar Two","artist":{"name":"Artist E"},"match":"0.80"}
                ]}}"""
                )

                else -> jsonResponse("""{"cached":[],"live":[]}""")
            }
        }
        try {
            val feed = repository.load(
                username = "listener",
                libraryTracks = listOf(
                    track(1, "Recent One", "Artist A"),
                    track(2, "Recent Two", "Artist B"),
                    track(3, "Rotation Track", "Artist C"),
                    track(4, "Similar One", "Artist D"),
                    track(5, "Similar Two", "Artist E")
                )
            )
            assertEquals(
                listOf("Recent One", "Recent Two"),
                feed.recentTracks.map { it.track.title })
            assertEquals(listOf("Rotation Track"), feed.yourRotation.map { it.track.title })
            assertTrue(feed.yourRotation.none { it.track.title.startsWith("Recent") })
            assertEquals(2, feed.similarToTaste.map { it.track.id }.distinct().size)
            assertTrue(feed.similarToTaste.none { item ->
                item.track.id in feed.recentTracks.map { it.track.id } + feed.yourRotation.map { it.track.id }
            })
        } finally {
            client.close()
        }
    }

    @Test
    fun catalogAliasesDoNotRepeatListenAgainInYourRotation() = runTest {
        val (repository, client) = runTestWithFeed { request ->
            when (request.url.parameters["method"]) {
                "user.getrecenttracks" -> jsonResponse(
                    """{"recenttracks":{"track":[{"name":"Song","artist":{"#text":"Artist"}}]}}"""
                )

                "user.gettoptracks" -> jsonResponse(
                    """{"toptracks":{"track":[{"name":"Song Remastered","artist":{"name":"Artist"},"playcount":"12"}]}}"""
                )

                "track.getsimilar" -> jsonResponse("""{"similartracks":{"track":[]}}""")
                else -> jsonResponse("""{"cached":[],"live":[]}""")
            }
        }
        try {
            val feed = repository.load(
                username = "listener",
                libraryTracks = listOf(track(10, "Song", "Artist"))
            )

            assertEquals(listOf(10), feed.recentTracks.map { it.track.id })
            assertTrue(feed.yourRotation.none { it.track.id == 10 })
        } finally {
            client.close()
        }
    }

    @Test
    fun emptyHistoryHasItsOwnReason() = runTest {
        val (repository, client) = runTestWithFeed { request ->
            when (request.url.parameters["method"]) {
                "user.getrecenttracks" -> jsonResponse("""{"recenttracks":{"track":[]}}""")
                "user.gettoptracks" -> jsonResponse("""{"toptracks":{"track":[]}}""")
                else -> jsonResponse("""{"similartracks":{"track":[]}}""")
            }
        }
        try {
            val feed = repository.load("listener", emptyList())

            assertFalse(feed.hasError)
            assertEquals(HomeFeedEmptyReason.NO_LISTENING_HISTORY, feed.emptyReason)
        } finally {
            client.close()
        }
    }

    @Test
    fun unmatchedHistoryOffersCatalogRecoveryState() = runTest {
        val (repository, client) = runTestWithFeed { request ->
            when (request.url.parameters["method"]) {
                "user.getrecenttracks" -> jsonResponse(
                    """{"recenttracks":{"track":[{"name":"Unavailable","artist":{"#text":"Unknown"}}]}}"""
                )

                "user.gettoptracks" -> jsonResponse(
                    """{"toptracks":{"track":[{"name":"Unavailable","artist":{"name":"Unknown"},"playcount":"1"}]}}"""
                )

                "track.getsimilar" -> jsonResponse("""{"similartracks":{"track":[]}}""")
                else -> jsonResponse("""{"cached":[],"live":[]}""")
            }
        }
        try {
            val feed = repository.load("listener", emptyList())

            assertFalse(feed.hasError)
            assertEquals(HomeFeedEmptyReason.NO_PLAYABLE_MATCHES, feed.emptyReason)
        } finally {
            client.close()
        }
    }

    @Test
    fun totalHistoryFailureRemainsRetryable() = runTest {
        val (repository, client) = runTestWithFeed { _ ->
            jsonResponse("{}", HttpStatusCode.InternalServerError)
        }
        try {
            val feed = repository.load("listener", emptyList())

            assertTrue(feed.hasError)
            assertEquals(null, feed.emptyReason)
        } finally {
            client.close()
        }
    }

    private fun track(id: Int, title: String, artist: String) = TrackSummaryDto(
        id = id,
        provider = "apple",
        track_id = id.toString(),
        title = title,
        artist = artist,
        album = "Album",
        duration = 180,
        codec = "alac"
    )

    private fun runTestWithFeed(
        response: (HttpRequestData) -> FeedResponse
    ): Pair<HomeFeedRepository, HttpClient> {
        val client = HttpClient(MockEngine { request ->
            val result = response(request)
            respond(
                content = result.body,
                status = result.status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return HomeFeedRepository(
            lastFmClient = LastFmClient(
                baseUrl = "https://lastfm.test/",
                httpClient = client,
                enableFallback = false
            ),
            apiClient = PeerlessApiClient(
                baseUrl = "https://peerless.test",
                httpClient = client
            )
        ) to client
    }

    private fun jsonResponse(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = FeedResponse(body, status)

    private data class FeedResponse(val body: String, val status: HttpStatusCode)
}
