package org.shilpo.peerless.player

import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class AudioEngineState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

interface AudioEngine {
    val state: StateFlow<AudioEngineState>

    fun prepare(url: String, headers: Map<String, String> = emptyMap())
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}

expect fun createAudioEngine(): AudioEngine
