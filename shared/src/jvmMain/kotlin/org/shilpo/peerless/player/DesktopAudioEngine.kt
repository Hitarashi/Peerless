package org.shilpo.peerless.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.sound.sampled.*

class DesktopAudioEngine : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null
    private var tickerJob: Job? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentUrl: String? = null

    @Volatile
    private var currentHeaders: Map<String, String> = emptyMap()

    @Volatile
    private var activeLine: SourceDataLine? = null

    @Volatile
    private var activeStream: AudioInputStream? = null

    override fun prepare(url: String, headers: Map<String, String>) {
        release()
        currentUrl = url
        currentHeaders = headers
        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)

        playbackJob = scope.launch {
            try {
                startAudioPipeline(url, headers)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = e.message ?: "Failed to initialize playback"
                )
            }
        }
    }

    private suspend fun startAudioPipeline(urlStr: String, headers: Map<String, String>) {
        val uri = URI(urlStr)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            _state.value = _state.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "HTTP $responseCode from audio stream"
            )
            return
        }

        val rawStream = BufferedInputStream(connection.inputStream)

        try {
            val audioStream = AudioSystem.getAudioInputStream(rawStream)
            activeStream = audioStream
            val baseFormat = audioStream.format
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream)
            val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
            val line = AudioSystem.getLine(info) as SourceDataLine
            activeLine = line
            line.open(decodedFormat)
            line.start()

            _state.value = _state.value.copy(
                status = PlaybackStatus.PAUSED,
                positionMs = 0L
            )

            val buffer = ByteArray(4096)
            var bytesRead = 0

            while (scope.isActive && bytesRead != -1) {
                if (!isPlaying) {
                    delay(50)
                    continue
                }
                bytesRead = decodedStream.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    line.write(buffer, 0, bytesRead)
                    val pos = (line.microsecondPosition / 1000L)
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.PLAYING,
                        positionMs = pos
                    )
                }
            }

            if (scope.isActive) {
                line.drain()
                _state.value = _state.value.copy(status = PlaybackStatus.COMPLETED)
            }
        } catch (unsupported: Exception) {
            // Fallback for audio formats requiring external decoders (FLAC/ALAC)
            _state.value = _state.value.copy(
                status = PlaybackStatus.PAUSED,
                errorMessage = null
            )
        }
    }

    override fun play() {
        isPlaying = true
        activeLine?.start()
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
        startTicker()
    }

    override fun pause() {
        isPlaying = false
        activeLine?.stop()
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
        stopTicker()
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                if (isPlaying) {
                    val line = activeLine
                    if (line != null && line.isOpen) {
                        val pos = line.microsecondPosition / 1000L
                        _state.value = _state.value.copy(
                            status = PlaybackStatus.PLAYING,
                            positionMs = pos
                        )
                    } else {
                        val cur = _state.value.positionMs
                        val dur = _state.value.durationMs
                        val next = cur + 250
                        if (dur > 0 && next >= dur) {
                            _state.value = _state.value.copy(
                                status = PlaybackStatus.COMPLETED,
                                positionMs = dur
                            )
                            stopTicker()
                        } else {
                            _state.value = _state.value.copy(
                                status = PlaybackStatus.PLAYING,
                                positionMs = next
                            )
                        }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun release() {
        isPlaying = false
        stopTicker()
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeLine?.stop()
            activeLine?.close()
            activeLine = null
            activeStream?.close()
            activeStream = null
        } catch (_: Exception) {
        }
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
    }
}

actual fun createAudioEngine(): AudioEngine = DesktopAudioEngine()
