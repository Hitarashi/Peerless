package org.shilpo.peerless.lastfm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LastFmClientContextTest {
    @Test
    fun `track info parses Last fm listener and play counts`() = runTest {
        var capturedRequest: HttpRequestData? = null
        val client = LastFmClient(
            apiKey = "test-key",
            baseUrl = "https://lastfm.test/2.0/",
            httpClient = HttpClient(MockEngine { request ->
                capturedRequest = request
                respond(
                    content = """{"track":{"name":"Digital Love","artist":{"name":"Daft Punk"},"listeners":"124","playcount":"1409"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }),
            enableFallback = false
        )

        try {
            val info = client.getTrackInfo("Daft Punk", "Digital Love").getOrThrow()

            assertEquals("track.getinfo", capturedRequest?.url?.parameters?.get("method"))
            assertEquals("Daft Punk", capturedRequest?.url?.parameters?.get("artist"))
            assertEquals("Digital Love", capturedRequest?.url?.parameters?.get("track"))
            assertEquals(124L, info.listeners)
            assertEquals(1409L, info.playcount)
        } finally {
            client.httpClient.close()
        }
    }

    @Test
    fun `fallback track info does not invent Last fm counts`() = runTest {
        val client = LastFmClient(
            httpClient = HttpClient(MockEngine {
                error("Offline")
            })
        )

        try {
            val info = client.getTrackInfo("Artist", "Track").getOrThrow()

            assertEquals(0L, info.listeners)
            assertEquals(0L, info.playcount)
        } finally {
            client.httpClient.close()
        }
    }

    @Test
    fun `album info parses bio tags stats and external url`() = runTest {
        var capturedRequest: HttpRequestData? = null
        val client = LastFmClient(
            apiKey = "test-key",
            baseUrl = "https://lastfm.test/2.0/",
            httpClient = HttpClient(MockEngine { request ->
                capturedRequest = request
                respond(
                    content = """{"album":{"name":"Discovery","artist":"Daft Punk","url":"http://www.last.fm/music/Daft+Punk/Discovery","releasedate":"12 Mar 2001, 00:00","listeners":"1200","playcount":"3400","wiki":{"summary":"<p>A landmark record.<a href=\"https://last.fm/more\">Read more</a></p>"},"toptags":{"tag":[{"name":"electronic","count":"9"}]}}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }),
            enableFallback = false
        )

        try {
            val info = client.getAlbumInfo("Daft Punk", "Discovery").getOrThrow()

            assertEquals("album.getinfo", capturedRequest?.url?.parameters?.get("method"))
            assertEquals("Daft Punk", capturedRequest?.url?.parameters?.get("artist"))
            assertEquals("Discovery", capturedRequest?.url?.parameters?.get("album"))
            assertEquals("Discovery", info.name)
            assertEquals("Daft Punk", info.artist)
            assertEquals("A landmark record.", info.wikiSummary)
            assertEquals("12 Mar 2001, 00:00", info.releaseDate)
            assertEquals(1200L, info.listeners)
            assertEquals(3400L, info.playcount)
            assertEquals("http://www.last.fm/music/Daft+Punk/Discovery", info.url)
            assertEquals("electronic", info.tags.single().name)
        } finally {
            client.httpClient.close()
        }
    }

    @Test
    fun `artist info exposes its external url`() = runTest {
        val client = LastFmClient(
            baseUrl = "https://lastfm.test/2.0/",
            httpClient = HttpClient(MockEngine {
                respond(
                    content = """{"artist":{"name":"Daft Punk","url":"http://www.last.fm/music/Daft+Punk","bio":{"summary":"French electronic duo."}}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }),
            enableFallback = false
        )

        try {
            val info = client.getArtistInfo("Daft Punk").getOrThrow()
            assertEquals("French electronic duo.", info.bioSummary)
            assertEquals("http://www.last.fm/music/Daft+Punk", info.url)
        } finally {
            client.httpClient.close()
        }
    }
}
