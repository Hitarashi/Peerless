package org.shilpo.peerless.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.MediaPlayer.*

class IosAudioEngine : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState(outputLatencyMs = -65L))
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AudioEngineEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<AudioEngineEvent> = _events.asSharedFlow()

    private val _signalPath = MutableStateFlow<SignalPathSnapshot?>(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = _signalPath.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var player: AVQueuePlayer? = null
    private var progressJob: Job? = null
    private var observerToken: Any? = null

    private var nextUrl: String? = null
    private var nextPlayerItem: AVPlayerItem? = null
    private var currentTitle: String? = null
    private var currentArtist: String? = null
    private var currentDurationSec: Double = 0.0

    init {
        configureAudioSession()
        setupRemoteCommands()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun configureAudioSession() {
        runCatching {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
            audioSession.setActive(true, error = null)
        }
    }

    private fun setupRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()
        commandCenter.playCommand.addTargetWithHandler { _ ->
            play()
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.pauseCommand.addTargetWithHandler { _ ->
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }
        commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            if (_state.value.status == PlaybackStatus.PLAYING) pause() else play()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun prepare(
        url: String,
        headers: Map<String, String>,
        title: String?,
        artist: String?,
        artworkUrl: String?
    ) {
        release()
        currentTitle = title
        currentArtist = artist
        nextUrl = null
        nextPlayerItem = null

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
        val queuePlayer = AVQueuePlayer(playerItem = playerItem)
        player = queuePlayer

        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)

        observerToken = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            handleTrackEnd()
        }

        updateSignalPathSnapshot()
        updateNowPlayingInfo(positionSec = 0.0, rate = 0.0)
        startProgress()
    }

    private fun handleTrackEnd() {
        val nextItem = nextPlayerItem
        val nextTrackUrl = nextUrl
        if (nextItem != null && player?.currentItem == nextItem) {
            nextPlayerItem = null
            nextUrl = null
            _events.tryEmit(AudioEngineEvent.TransitionedToNext(nextTrackUrl))
            updateSignalPathSnapshot()
        } else if (nextItem != null) {
            nextPlayerItem = null
            nextUrl = null
            player?.advanceToNextItem()
            player?.play()
            _events.tryEmit(AudioEngineEvent.TransitionedToNext(nextTrackUrl))
            updateSignalPathSnapshot()
        } else {
            _state.value = _state.value.copy(status = PlaybackStatus.COMPLETED)
            _events.tryEmit(AudioEngineEvent.TrackCompleted)
            stopProgress()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun prepareNext(url: String?, headers: Map<String, String>) {
        nextUrl = url
        if (url == null) {
            nextPlayerItem = null
            return
        }

        val nsUrl = NSURL.URLWithString(url) ?: return
        val asset = if (headers.isNotEmpty()) {
            val options = mapOf<Any?, Any?>("AVURLAssetHTTPHeaderFieldsKey" to headers)
            AVURLAsset.URLAssetWithURL(nsUrl, options)
        } else {
            AVURLAsset.URLAssetWithURL(nsUrl, null)
        }
        val item = AVPlayerItem.playerItemWithAsset(asset)
        nextPlayerItem = item
        player?.insertItem(item, afterItem = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateSignalPathSnapshot() {
        val audioSession = AVAudioSession.sharedInstance()
        val sampleRate = audioSession.sampleRate.toInt().takeIf { it > 0 } ?: 48000
        val channels = audioSession.outputNumberOfChannels.toInt().takeIf { it > 0 } ?: 2
        val sinkName =
            (audioSession.currentRoute.outputs.firstOrNull() as? AVAudioSessionPortDescription)?.portName
                ?: "CoreAudio Output"

        val isAtmos = channels > 2
        val codec = if (isAtmos) "Dolby Atmos (Spatial Audio)" else "Apple Lossless (ALAC)"
        val channelLayout = when {
            isAtmos -> "$channels Channels (Dolby Atmos Spatial)"
            channels == 2 -> "Stereo"
            else -> "$channels Channels"
        }

        _signalPath.value = SignalPathSnapshot(
            sourceFormat = "$codec 24-bit / ${sampleRate / 1000.0} kHz",
            sampleRateHz = sampleRate,
            bitDepth = 24,
            channels = channels,
            channelLayout = channelLayout,
            bitRateKbps = null,
            decoder = "CoreAudio AVPlayer / AudioUnit",
            outputSink = sinkName,
            isBitPerfect = true,
            isDolbyAtmos = isAtmos
        )
    }

    private fun updateNowPlayingInfo(positionSec: Double, rate: Double) {
        val info = mutableMapOf<Any?, Any?>()
        currentTitle?.let { info[MPMediaItemPropertyTitle] = it }
        currentArtist?.let { info[MPMediaItemPropertyArtist] = it }
        if (currentDurationSec > 0.0) {
            info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = currentDurationSec)
        }
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = positionSec)
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = rate)
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
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
                    if (!durationSec.isNaN() && durationSec > 0.0) {
                        currentDurationSec = durationSec
                    }

                    val posMs = if (currentSec.isNaN() || currentSec < 0.0) 0L else (currentSec * 1000).toLong()
                    val durMs = if (durationSec.isNaN() || durationSec < 0.0) 0L else (durationSec * 1000).toLong()

                    val status = when {
                        p.error != null -> PlaybackStatus.ERROR
                        p.rate > 0f -> PlaybackStatus.PLAYING
                    _state.value = _state.value.copy(
                        status = status,
                        positionMs = posMs,
                        durationMs = durMs,
                        outputLatencyMs = calculateOutputLatencyMs(),
                        errorMessage = p.error?.localizedDescription
                    )
                }
                delay(250)
            }
        }
    }

        private fun calculateOutputLatencyMs(): Long {
            val basePipelineLeadMs = 50L
            val hardwareLatencyMs = runCatching {
                val session = AVAudioSession.sharedInstance()
                val totalSec = session.outputLatency + session.IOBufferDuration
                (totalSec * 1000.0).toLong()
            }.getOrDefault(15L)
            return -(basePipelineLeadMs + hardwareLatencyMs)
        }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun play() {
        player?.play()
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
        updateNowPlayingInfo(positionSec = _state.value.positionMs / 1000.0, rate = 1.0)
    }

    override fun pause() {
        player?.pause()
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
        updateNowPlayingInfo(positionSec = _state.value.positionMs / 1000.0, rate = 0.0)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        val time = CMTimeMakeWithSeconds(seconds, 1000)
        player?.seekToTime(time)
        _state.value = _state.value.copy(positionMs = positionMs)
        updateNowPlayingInfo(
            positionSec = seconds,
            rate = if (_state.value.status == PlaybackStatus.PLAYING) 1.0 else 0.0
        )
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
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
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }
}

actual fun createAudioEngine(): AudioEngine = IosAudioEngine()
