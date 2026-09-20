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

sealed interface AudioEngineEvent {
    data object TrackCompleted : AudioEngineEvent
    data class TransitionedToNext(val url: String? = null) : AudioEngineEvent
    data class Error(val message: String, val code: Int? = null) : AudioEngineEvent
}

data class AudioEngineState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val errorMessage: String? = null
)

interface AudioEngine {
    val state: StateFlow<AudioEngineState>
    val events: SharedFlow<AudioEngineEvent>
    val signalPath: StateFlow<SignalPathSnapshot?>
    val spectrum: StateFlow<AudioSpectrumFrame?> get() = EmptyAudioSpectrum

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
