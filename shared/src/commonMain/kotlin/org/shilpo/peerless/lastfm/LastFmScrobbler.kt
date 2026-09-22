package org.shilpo.peerless.lastfm

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.network.createDefaultPeerlessHttpClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import kotlin.math.min
import kotlin.time.Clock

@Serializable
data class LastFmConfig(
    val apiKey: String,
    val apiSecret: String,
    val sessionKey: String,
    val username: String = ""
)

@Serializable
private data class LastFmErrorResponse(
    val error: Int = 0,
    val message: String = ""
)

class LastFmScrobbler(
    private val httpClient: HttpClient = createDefaultPeerlessHttpClient(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val clock: Clock = Clock.System
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://ws.audioscrobbler.com/2.0/"

    private val _config = MutableStateFlow<LastFmConfig?>(null)
    val config: StateFlow<LastFmConfig?> = _config.asStateFlow()

    var isSelfActivePlaybackDevice: Boolean = true

    private var trackingJob: Job? = null
    private var lastScrobbledTrackId: Int? = null
    private var trackStartTimestamp: Long = 0L
    private var accumulatedPlayedMs: Long = 0L
    private var lastRecordedPositionMs: Long = 0L

    fun configure(config: LastFmConfig?) {
        _config.value = config
    }

    fun isConfigured(): Boolean =
        _config.value != null && _config.value?.sessionKey?.isNotBlank() == true

    fun calculateApiSig(params: Map<String, String>, secret: String): String {
        val sortedPairs = params.entries.sortedBy { it.key }
        val concatenated = buildString {
            for ((k, v) in sortedPairs) {
                if (k != "format" && k != "callback") {
                    append(k)
                    append(v)
                }
            }
            append(secret)
        }
        return Md5.hex(concatenated)
    }

    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        album: String? = null,
        durationSeconds: Int? = null
    ): Result<Unit> = runCatching {
        val cfg = _config.value ?: error("Last.fm scrobbler is not configured")
        val params = mutableMapOf(
            "method" to "track.updateNowPlaying",
            "api_key" to cfg.apiKey,
            "sk" to cfg.sessionKey,
            "artist" to artist.trim(),
            "track" to track.trim()
        )
        if (!album.isNullOrBlank()) params["album"] = album.trim()
        if (durationSeconds != null && durationSeconds > 0) params["duration"] =
            durationSeconds.toString()

        val sig = calculateApiSig(params, cfg.apiSecret)
        params["api_sig"] = sig
        params["format"] = "json"

        val response = httpClient.submitForm(
            url = baseUrl,
            formParameters = parameters {
                params.forEach { (k, v) -> append(k, v) }
            }
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess() || body.contains("\"error\"")) {
            val err = runCatching { json.decodeFromString<LastFmErrorResponse>(body) }.getOrNull()
            error("Last.fm updateNowPlaying failed: ${err?.message ?: body}")
        }
    }

    suspend fun scrobble(
        artist: String,
        track: String,
        timestampEpochSeconds: Long,
        album: String? = null,
        durationSeconds: Int? = null
    ): Result<Unit> = runCatching {
        val cfg = _config.value ?: error("Last.fm scrobbler is not configured")
        val params = mutableMapOf(
            "method" to "track.scrobble",
            "api_key" to cfg.apiKey,
            "sk" to cfg.sessionKey,
            "artist[0]" to artist.trim(),
            "track[0]" to track.trim(),
            "timestamp[0]" to timestampEpochSeconds.toString()
        )
        if (!album.isNullOrBlank()) params["album[0]"] = album.trim()
        if (durationSeconds != null && durationSeconds > 0) params["duration[0]"] =
            durationSeconds.toString()

        val sig = calculateApiSig(params, cfg.apiSecret)
        params["api_sig"] = sig
        params["format"] = "json"

        val response = httpClient.submitForm(
            url = baseUrl,
            formParameters = parameters {
                params.forEach { (k, v) -> append(k, v) }
            }
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess() || body.contains("\"error\"")) {
            val err = runCatching { json.decodeFromString<LastFmErrorResponse>(body) }.getOrNull()
            error("Last.fm scrobble failed: ${err?.message ?: body}")
        }
    }

    fun attachToPlayer(playerConnection: PlayerConnection) {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            var previousTrack: Track? = null

            playerConnection.currentTrack.collect { currentTrack ->
                if (currentTrack?.id != previousTrack?.id) {
                    previousTrack = currentTrack
                    lastScrobbledTrackId = null
                    accumulatedPlayedMs = 0L
                    lastRecordedPositionMs = 0L
                    trackStartTimestamp = clock.now().toEpochMilliseconds() / 1000L

                    if (currentTrack != null && isConfigured() && isSelfActivePlaybackDevice) {
                        launch {
                            val durationSec = (currentTrack.durationMs / 1000L).toInt()
                            updateNowPlaying(
                                artist = currentTrack.artist,
                                track = currentTrack.title,
                                album = currentTrack.album,
                                durationSeconds = durationSec
                            )
                        }
                    }
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(1000)
                val track = playerConnection.currentTrack.value ?: continue
                val isPlaying = playerConnection.isPlaying.value
                val status = playerConnection.status.value

                if (isPlaying && status == PlaybackStatus.PLAYING && isSelfActivePlaybackDevice && isConfigured()) {
                    val pos = playerConnection.positionMs.value
                    if (pos > lastRecordedPositionMs && (pos - lastRecordedPositionMs) < 3000) {
                        accumulatedPlayedMs += (pos - lastRecordedPositionMs)
                    }
                    lastRecordedPositionMs = pos

                    val totalDurationSec = track.durationMs / 1000L
                    if (totalDurationSec >= 30 && lastScrobbledTrackId != track.id) {
                        val requiredMs = min(track.durationMs / 2, 240_000L)
                        if (accumulatedPlayedMs >= requiredMs) {
                            lastScrobbledTrackId = track.id
                            launch {
                                scrobble(
                                    artist = track.artist,
                                    track = track.title,
                                    timestampEpochSeconds = trackStartTimestamp,
                                    album = track.album,
                                    durationSeconds = totalDurationSec.toInt()
                                )
                            }
                        }
                    }
                } else {
                    lastRecordedPositionMs = playerConnection.positionMs.value
                }
            }
        }
    }
}
