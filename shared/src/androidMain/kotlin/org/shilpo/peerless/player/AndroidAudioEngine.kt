package org.shilpo.peerless.player

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AndroidAudioContextHolder {
    @Volatile
    var context: Context? = null
        get() {
            if (field == null) {
                field = resolveApplicationContext()
            }
            return field
        }

    private fun resolveApplicationContext(): Context? {
        return runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = activityThreadClass.getMethod("currentApplication")
            currentAppMethod.invoke(null) as? Context
        }.getOrNull()
    }
}

object AndroidMediaSessionHolder {
    @Volatile
    var player: ExoPlayer? = null

    @Volatile
    var mediaSession: MediaSession? = null
}

@androidx.annotation.OptIn(UnstableApi::class)
class AndroidAudioEngine(
    private val contextProvider: () -> Context? = { AndroidAudioContextHolder.context }
) : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AudioEngineEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<AudioEngineEvent> = _events.asSharedFlow()

    private val _signalPath = MutableStateFlow<SignalPathSnapshot?>(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = _signalPath.asStateFlow()

    private val spectrumAnalyzer = AudioSpectrumAnalyzer()
    override val spectrum: StateFlow<AudioSpectrumFrame?> = spectrumAnalyzer.frame

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressJob: Job? = null
    private var artworkJob: Job? = null

    @Volatile
    private var currentTitle: String? = null

    private fun getOrCreatePlayer(): ExoPlayer? {
        AndroidMediaSessionHolder.player?.let { return it }
        val ctx = contextProvider() ?: return null

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val spectrumProcessor = SpectrumAudioProcessor(spectrumAnalyzer)
        val renderersFactory = object : DefaultRenderersFactory(ctx) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                .setAudioProcessors(arrayOf(spectrumProcessor))
                .build()
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val player = ExoPlayer.Builder(ctx, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerStatus()
                if (playbackState == Player.STATE_ENDED) {
                    _events.tryEmit(AudioEngineEvent.TrackCompleted)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerStatus()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlayerStatus()
                updateSignalPathSnapshot(player.audioFormat)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    val uriStr = mediaItem?.localConfiguration?.uri?.toString()
                    _events.tryEmit(AudioEngineEvent.TransitionedToNext(uriStr))
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateSignalPathSnapshot(player.audioFormat)
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e(
                    "AndroidAudioEngine",
                    "ExoPlayer error: ${error.errorCodeName} (${error.errorCode})",
                    error
                )
                _state.value = _state.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = error.message
                )
                _events.tryEmit(
                    AudioEngineEvent.Error(
                        error.message ?: "Unknown playback error",
                        error.errorCode
                    )
                )
                stopProgressUpdates()
            }
        })

        val session = MediaSession.Builder(ctx, player).build()

        AndroidMediaSessionHolder.player = player
        AndroidMediaSessionHolder.mediaSession = session
        return player
    }

    private fun updateSignalPathSnapshot(format: Format?) {
        val sampleRate = format?.sampleRate?.takeIf { it > 0 } ?: 48000
        val channelCount = format?.channelCount?.takeIf { it > 0 } ?: 2
        val mimeType = format?.sampleMimeType ?: "audio/alac"
        val bitRate = format?.bitrate?.takeIf { it > 0 }?.let { it / 1000 }

        val isAtmos = mimeType == MimeTypes.AUDIO_E_AC3_JOC ||
                mimeType == MimeTypes.AUDIO_E_AC3 ||
                mimeType.contains("dolby") ||
                mimeType.contains("eac3") ||
                channelCount > 2

        val codecName = when {
            isAtmos -> "Dolby Atmos (E-AC-3 JOC)"
            mimeType.contains("flac") -> "FLAC"
            mimeType.contains("alac") -> "ALAC"
            mimeType.contains("mp4a") || mimeType.contains("aac") -> "AAC"
            else -> mimeType.substringAfterLast('/')
        }

        val bitDepth = when {
            format?.pcmEncoding == C.ENCODING_PCM_FLOAT -> 32
            format?.pcmEncoding == C.ENCODING_PCM_32BIT -> 32
            format?.pcmEncoding == C.ENCODING_PCM_24BIT -> 24
            else -> 24
        }

        val channelLayoutDesc = when {
            isAtmos -> "Dolby Atmos (Spatial)"
            channelCount == 2 -> "Stereo"
            channelCount == 6 -> "5.1 Surround"
            channelCount == 8 -> "7.1 Surround"
            else -> "$channelCount Channels"
        }

        _signalPath.value = SignalPathSnapshot(
            sourceFormat = "$codecName $bitDepth-bit / ${sampleRate / 1000.0} kHz",
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            channels = channelCount,
            channelLayout = channelLayoutDesc,
            bitRateKbps = bitRate,
            decoder = "Media3 ExoPlayer / AudioTrack (Direct HAL)",
            outputSink = "AudioTrack Direct",
            isBitPerfect = false,
            isDolbyAtmos = isAtmos
        )
    }

    private fun updatePlayerStatus() {
        val player = AndroidMediaSessionHolder.player ?: return
        val playbackState = player.playbackState
        val isPlaying = player.isPlaying

        val status = when (playbackState) {
            Player.STATE_IDLE -> PlaybackStatus.IDLE
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
            Player.STATE_ENDED -> PlaybackStatus.COMPLETED
            else -> PlaybackStatus.IDLE
        }

        val duration = if (player.duration > 0) player.duration else _state.value.durationMs
        val position = player.currentPosition

        _state.value = _state.value.copy(
            status = status,
            positionMs = position,
            durationMs = duration,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
        )

        if (status == PlaybackStatus.PLAYING) {
            startProgressUpdates()
        } else {
            stopProgressUpdates()
        }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val player = AndroidMediaSessionHolder.player
                if (player != null && player.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = player.currentPosition,
                        durationMs = if (player.duration > 0) player.duration else _state.value.durationMs,
                        bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun prepare(
        url: String,
        headers: Map<String, String>,
        title: String?,
        artist: String?,
        artworkUrl: String?
    ) {
        currentTitle = title
        spectrumAnalyzer.reset()
        artworkJob?.cancel()
        mainHandler.post {
            val player = getOrCreatePlayer()
            if (player == null) {
                _state.value = _state.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Android Context is not available to initialize ExoPlayer"
                )
                return@post
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)
            updateSignalPathSnapshot(player.audioFormat)

            if (!artworkUrl.isNullOrBlank()) {
                android.util.Log.d("AndroidAudioEngine", "Loading artwork bytes for media session")
                loadMediaSessionArtwork(
                    player = player,
                    mediaUri = url,
                    artworkUrl = artworkUrl,
                    headers = headers
                )
            } else {
                android.util.Log.w(
                    "AndroidAudioEngine",
                    "No artwork URL was provided for media session metadata"
                )
            }
        }
    }

    private fun loadMediaSessionArtwork(
        player: ExoPlayer,
        mediaUri: String,
        artworkUrl: String,
        headers: Map<String, String>
    ) {
        artworkJob = scope.launch(Dispatchers.IO) {
            val artworkResult = runCatching {
                val connection = URL(artworkUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 8_000
                    connection.setRequestProperty("User-Agent", "Peerless Android")
                    headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            check(totalBytes <= MAX_ARTWORK_BYTES) { "Artwork exceeds media-session limit" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                } finally {
                    connection.disconnect()
                }
            }
            val artworkBytes = artworkResult.getOrElse { error ->
                android.util.Log.w("AndroidAudioEngine", "Unable to load media artwork", error)
                return@launch
            }
            if (artworkBytes.isEmpty()) {
                android.util.Log.w("AndroidAudioEngine", "Media artwork response was empty")
                return@launch
            }

            withContext(Dispatchers.Main) {
                val index = player.currentMediaItemIndex
                val currentItem = player.currentMediaItem ?: run {
                    android.util.Log.w(
                        "AndroidAudioEngine",
                        "Media item disappeared before artwork loaded"
                    )
                    return@withContext
                }
                if (currentItem.localConfiguration?.uri?.toString() != mediaUri) {
                    android.util.Log.d(
                        "AndroidAudioEngine",
                        "Discarding artwork for a no longer active track"
                    )
                    return@withContext
                }

                val metadata = currentItem.mediaMetadata.buildUpon()
                    .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
                player.replaceMediaItem(
                    index,
                    currentItem.buildUpon().setMediaMetadata(metadata).build()
                )
                android.util.Log.d(
                    "AndroidAudioEngine",
                    "Attached ${artworkBytes.size} bytes of artwork to media session"
                )
            }
        }
    }

    override fun prepareNext(url: String?, headers: Map<String, String>) {
        mainHandler.post {
            val player = AndroidMediaSessionHolder.player ?: return@post
            if (url != null) {
                if (player.mediaItemCount > 1) {
                    player.removeMediaItem(1)
                }
                player.addMediaItem(MediaItem.fromUri(url))
            } else {
                while (player.mediaItemCount > 1) {
                    player.removeMediaItem(1)
                }
            }
        }
    }

    override fun play() {
        mainHandler.post {
            val player = getOrCreatePlayer() ?: return@post
            player.play()
            val ctx = contextProvider()
            if (ctx != null) {
                runCatching {
                    val intent = Intent(ctx, PeerlessMediaService::class.java)
                    ContextCompat.startForegroundService(ctx, intent)
                }
            }
        }
    }

    override fun pause() {
        mainHandler.post {
            AndroidMediaSessionHolder.player?.pause()
        }
    }

    override fun stop() {
        mainHandler.post {
            AndroidMediaSessionHolder.player?.stop()
            _state.value = _state.value.copy(status = PlaybackStatus.IDLE)
        }
    }

    override fun seekTo(positionMs: Long) {
        mainHandler.post {
            AndroidMediaSessionHolder.player?.seekTo(positionMs)
            _state.value = _state.value.copy(positionMs = positionMs)
        }
    }

    override fun setVolume(volume: Float) {
        mainHandler.post {
            AndroidMediaSessionHolder.player?.volume = volume.coerceIn(0f, 1f)
        }
    }

    override fun release() {
        mainHandler.post {
            artworkJob?.cancel()
            artworkJob = null
            stopProgressUpdates()
            AndroidMediaSessionHolder.mediaSession?.release()
            AndroidMediaSessionHolder.mediaSession = null
            AndroidMediaSessionHolder.player?.release()
            AndroidMediaSessionHolder.player = null
            spectrumAnalyzer.reset()
            _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
        }
    }

    private companion object {
        const val MAX_ARTWORK_BYTES = 8 * 1024 * 1024
    }
}

actual fun createAudioEngine(): AudioEngine = AndroidAudioEngine()

@androidx.annotation.OptIn(UnstableApi::class)
private class SpectrumAudioProcessor(
    private val analyzer: AudioSpectrumAnalyzer
) : BaseAudioProcessor() {
    private var sampleBuffer = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val supportedEncoding = inputAudioFormat.encoding in setOf(
            C.ENCODING_PCM_8BIT,
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_24BIT,
            C.ENCODING_PCM_32BIT,
            C.ENCODING_PCM_FLOAT
        )
        return if (supportedEncoding) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val format = inputAudioFormat
        val frameSize = format.bytesPerFrame
        val channels = format.channelCount
        val byteCount = inputBuffer.remaining()
        if (frameSize > 0 && channels > 0 && format.sampleRate > 0) {
            val sampleCount = byteCount / (frameSize / channels)
            if (sampleBuffer.size < sampleCount) sampleBuffer = FloatArray(sampleCount)
            val sampleView = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
            var index = 0
            while (sampleView.remaining() >= frameSize) {
                repeat(channels) {
                    sampleBuffer[index++] = readSample(sampleView, format.encoding)
                }
            }
            if (index > 0) {
                analyzer.acceptInterleavedPcm(
                    samples = sampleBuffer,
                    channels = channels,
                    sampleRate = format.sampleRate,
                    sampleCount = index
                )
            }
        }

        val output = replaceOutputBuffer(byteCount)
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        analyzer.reset()
    }

    override fun onReset() {
        analyzer.reset()
        sampleBuffer = FloatArray(0)
    }

    private fun readSample(buffer: ByteBuffer, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_FLOAT -> buffer.float.coerceIn(-1f, 1f)
        C.ENCODING_PCM_8BIT -> ((buffer.get().toInt() and 0xff) - 128) / 128f
        C.ENCODING_PCM_16BIT -> buffer.short / 32768f
        C.ENCODING_PCM_24BIT -> {
            val first = buffer.get().toInt() and 0xff
            val second = buffer.get().toInt() and 0xff
            val third = buffer.get().toInt() and 0xff
            val value = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                first or (second shl 8) or (third shl 16)
            } else {
                third or (second shl 8) or (first shl 16)
            }
            val signedValue = if (value and 0x800000 != 0) value or -0x1000000 else value
            signedValue / 8_388_608f
        }

        C.ENCODING_PCM_32BIT -> buffer.int / 2_147_483_648f
        else -> 0f
    }
}
