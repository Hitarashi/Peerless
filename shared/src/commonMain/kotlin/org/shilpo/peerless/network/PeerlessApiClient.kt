package org.shilpo.peerless.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.*

fun createDefaultPeerlessHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                encodeDefaults = true
            }
        )
    }
}

class PeerlessApiClient(
    baseUrl: String = "http://localhost:4444",
    val httpClient: HttpClient = createDefaultPeerlessHttpClient()
) {
    private val _baseUrl = MutableStateFlow(baseUrl.trimEnd('/'))
    val baseUrlState: StateFlow<String> = _baseUrl.asStateFlow()

    var baseUrl: String
        get() = _baseUrl.value
        set(value) {
            _baseUrl.value = value.trimEnd('/')
        }

    suspend fun search(
        query: String,
        provider: String? = null,
        page: Int = 1,
        limit: Int = 30
    ): Result<SearchResponse> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/search") {
            parameter("q", query)
            if (provider != null) {
                parameter("provider", provider)
            }
            parameter("page", page)
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) {
            error("Search request failed with status: ${response.status}")
        }
        response.body<SearchResponse>()
    }

    suspend fun getTrack(trackId: Int): Result<TrackDetailDto> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/tracks/$trackId")
        if (!response.status.isSuccess()) {
            error("Get track request failed with status: ${response.status}")
        }
        response.body<TrackDetailDto>()
    }

    suspend fun getPlaybackInfo(trackId: Int, token: String? = null): Result<PlaybackInfo> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/tracks/$trackId/playback") {
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        if (!response.status.isSuccess()) {
            error("Get playback info failed with status: ${response.status}")
        }
        response.body<PlaybackInfo>()
    }

    fun getStreamUrl(trackId: Int, ticket: String? = null): String {
        return if (!ticket.isNullOrBlank()) {
            "$baseUrl/api/v1/stream?ticket=$ticket"
        } else {
            "$baseUrl/api/v1/stream?track_id=$trackId"
        }
    }

    fun getArtworkUrl(trackId: Int, size: Int = 600): String {
        return "$baseUrl/api/v1/assets/tracks/$trackId/artwork?size=$size"
    }

    suspend fun exchangeOtp(
        code: String,
        deviceName: String,
        platform: String
    ): Result<ExchangeResponse> = runCatching {
        val response = httpClient.post("$baseUrl/api/v1/auth/exchange") {
            contentType(ContentType.Application.Json)
            setBody(ExchangeRequest(code = code, device_name = deviceName, platform = platform))
        }
        if (!response.status.isSuccess()) {
            error("OTP exchange failed with status: ${response.status}")
        }
        response.body<ExchangeResponse>()
    }

    suspend fun getLyrics(trackId: Int): Result<LyricsResponse> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/assets/tracks/$trackId/lyrics")
        if (!response.status.isSuccess()) {
            error("Get lyrics failed with status: ${response.status}")
        }
        response.body<LyricsResponse>()
    }

    fun close() {
        httpClient.close()
    }
}
