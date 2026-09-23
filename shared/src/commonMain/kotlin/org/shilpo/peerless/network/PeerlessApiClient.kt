package org.shilpo.peerless.network

import androidx.compose.runtime.staticCompositionLocalOf
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.shilpo.peerless.auth.TokenStorage
import org.shilpo.peerless.config.AppConfig
import org.shilpo.peerless.model.AlbumSummaryDto
import org.shilpo.peerless.model.CreatePlaylistRequest
import org.shilpo.peerless.model.ExchangeRequest
import org.shilpo.peerless.model.ExchangeResponse
import org.shilpo.peerless.model.LastFmIntegrationResponse
import org.shilpo.peerless.model.LastFmLoginRequest
import org.shilpo.peerless.model.LyricsResponse
import org.shilpo.peerless.model.MeResponse
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.PlaylistDto
import org.shilpo.peerless.model.RefreshRequest
import org.shilpo.peerless.model.RefreshResponse
import org.shilpo.peerless.model.RipTaskRequest
import org.shilpo.peerless.model.RipTaskResponse
import org.shilpo.peerless.model.RipTaskSnapshotDto
import org.shilpo.peerless.model.SearchResponse
import org.shilpo.peerless.model.ServerHealthDto
import org.shilpo.peerless.model.TrackDetailDto
import org.shilpo.peerless.model.TrackSummaryDto

fun createDefaultPeerlessHttpClient(
    engine: HttpClientEngine? = null,
    requestTimeoutMillis: Long = 20_000L
): HttpClient {
    val timeoutMillis = requestTimeoutMillis
    val configureClient: HttpClientConfig<*>.() -> Unit = {
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
        install(HttpTimeout) {
            this.requestTimeoutMillis = timeoutMillis
            this.connectTimeoutMillis = minOf(timeoutMillis, 10_000L)
            this.socketTimeoutMillis = timeoutMillis
        }
    }
    return if (engine == null) HttpClient(configureClient) else HttpClient(engine, configureClient)
}

class ApiHttpException(val statusCode: Int, message: String) : Exception(message)

private suspend fun <T> requestResult(request: suspend () -> T): Result<T> =
    try {
        Result.success(request())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (responseError: ResponseException) {
        Result.failure(
            ApiHttpException(
                statusCode = responseError.response.status.value,
                message = responseError.message ?: "HTTP request failed"
            )
        )
    } catch (timeout: HttpRequestTimeoutException) {
        Result.failure(
            IllegalStateException(
                "The server took too long to respond. Check your connection or server address, then try again.",
                timeout
            )
        )
    } catch (error: Exception) {
        Result.failure(error)
    }

open class PeerlessApiClient(
    baseUrl: String = AppConfig.DEFAULT_SERVER_URL,
    val httpClient: HttpClient = createDefaultPeerlessHttpClient(),
    val tokenStorage: TokenStorage? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

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

    suspend fun search(
        query: String,
        provider: String? = null,
        page: Int = 1,
        limit: Int = 30
    ): Result<SearchResponse> {
        return try {
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
            Result.success(response.body<SearchResponse>())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getTrack(trackId: Int): Result<TrackDetailDto> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/tracks/$trackId")
        if (!response.status.isSuccess()) {
            error("Get track request failed with status: ${response.status}")
        }
        response.body<TrackDetailDto>()
    }

    suspend fun listAlbums(page: Int = 1, limit: Int = 30): Result<List<AlbumSummaryDto>> =
        runCatching {
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

    open suspend fun getPlaybackInfo(
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

    fun resolveStreamUrl(trackId: Int, ticket: String? = null): String {
        require(!ticket.isNullOrBlank()) {
            "Playback ticket is required for streaming. Direct unauthenticated streams are not supported."
        }
        return "$baseUrl/api/v1/stream?ticket=$ticket"
    }


    fun getArtworkUrl(track: TrackSummaryDto, size: Int = 600): String {
        if (track.id > 0) {
            return getArtworkUrl(track.id, size)
        }
        val provider = track.provider.lowercase()
        if (track.track_id.isNotBlank() && (provider == "apple" || provider == "qobuz")) {
            return URLBuilder(
                "$baseUrl/api/v1/assets/providers/$provider/tracks/${track.track_id.encodeURLPathPart()}/artwork"
            ).apply {
                parameters.append("size", size.toString())
                parameters.append("title", track.title)
                parameters.append("artist", track.artist)
            }.buildString()
        }
        if (!track.artwork_url.isNullOrBlank()) {
            val regex = Regex("""\d+x\d+bb""")
            return if (track.artwork_url.contains(regex)) {
                track.artwork_url.replace(regex, "${size}x${size}bb")
            } else {
                track.artwork_url
            }
        }
        return getArtworkUrl(track.id, size)
    }

    fun getArtworkUrl(trackId: Int, size: Int = 600): String {
        if (trackId < 0) {
            return when (trackId) {
                -101 -> "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/3e/76/b0/3e76b0e3-762b-2286-a019-8afb19cee541/886445635829.jpg/600x600bb.jpg"
                -102 -> "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/600x600bb.jpg"
                -103 -> "https://is1-ssl.mzstatic.com/image/thumb/Music/7f/9f/d6/mzi.vtnaewef.jpg/600x600bb.jpg"
                -104 -> "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/d2/48/f4/d248f4ae-a7e4-a48e-1588-6617de3e8d76/mzi.izeorbmm.jpg/600x600bb.jpg"
                -105 -> "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/07/60/ba/0760ba0f-148c-b18f-d0ff-169ee96f3af5/634904078164.png/600x600bb.jpg"
                -106 -> "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/61/e7/3f/61e73f94-018d-5f50-50ec-8521952bc72e/20UM1IM11629.rgb.jpg/600x600bb.jpg"
                -107 -> "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/88/16/2c/88162c3d-46db-8321-61f3-3a47404cfe76/075596050920.jpg/600x600bb.jpg"
                -108 -> "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/df/db/61/dfdb615d-47f8-06e9-9533-b96daccc029f/18UMGIM31076.rgb.jpg/600x600bb.jpg"
                else -> ""
            }
        }
        if (trackId == 0) return ""
        return "$baseUrl/api/v1/assets/tracks/$trackId/artwork?size=$size"
    }

    suspend fun getLyrics(trackId: Int, refresh: Boolean = false): Result<LyricsResponse> =
        runCatching {
            val response = httpClient.get("$baseUrl/api/v1/assets/tracks/$trackId/lyrics") {
                if (refresh) parameter("refresh", true)
            }
            if (!response.status.isSuccess()) {
                error("Get lyrics failed with status: ${response.status}")
            }
            response.body<LyricsResponse>()
        }

    open suspend fun createRipTask(
        provider: String,
        trackId: String,
        codec: String? = null,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        duration: Int? = null,
        token: String? = null
    ): Result<RipTaskResponse> = runCatching {
        val authToken = resolveToken(token)
        val response = httpClient.post("$baseUrl/api/v1/tasks/rip") {
            contentType(ContentType.Application.Json)
            if (!authToken.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            setBody(
                RipTaskRequest(
                    provider = provider,
                    track_id = trackId,
                    codec = codec,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration
                )
            )
        }
        if (!response.status.isSuccess()) {
            error("Create rip task failed with status: ${response.status}")
        }
        response.body<RipTaskResponse>()
    }

    open suspend fun listRipTasks(token: String? = null): Result<List<RipTaskSnapshotDto>> =
        requestResult {
            val authToken = resolveToken(token)
            val response = httpClient.get("$baseUrl/api/v1/tasks") {
                if (!authToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }
            if (!response.status.isSuccess()) {
                error("List rip tasks failed with status: ${response.status}")
            }
            response.body<List<RipTaskSnapshotDto>>()
        }

    open suspend fun cancelRipTask(taskId: String, token: String? = null): Result<Unit> =
        runCatching {
            val authToken = resolveToken(token)
            val response = httpClient.delete("$baseUrl/api/v1/tasks/$taskId") {
                if (!authToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }
            if (!response.status.isSuccess()) {
                error("Cancel rip task failed with status: ${response.status}")
            }
        }

    open suspend fun exchangeOtp(
        code: String,
        deviceName: String? = null,
        platform: String? = null
    ): Result<ExchangeResponse> = requestResult {
        val response = httpClient.post("$baseUrl/api/v1/auth/exchange") {
            contentType(ContentType.Application.Json)
            setBody(
                ExchangeRequest(
                    code = code.trim(),
                    device_name = deviceName,
                    platform = platform
                )
            )
        }
        if (!response.status.isSuccess()) {
            throw ApiHttpException(
                statusCode = response.status.value,
                message = if (response.status.value == 401) {
                    "Telegram connection code was rejected. It may be expired or already used; send /stream to the bot for a fresh code."
                } else {
                    "Exchange code failed with status: ${response.status}"
                }
            )
        }
        val exchangeResp = response.body<ExchangeResponse>()
        tokenStorage?.saveToken(exchangeResp.token)
        exchangeResp
    }

    open suspend fun refreshToken(refreshToken: String? = null): Result<RefreshResponse> =
        requestResult {
            val currentToken = resolveToken(refreshToken)
                ?: error("No refresh token available")
            val response = httpClient.post("$baseUrl/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refresh_token = currentToken))
            }
            if (!response.status.isSuccess()) {
                throw ApiHttpException(
                    statusCode = response.status.value,
                    message = "Refresh token failed with status: ${response.status}"
                )
            }
            val refreshResp = response.body<RefreshResponse>()
            tokenStorage?.saveToken(refreshResp.access_token)
            refreshResp
        }

    open suspend fun logout(refreshToken: String? = null): Result<Unit> = runCatching {
        val currentToken = resolveToken(refreshToken)
        if (!currentToken.isNullOrBlank()) {
            httpClient.post("$baseUrl/api/v1/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refresh_token = currentToken))
            }
        }
        tokenStorage?.clearToken()
    }

    open suspend fun getMe(token: String? = null): Result<MeResponse> = requestResult {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.get("$baseUrl/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            throw ApiHttpException(
                statusCode = response.status.value,
                message = "Get me failed with status: ${response.status}"
            )
        }
        response.body<MeResponse>()
    }

    open suspend fun getUserAvatar(token: String? = null): Result<ByteArray?> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.get("$baseUrl/api/v1/auth/me/avatar") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        when (response.status) {
            HttpStatusCode.NotFound, HttpStatusCode.NoContent -> null
            else -> {
                if (!response.status.isSuccess()) {
                    error("Get user avatar failed with status: ${response.status}")
                }
                response.body<ByteArray>()
            }
        }
    }

    open suspend fun getServerHealth(): Result<ServerHealthDto> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/health")
        if (!response.status.isSuccess()) {
            error("Get server health failed with status: ${response.status}")
        }
        response.body<ServerHealthDto>()
    }

    open suspend fun getFavorites(token: String? = null): Result<List<TrackSummaryDto>> =
        runCatching {
            val authToken = resolveToken(token) ?: error("Not authenticated")
            val response = httpClient.get("$baseUrl/api/v1/me/favorites") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            if (!response.status.isSuccess()) {
                error("Get favorites failed with status: ${response.status}")
            }
            response.body<List<TrackSummaryDto>>()
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

    suspend fun createPlaylist(name: String, token: String? = null): Result<PlaylistDto> =
        runCatching {
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

    open suspend fun loginLastFm(
        username: String,
        password: String,
        token: String? = null
    ): Result<LastFmIntegrationResponse> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.post("$baseUrl/api/v1/integrations/lastfm/login") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $authToken")
            setBody(LastFmLoginRequest(username = username, password = password))
        }
        if (!response.status.isSuccess()) {
            val errText = response.bodyAsText()
            error("Last.fm login failed: $errText")
        }
        response.body<LastFmIntegrationResponse>()
    }

    open suspend fun getLastFmStatus(token: String? = null): Result<LastFmIntegrationResponse> =
        requestResult {
            val authToken = resolveToken(token) ?: error("Not authenticated")
            val response = httpClient.get("$baseUrl/api/v1/integrations/lastfm/status") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
            if (!response.status.isSuccess()) {
                throw ApiHttpException(
                    statusCode = response.status.value,
                    message = "Get Last.fm status failed with status: ${response.status}"
                )
            }
            response.body<LastFmIntegrationResponse>()
        }

    open suspend fun disconnectLastFm(token: String? = null): Result<Unit> = runCatching {
        val authToken = resolveToken(token) ?: error("Not authenticated")
        val response = httpClient.delete("$baseUrl/api/v1/integrations/lastfm") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        if (!response.status.isSuccess()) {
            error("Disconnect Last.fm failed with status: ${response.status}")
        }
    }
}

val LocalPeerlessApiClient = staticCompositionLocalOf<PeerlessApiClient> {
    error("No PeerlessApiClient provided")
}
