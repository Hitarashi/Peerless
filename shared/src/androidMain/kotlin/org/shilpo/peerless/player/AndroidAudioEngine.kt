package org.shilpo.peerless.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class AndroidAudioEngine(
    private val contextProvider: () -> Context? = { AndroidAudioContextHolder.context }
) : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private fun getOrCreatePlayer(): ExoPlayer? {
        if (exoPlayer != null) return exoPlayer
        val ctx = contextProvider() ?: return null
        val player = ExoPlayer.Builder(ctx).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updatePlayerStatus()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayerStatus()
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = error.message
                    )
                    stopProgressUpdates()
                }
            })
        }
        exoPlayer = player
        return player
    }

    private fun updatePlayerStatus() {
        val player = exoPlayer ?: return
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
                val player = exoPlayer
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

    override fun prepare(url: String, headers: Map<String, String>) {
        mainHandler.post {
            val ctx = contextProvider()
            if (ctx == null) {
                _state.value = _state.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Android Context is not available to initialize ExoPlayer"
                )
                return@post
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            if (headers.isNotEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(headers)
            }
            val mediaSourceFactory = DefaultMediaSourceFactory(ctx)
                .setDataSourceFactory(httpDataSourceFactory)

            val player = ExoPlayer.Builder(ctx)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()

            exoPlayer?.release()
            exoPlayer = player

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updatePlayerStatus()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayerStatus()
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = error.message
                    )
                    stopProgressUpdates()
                }
            })

            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)
        }
    }

    override fun play() {
        mainHandler.post {
            val player = getOrCreatePlayer()
            player?.play()
        }
    }

    override fun pause() {
        mainHandler.post {
            exoPlayer?.pause()
        }
    }

    override fun seekTo(positionMs: Long) {
        mainHandler.post {
            exoPlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(positionMs = positionMs)
        }
    }

    override fun release() {
        mainHandler.post {
            stopProgressUpdates()
            exoPlayer?.release()
            exoPlayer = null
            _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
        }
    }
}

actual fun createAudioEngine(): AudioEngine = AndroidAudioEngine()
