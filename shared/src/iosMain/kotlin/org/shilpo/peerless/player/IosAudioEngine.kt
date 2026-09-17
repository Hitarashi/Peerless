package org.shilpo.peerless.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

class IosAudioEngine : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var player: AVPlayer? = null
    private var progressJob: Job? = null
    private var observerToken: Any? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun prepare(url: String, headers: Map<String, String>) {
        release()
        val nsUrl = NSURL.URLWithString(url) ?: run {
            _state.value = _state.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Invalid URL: $url"
            )
            return
        }

        val asset = if (headers.isNotEmpty()) {
            val options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to headers)
            AVURLAsset.URLAssetWithURL(nsUrl, options)
        } else {
            AVURLAsset.URLAssetWithURL(nsUrl, null)
        }

        val playerItem = AVPlayerItem.playerItemWithAsset(asset)
        val newPlayer = AVPlayer.playerWithPlayerItem(playerItem)
        player = newPlayer

        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)

        observerToken = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            _state.value = _state.value.copy(status = PlaybackStatus.COMPLETED)
            stopProgress()
        }

        startProgress()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startProgress() {
        stopProgress()
        progressJob = scope.launch {
            while (isActive) {
                val p = player
                if (p != null) {
                    val currentSec = CMTimeGetSeconds(p.currentTime())
                    val item = p.currentItem
                    val durationSec = if (item != null) CMTimeGetSeconds(item.duration) else 0.0

                    val posMs = if (currentSec.isNaN() || currentSec < 0.0) 0L else (currentSec * 1000).toLong()
                    val durMs = if (durationSec.isNaN() || durationSec < 0.0) 0L else (durationSec * 1000).toLong()

                    val status = when {
                        p.error != null -> PlaybackStatus.ERROR
                        p.rate > 0f -> PlaybackStatus.PLAYING
                        _state.value.status == PlaybackStatus.BUFFERING -> PlaybackStatus.BUFFERING
                        _state.value.status == PlaybackStatus.COMPLETED -> PlaybackStatus.COMPLETED
                        else -> PlaybackStatus.PAUSED
                    }

                    _state.value = _state.value.copy(
                        status = status,
                        positionMs = posMs,
                        durationMs = durMs,
                        errorMessage = p.error?.localizedDescription
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun play() {
        player?.play()
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
    }

    override fun pause() {
        player?.pause()
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        val time = CMTimeMakeWithSeconds(seconds, 1000)
        player?.seekToTime(time)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun release() {
        stopProgress()
        observerToken?.let { token ->
            NSNotificationCenter.defaultCenter.removeObserver(token)
            observerToken = null
        }
        player?.pause()
        player = null
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
    }
}

actual fun createAudioEngine(): AudioEngine = IosAudioEngine()
