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
import org.shilpo.peerless.auth.TokenStorage
import org.shilpo.peerless.config.AppConfig
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
    baseUrl: String = AppConfig.DEFAULT_SERVER_URL,
    val httpClient: HttpClient = createDefaultPeerlessHttpClient(),
    val tokenStorage: TokenStorage? = null
) {
    private val _baseUrl = MutableStateFlow(baseUrl.trimEnd('/'))
    val baseUrlState: StateFlow<String> = _baseUrl.asStateFlow()

    var baseUrl: String
        get() = _baseUrl.value
        set(value) {
            _baseUrl.value = value.trimEnd('/')
        }

    private suspend fun resolveToken(explicitToken: String?): String? {
        return explicitToken ?: tokenStorage?.getToken()
    }

    // ========================================================================
    // Catalog & Search
    // ========================================================================

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

    suspend fun listAlbums(page: Int = 1, limit: Int = 30): Result<List<AlbumSummaryDto>> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/albums") {
            parameter("page", page)
            parameter("limit", limit)
        }
        if (!response.status.isSuccess()) {
            error("List albums failed with status: ${response.status}")
        }
        response.body<List<AlbumSummaryDto>>()
    }

    suspend fun getAlbumTracks(albumId: Int): Result<List<TrackSummaryDto>> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/albums/$albumId")
        if (!response.status.isSuccess()) {
            error("Get album tracks failed with status: ${response.status}")
        }
        response.body<List<TrackSummaryDto>>()
    }

    suspend fun getArtistTracks(artistName: String): Result<List<TrackSummaryDto>> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/artists/$artistName/tracks")
        if (!response.status.isSuccess()) {
            error("Get artist tracks failed with status: ${response.status}")
        }
        response.body<List<TrackSummaryDto>>()
    }

    // ========================================================================
    // Streaming & Playback
    // ========================================================================

    suspend fun getPlaybackInfo(
        trackId: Int,
        token: String? = null
    ): Result<PlaybackInfo> = runCatching {
        val authToken = resolveToken(token)
        val response = httpClient.post("$baseUrl/api/v1/tracks/$trackId/playback") {
            if (!authToken.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        if (!response.status.isSuccess()) {
            error("Get playback info failed with status: ${response.status}")
        }
        response.body<PlaybackInfo>()
    }

    /**
     * Resolves the stream URL.
     * In DEV mode: resolves directly to `/api/v1/stream?track_id=...` for testing.
     * In PROD mode with ticket: resolves to `/api/v1/stream?ticket=...`.
     */
    fun resolveStreamUrl(trackId: Int, ticket: String? = null): String {
        return when {
            AppConfig.IS_DEV_MODE && ticket.isNullOrBlank() -> {
                "$baseUrl/api/v1/stream?track_id=$trackId"
            }

            !ticket.isNullOrBlank() -> {
                "$baseUrl/api/v1/stream?ticket=$ticket"
            }

            else -> {
                "$baseUrl/api/v1/stream?track_id=$trackId"
            }
        }
    }

    fun getStreamUrl(trackId: Int, ticket: String? = null): String = resolveStreamUrl(trackId, ticket)


    // ========================================================================
    // Assets & Lyrics
    // ========================================================================

    fun getArtworkUrl(trackId: Int, size: Int = 600): String {
        return "$baseUrl/api/v1/assets/tracks/$trackId/artwork?size=$size"
    }

    suspend fun getLyrics(trackId: Int): Result<LyricsResponse> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/assets/tracks/$trackId/lyrics")
        if (!response.status.isSuccess()) {
            error("Get lyrics failed with status: ${response.status}")
        }
        response.body<LyricsResponse>()
    }

    // ========================================================================
    // Tasks & On-Demand Ripping
    // ========================================================================

    suspend fun createRipTask(
        provider: String,
        trackId: String,
        codec: String? = null,
        token: String? = null
    ): Result<RipTaskResponse> = runCatching {
        val authToken = resolveToken(token)
        val response = httpClient.post("$baseUrl/api/v1/tasks/rip") {
            contentType(ContentType.Application.Json)
            if (!authToken.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            setBody(RipTaskRequest(provider = provider, track_id = trackId, codec = codec))
        }
        if (!response.status.isSuccess()) {
            error("Create rip task failed with status: ${response.status}")
        }
        response.body<RipTaskResponse>()
    }

    // ========================================================================
    // Authentication & Sliding Session (256-bit Opaque Refresh Token)
    // ========================================================================

    suspend fun exchangeOtp(
        code: String,
        deviceName: String? = null,
        platform: String? = null
    ): Result<ExchangeResponse> = runCatching {
        val response = httpClient.post("$baseUrl/api/v1/auth/exchange") {
            contentType(ContentType.Application.Json)
            setBody(ExchangeRequest(code = code.trim(), device_name = deviceName, platform = platform))
        }
        if (!response.status.isSuccess()) {
            error("Exchange code failed with status: ${response.status}")
        }
        val exchangeResp = response.body<ExchangeResponse>()
        tokenStorage?.saveToken(exchangeResp.token)
        exchangeResp
    }

    suspend fun refreshToken(refreshToken: String? = null): Result<RefreshResponse> = runCatching {
        val currentToken = resolveToken(refreshToken)
            ?: error("No refresh token available")
        val response = httpClient.post("$baseUrl/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refresh_token = currentToken))
        }
        if (!response.status.isSuccess()) {
            error("Refresh token failed with status: ${response.status}")
        }
        val refreshResp = response.body<RefreshResponse>()
        tokenStorage?.saveToken(refreshResp.access_token)
        refreshResp
    }

    suspend fun logout(refreshToken: String? = null): Result<Unit> = runCatching {
        val currentToken = resolveToken(refreshToken)
        if (!currentToken.isNullOrBlank()) {
            httpClient.post("$baseUrl/api/v1/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refresh_token = currentToken))
            }
        }
        tokenStorage?.clearToken()
    }

    suspend fun getMe(token: String? = null): Result<MeResponse> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.get("$baseUrl/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Get me failed with status: ${response.status}")
        }
        response.body<MeResponse>()
    }

    // ========================================================================
    // User Library (Favorites & Playlists)
    // ========================================================================

    suspend fun getFavorites(token: String? = null): Result<List<FavoriteItemDto>> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.get("$baseUrl/api/v1/me/favorites") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Get favorites failed with status: ${response.status}")
        }
        response.body<List<FavoriteItemDto>>()
    }

    suspend fun addFavorite(trackId: Int, token: String? = null): Result<Unit> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.post("$baseUrl/api/v1/me/favorites/$trackId") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Add favorite failed with status: ${response.status}")
        }
    }

    suspend fun removeFavorite(trackId: Int, token: String? = null): Result<Unit> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.delete("$baseUrl/api/v1/me/favorites/$trackId") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Remove favorite failed with status: ${response.status}")
        }
    }

    suspend fun getPlaylists(token: String? = null): Result<List<PlaylistDto>> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.get("$baseUrl/api/v1/me/playlists") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Get playlists failed with status: ${response.status}")
        }
        response.body<List<PlaylistDto>>()
    }

    suspend fun createPlaylist(name: String, token: String? = null): Result<PlaylistDto> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.post("$baseUrl/api/v1/me/playlists") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $authToken")
            setBody(CreatePlaylistRequest(name = name))
        }
        if (!response.status.isSuccess()) {
            error("Create playlist failed with status: ${response.status}")
        }
        response.body<PlaylistDto>()
    }
}
