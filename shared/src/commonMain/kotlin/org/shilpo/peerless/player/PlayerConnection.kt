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
    val spectrum: StateFlow<AudioSpectrumFrame?> get() = EmptyAudioSpectrum
    val status: StateFlow<PlaybackStatus>
    val isPlaying: StateFlow<Boolean>
    val queue: StateFlow<List<Track>>
    val currentIndex: StateFlow<Int>
    val shuffleMode: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val canSkipNext: StateFlow<Boolean>
    val canSkipPrevious: StateFlow<Boolean>
    val volume: StateFlow<Float>

    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val bufferedPositionMs: StateFlow<Long>
    val outputLatencyMs: StateFlow<Long>

    val currentPositionMs: Long get() = positionMs.value
    val currentDurationMs: Long get() = durationMs.value
    val currentBufferedPositionMs: Long get() = bufferedPositionMs.value

    fun play(track: Track, queue: List<Track> = emptyList())
    fun playFromContext(track: Track, queue: List<Track>) = play(track, queue)
    fun playQueueItem(index: Int)
    fun startRadio(track: Track)
    fun configureRadioRecommendations(provider: TrackRecommendationProvider?) {}
    fun play()
    fun pause()
    fun togglePlayPause()
    fun playNext()
    fun skipNext() = playNext()
    fun playPrevious()
    fun skipPrevious() = playPrevious()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setShuffleMode(enabled: Boolean)
    fun toggleShuffle()
    fun setRepeatMode(mode: RepeatMode)
    fun cycleRepeatMode()

    fun addToQueue(track: Track)
    fun playNextInQueue(track: Track)
    fun moveInQueue(fromIndex: Int, toIndex: Int)
    fun removeAt(index: Int)
    fun clearQueue()
    fun stopAndDismiss()
}

fun PlayerConnection.playTrack(
    track: org.shilpo.peerless.model.TrackSummaryDto,
    queue: List<org.shilpo.peerless.model.TrackSummaryDto> = emptyList()
) {
    play(
        Track.fromSummaryDto(track),
        queue.map { Track.fromSummaryDto(it) })
}

val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection> {
    error("No PlayerConnection provided")
}

expect fun platformSetup(playerConnection: PlayerConnection)
