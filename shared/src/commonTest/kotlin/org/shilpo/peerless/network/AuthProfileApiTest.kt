package org.shilpo.peerless.network

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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthProfileApiTest {
    @Test
    fun authenticatedAvatarRequestReturnsImageBytes() = runTest {
        val image = byteArrayOf(1, 2, 3, 4)
        var capturedRequest: HttpRequestData? = null
        val client = PeerlessApiClient(
            baseUrl = "https://peerless.test",
            httpClient = HttpClient(MockEngine { request ->
                capturedRequest = request
                respond(
                    content = image,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                )
            })
        )

        val result = client.getUserAvatar(token = "session-token").getOrThrow()

        assertContentEquals(image, result)
        assertEquals("/api/v1/auth/me/avatar", capturedRequest?.url?.encodedPath)
        assertEquals(
            "Bearer session-token",
            capturedRequest?.headers?.get(HttpHeaders.Authorization)
        )
        client.httpClient.close()
    }

    @Test
    fun missingTelegramPhotoReturnsNull() = runTest {
        val client = PeerlessApiClient(
            baseUrl = "https://peerless.test",
            httpClient = HttpClient(MockEngine {
                respond(content = "", status = HttpStatusCode.NotFound)
            })
        )

        assertEquals(null, client.getUserAvatar(token = "session-token").getOrThrow())
        client.httpClient.close()
    }

    @Test
    fun unauthorizedProfileResponsePreservesHttpStatus() = runTest {
        val client = PeerlessApiClient(
            baseUrl = "https://peerless.test",
            httpClient = HttpClient(MockEngine {
                respond(content = "", status = HttpStatusCode.Unauthorized)
            })
        )

        val error = client.getMe(token = "expired-token").exceptionOrNull()

        assertEquals(401, assertIs<ApiHttpException>(error).statusCode)
        client.httpClient.close()
    }

    @Test
    fun invalidConnectionCodePreservesUnauthorizedStatus() = runTest {
        val client = PeerlessApiClient(
            baseUrl = "https://peerless.test",
            httpClient = HttpClient(MockEngine {
                respond(content = "", status = HttpStatusCode.Unauthorized)
            }) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val error = client.exchangeOtp(code = "ABC-123").exceptionOrNull()

        assertEquals(401, assertIs<ApiHttpException>(error).statusCode)
        client.httpClient.close()
    }

    @Test
    fun refreshResponsePreservesServerFailureStatus() = runTest {
        val client = PeerlessApiClient(
            baseUrl = "https://peerless.test",
            httpClient = HttpClient(MockEngine {
                respond(content = "", status = HttpStatusCode.ServiceUnavailable)
            }) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )

        val error = client.refreshToken(refreshToken = "session-token").exceptionOrNull()

        assertEquals(503, assertIs<ApiHttpException>(error).statusCode)
        client.httpClient.close()
    }
}
