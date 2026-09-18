package org.shilpo.peerless.player

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * Live audio chain parameters powering the Poweramp-style Signal Path inspector
 * and Lossless Badge (ADR 0004 & 0005).
 */
data class SignalPathSnapshot(
    val sourceFormat: String = "",
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val channels: Int = 2,
    val channelLayout: String? = null,
    val bitRateKbps: Int? = null,
    val decoder: String = "",
    val outputSink: String = "",
    val isBitPerfect: Boolean = true,
    val isDolbyAtmos: Boolean = false
)

/**
 * Discrete lifecycle and playback transition events emitted by underlying platform audio engines.
 */
sealed interface AudioEngineEvent {
    data object TrackCompleted : AudioEngineEvent
    data class TransitionedToNext(val url: String? = null) : AudioEngineEvent
    data class Error(val message: String, val code: Int? = null) : AudioEngineEvent
}

data class AudioEngineState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

interface AudioEngine {
    val state: StateFlow<AudioEngineState>
    val events: SharedFlow<AudioEngineEvent>
    val signalPath: StateFlow<SignalPathSnapshot?>

    fun prepare(
        url: String,
        headers: Map<String, String> = emptyMap(),
        title: String? = null,
        artist: String? = null,
        artworkUrl: String? = null
    )

    fun prepareNext(url: String?, headers: Map<String, String> = emptyMap()) {}
    fun play()
    fun pause()
    fun stop() {
        pause()
    }

    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float) {}
    fun release()
}

expect fun createAudioEngine(): AudioEngine
