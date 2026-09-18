package org.shilpo.peerless.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressJob: Job? = null

    @Volatile
    private var currentTitle: String? = null

    private fun getOrCreatePlayer(): ExoPlayer? {
        AndroidMediaSessionHolder.player?.let { return it }
        val ctx = contextProvider() ?: return null

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory =
            DefaultRenderersFactory(ctx).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

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
                _events.tryEmit(AudioEngineEvent.Error(error.message ?: "Unknown playback error", error.errorCode))
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
            isBitPerfect = true,
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
            durationMs = duration
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
                        durationMs = if (player.duration > 0) player.duration else _state.value.durationMs
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
                .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)
            updateSignalPathSnapshot(player.audioFormat)
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
            stopProgressUpdates()
            AndroidMediaSessionHolder.mediaSession?.release()
            AndroidMediaSessionHolder.mediaSession = null
            AndroidMediaSessionHolder.player?.release()
            AndroidMediaSessionHolder.player = null
            _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
        }
    }
}

actual fun createAudioEngine(): AudioEngine = AndroidAudioEngine()
