package org.shilpo.peerless.tasks

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.shilpo.peerless.network.PeerlessApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RipCoordinatorTest {
    @Test
    fun testCancelRipTaskInApiClient() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/tasks/task_test_99", request.url.encodedPath)
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("Bearer sample_token", request.headers[HttpHeaders.Authorization])
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = PeerlessApiClient(
            baseUrl = "https://server.peerless.test",
            httpClient = HttpClient(mockEngine)
        )

        assertTrue(client.cancelRipTask("task_test_99", "sample_token").isSuccess)
    }
}
