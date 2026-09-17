package org.shilpo.peerless.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient

data class PlayerState(
    val currentTrack: TrackSummaryDto? = null,
    val playbackInfo: PlaybackInfo? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<TrackSummaryDto> = emptyList(),
    val isDevMode: Boolean = true,
    val serverUrl: String = "http://localhost:4444"
)

class PlaybackCoordinator(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val audioEngine: AudioEngine = createAudioEngine(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _state = MutableStateFlow(PlayerState(serverUrl = apiClient.baseUrl))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    var currentIndex: Int = -1
        private set

    val currentTrack: TrackSummaryDto?
        get() = _state.value.currentTrack

    val queue: List<TrackSummaryDto>
        get() = _state.value.queue

    val playbackInfo: PlaybackInfo?
        get() = _state.value.playbackInfo

    val engineState: AudioEngineState
        get() = audioEngine.state.value

    private var loadJob: Job? = null

    init {
        scope.launch {
            audioEngine.state.collect { engState ->
                _state.update { current ->
                    current.copy(
                        status = engState.status,
                        positionMs = engState.positionMs,
                        durationMs = if (engState.durationMs > 0L) engState.durationMs else current.durationMs
                    )
                }

                if (engState.status == PlaybackStatus.COMPLETED) {
                    playNext()
                }
            }
        }
    }

    fun playTrack(track: TrackSummaryDto, queue: List<TrackSummaryDto> = emptyList()) {
        val updatedQueue = if (queue.isNotEmpty()) {
            queue
        } else if (_state.value.queue.none { it.id == track.id }) {
            _state.value.queue + track
        } else {
            _state.value.queue
        }

        val idx = updatedQueue.indexOfFirst { it.id == track.id }
        currentIndex = if (idx >= 0) idx else 0

        _state.update {
            it.copy(
                currentTrack = track,
                queue = updatedQueue,
                durationMs = track.duration * 1000L,
                positionMs = 0L,
                status = PlaybackStatus.BUFFERING
            )
        }

        loadJob?.cancel()
        loadJob = scope.launch {
            val devMode = _state.value.isDevMode
            val streamUrl: String

            if (devMode) {
                streamUrl = apiClient.getStreamUrl(track.id)
                // Optionally resolve playback info in parallel for metadata display
                launch {
                    val infoResult = apiClient.getPlaybackInfo(track.id)
                    infoResult.onSuccess { info ->
                        _state.update { it.copy(playbackInfo = info) }
                    }
                }
            } else {
                val infoResult = apiClient.getPlaybackInfo(track.id)
                val info = infoResult.getOrNull()
                if (info != null) {
                    _state.update { it.copy(playbackInfo = info) }
                    streamUrl = if (info.stream_url.startsWith("http://") || info.stream_url.startsWith("https://")) {
                        info.stream_url
                    } else {
                        val base = apiClient.baseUrl.trimEnd('/')
                        val path = info.stream_url.trimStart('/')
                        "$base/$path"
                    }
                } else {
                    // Fallback to direct stream URL if playback info failed
                    streamUrl = apiClient.getStreamUrl(track.id)
                }
            }

            audioEngine.prepare(streamUrl)
            audioEngine.play()
        }
    }

    fun togglePlayPause() {
        val currentStatus = audioEngine.state.value.status
        when (currentStatus) {
            PlaybackStatus.PLAYING -> audioEngine.pause()
            PlaybackStatus.PAUSED -> audioEngine.play()
            PlaybackStatus.IDLE, PlaybackStatus.COMPLETED, PlaybackStatus.ERROR -> {
                val track = _state.value.currentTrack
                if (track != null) {
                    playTrack(track, _state.value.queue)
                }
            }

            PlaybackStatus.BUFFERING -> {
                // Already buffering, pause if requested
                audioEngine.pause()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun playNext() {
        val q = _state.value.queue
        if (q.isNotEmpty()) {
            val nextIdx = currentIndex + 1
            if (nextIdx in q.indices) {
                currentIndex = nextIdx
                playTrack(q[nextIdx], q)
            }
        }
    }

    fun playPrevious() {
        val currentPos = audioEngine.state.value.positionMs
        if (currentPos > 3000L) {
            seekTo(0L)
            return
        }

        val q = _state.value.queue
        if (q.isNotEmpty()) {
            val prevIdx = currentIndex - 1
            if (prevIdx in q.indices) {
                currentIndex = prevIdx
                playTrack(q[prevIdx], q)
            } else {
                seekTo(0L)
            }
        }
    }

    fun setDevMode(enabled: Boolean) {
        _state.update { it.copy(isDevMode = enabled) }
    }

    fun setServerUrl(url: String) {
        apiClient.baseUrl = url
        _state.update { it.copy(serverUrl = url) }
    }

    fun release() {
        loadJob?.cancel()
        audioEngine.release()
        _state.update {
            it.copy(
                status = PlaybackStatus.IDLE,
                positionMs = 0L
            )
        }
    }
}
