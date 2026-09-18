package org.shilpo.peerless.network

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
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

    fun getArtworkUrl(track: TrackSummaryDto, size: Int = 600): String {
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

    suspend fun getLyrics(trackId: Int): Result<LyricsResponse> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/assets/tracks/$trackId/lyrics")
        if (!response.status.isSuccess()) {
            error("Get lyrics failed with status: ${response.status}")
        }
        response.body<LyricsResponse>()
    }

    open suspend fun createRipTask(
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

    open fun streamTaskEvents(taskId: String, token: String? = null): Flow<TaskProgressEvent> = flow {
        val authToken = resolveToken(token)
        try {
            val statement = httpClient.prepareGet("$baseUrl/api/v1/tasks/$taskId/events") {
                header(HttpHeaders.Accept, "text/event-stream")
                if (!authToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }
            statement.execute { response ->
                if (!response.status.isSuccess()) {
                    emit(
                        TaskProgressEvent(
                            task_id = taskId,
                            stage = "failed",
                            completed = true,
                            error = "SSE connection failed with status: ${response.status}"
                        )
                    )
                    return@execute
                }
                val channel: ByteReadChannel = response.body()
                @Suppress("DEPRECATION")
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":") || trimmed.contains(
                            "keep-alive",
                            ignoreCase = true
                        )
                    ) {
                        continue
                    }
                    if (trimmed.startsWith("data:")) {
                        val jsonStr = trimmed.removePrefix("data:").trim()
                        if (jsonStr.isNotEmpty()) {
                            val event = try {
                                json.decodeFromString<TaskProgressEvent>(jsonStr)
                            } catch (e: Exception) {
                                null
                            }
                            if (event != null) {
                                emit(event)
                                if (event.isFinished) {
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(
                TaskProgressEvent(
                    task_id = taskId,
                    stage = "failed",
                    completed = true,
                    error = e.message ?: "Network error during SSE stream"
                )
            )
        }
    }

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

val LocalPeerlessApiClient = staticCompositionLocalOf<PeerlessApiClient> {
    error("No PeerlessApiClient provided")
}

val LocalDevMode = compositionLocalOf<MutableState<Boolean>> {
    mutableStateOf(AppConfig.IS_DEV_MODE)
}

