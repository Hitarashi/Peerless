package org.shilpo.peerless.player

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.Track

interface PlayerConnection {
    val currentTrack: StateFlow<Track?>
    val playbackInfo: StateFlow<PlaybackInfo?>
    val signalPath: StateFlow<SignalPathSnapshot?>
    val status: StateFlow<PlaybackStatus>
    val isPlaying: StateFlow<Boolean>
    val queue: StateFlow<List<Track>>
    val currentIndex: StateFlow<Int>
    val shuffleMode: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val canSkipNext: StateFlow<Boolean>
    val canSkipPrevious: StateFlow<Boolean>
    val volume: StateFlow<Float>

    val currentPositionMs: Long
    val durationMs: Long

    fun play(track: Track, queue: List<Track> = emptyList())
    fun togglePlayPause()
    fun playNext()
    fun playPrevious()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setShuffleMode(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)

    fun addToQueue(track: Track)
    fun playNextInQueue(track: Track)
    fun moveInQueue(fromIndex: Int, toIndex: Int)
    fun removeAt(index: Int)
    fun clearQueue()
    fun stopAndDismiss()
}

val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection> {
    error("No PlayerConnection provided")
}

expect fun platformSetup(playerConnection: PlayerConnection)
