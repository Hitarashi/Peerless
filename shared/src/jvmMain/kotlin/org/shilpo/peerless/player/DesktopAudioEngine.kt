package org.shilpo.peerless.player

import com.sun.jna.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("FunctionName")
interface LibMpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_destroy(ctx: Pointer)
    fun mpv_terminate_destroy(ctx: Pointer)

    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_command_string(ctx: Pointer, args: String): Int

    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(data: Pointer?)

    fun mpv_observe_property(ctx: Pointer, reply_userdata: Long, name: String, format: Int): Int
    fun mpv_unobserve_property(ctx: Pointer, registered_reply_userdata: Long): Int

    fun mpv_wait_event(ctx: Pointer, timeout: Double): MpvEventStructure.ByReference?
    fun mpv_error_string(error: Int): String?
}

@Suppress("FunctionName")
interface CStandardLibrary : Library {
    fun setlocale(category: Int, locale: String): Pointer?
}

internal object LocaleFixer {
    private val fixed = AtomicBoolean(false)

    fun ensureCLocale() {
        if (fixed.getAndSet(true)) return
        runCatching {
            val libName = when {
                Platform.isWindows() -> "msvcrt"
                Platform.isMac() -> "System"
                else -> "c"
            }
            val crt = Native.load(libName, CStandardLibrary::class.java)
            for (category in 0..6) {
                crt.setlocale(category, "C")
            }
        }.onFailure {
            System.err.println("Peerless: Failed to force C locale via libc: ${it.message}")
        }
    }
}

@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
open class MpvEventStructure : Structure() {
    class ByReference : MpvEventStructure(), Structure.ByReference

    @JvmField
    var event_id: Int = 0

    @JvmField
    var error: Int = 0

    @JvmField
    var reply_userdata: Long = 0L

    @JvmField
    var data: Pointer? = null
}

object MpvConstants {
    const val EVENT_NONE = 0
    const val EVENT_SHUTDOWN = 1
    const val EVENT_LOG_MESSAGE = 2
    const val EVENT_START_FILE = 6
    const val EVENT_END_FILE = 7
    const val EVENT_FILE_LOADED = 8
    const val EVENT_IDLE = 11
    const val EVENT_PAUSE = 12
    const val EVENT_UNPAUSE = 13
    const val EVENT_SEEK = 20
    const val EVENT_PLAYBACK_RESTART = 21
    const val EVENT_PROPERTY_CHANGE = 22

    const val FORMAT_NONE = 0
    const val FORMAT_STRING = 1
    const val FORMAT_OSD_STRING = 2
    const val FORMAT_FLAG = 3
    const val FORMAT_INT64 = 4
    const val FORMAT_DOUBLE = 5
}

internal object MpvLoader {
    private val candidatePaths = listOf(
        "/usr/lib/libmpv.so.2",
        "/usr/lib64/libmpv.so.2",
        "/usr/local/lib/libmpv.so.2",
        "/usr/lib/libmpv.so",
        "/usr/lib/x86_64-linux-gnu/libmpv.so.2",
        "/usr/lib/aarch64-linux-gnu/libmpv.so.2",
        "/opt/homebrew/lib/libmpv.dylib",
        "/usr/local/lib/libmpv.dylib",
        "mpv-2.dll",
        "libmpv-2.dll"
    )

    fun load(): LibMpv {
        LocaleFixer.ensureCLocale()

        try {
            return Native.load("mpv", LibMpv::class.java)
        } catch (_: Throwable) {
        }

        try {
            return Native.load("mpv-2", LibMpv::class.java)
        } catch (_: Throwable) {
        }

        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists()) {
                try {
                    return Native.load(file.absolutePath, LibMpv::class.java)
                } catch (_: Throwable) {
                }
            }
        }

        throw UnsatisfiedLinkError("Could not dynamically load libmpv. Ensure libmpv (or mpv) is installed on the system.")
    }
}

class DesktopAudioEngine : AudioEngine {
    private val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AudioEngineEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<AudioEngineEvent> = _events.asSharedFlow()

    private val _signalPath = MutableStateFlow<SignalPathSnapshot?>(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = _signalPath.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isReleased = AtomicBoolean(false)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentVolume = 1.0f

    @Volatile
    private var currentTrackTitle: String? = null

    @Volatile
    private var nextPreloadedUrl: String? = null

    private var mpvContext: Pointer? = null
    private var libMpvInstance: LibMpv? = null
    private var eventLoopJob: Job? = null

    init {
        scope.launch {
            initMpv()
        }
    }

    private fun initMpv(): Boolean {
        if (mpvContext != null) return true
        LocaleFixer.ensureCLocale()

        return try {
            val lib = MpvLoader.load()
            libMpvInstance = lib
            val ctx = lib.mpv_create() ?: error("mpv_create returned null")

            lib.mpv_set_option_string(ctx, "vo", "null")
            lib.mpv_set_option_string(ctx, "audio-display", "no")
            lib.mpv_set_option_string(ctx, "idle", "yes")
            lib.mpv_set_option_string(ctx, "keep-open", "always")
            lib.mpv_set_option_string(ctx, "gapless-audio", "yes")
            lib.mpv_set_option_string(ctx, "audio-pitch-correction", "yes")
            lib.mpv_set_option_string(ctx, "audio-client-name", "Peerless")
            lib.mpv_set_option_string(ctx, "terminal", "yes")
            lib.mpv_set_option_string(ctx, "msg-level", "all=warn,ao=info")
            lib.mpv_set_option_string(ctx, "cache", "yes")
            lib.mpv_set_option_string(ctx, "demuxer-max-bytes", "150MiB")
            lib.mpv_set_option_string(ctx, "demuxer-readahead-secs", "120")

            val initStatus = lib.mpv_initialize(ctx)
            if (initStatus < 0) {
                lib.mpv_destroy(ctx)
                error("mpv_initialize failed with code $initStatus")
            }

            mpvContext = ctx

            lib.mpv_observe_property(ctx, 1L, "time-pos", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 2L, "duration", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 3L, "pause", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 4L, "eof-reached", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 5L, "audio-params", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 6L, "audio-codec-name", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 7L, "audio-bitrate", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 8L, "demuxer-cache-time", MpvConstants.FORMAT_NONE)
            lib.mpv_observe_property(ctx, 9L, "demuxer-cache-duration", MpvConstants.FORMAT_NONE)

            setVolume(currentVolume)
            startEventLoop()
            true
        } catch (e: Throwable) {
            System.err.println("Peerless DesktopAudioEngine: initMpv failed: ${e.message}")
            _state.value = _state.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Failed to load libmpv: ${e.message}"
            )
            _events.tryEmit(AudioEngineEvent.Error("Failed to initialize libmpv: ${e.message}"))
            false
        }
    }

    private fun getProperty(name: String): String? {
        val ctx = mpvContext ?: return null
        val lib = libMpvInstance ?: return null
        val ptr = lib.mpv_get_property_string(ctx, name) ?: return null
        return try {
            ptr.getString(0, "UTF-8")
        } finally {
            lib.mpv_free(ptr)
        }
    }

    private fun setProperty(name: String, value: String) {
        val ctx = mpvContext ?: return
        val lib = libMpvInstance ?: return
        lib.mpv_set_property_string(ctx, name, value)
    }

    private fun executeCommand(vararg args: String) {
        val ctx = mpvContext ?: return
        val lib = libMpvInstance ?: return
        val cmdArgs = arrayOf<String?>(*args, null)
        lib.mpv_command(ctx, cmdArgs)
    }

    private fun startEventLoop() {
        eventLoopJob?.cancel()
        eventLoopJob = scope.launch(Dispatchers.IO) {
            val lib = libMpvInstance ?: return@launch
            val ctx = mpvContext ?: return@launch

            while (isActive && !isReleased.get()) {
                val event = lib.mpv_wait_event(ctx, 0.1) ?: continue
                when (event.event_id) {
                    MpvConstants.EVENT_NONE -> {}
                    MpvConstants.EVENT_START_FILE -> {
                        val posStr = getProperty("playlist-pos")
                        val pos = posStr?.toIntOrNull() ?: 0
                        if (pos > 0 && nextPreloadedUrl != null) {
                            val url = nextPreloadedUrl
                            nextPreloadedUrl = null
                            _events.emit(AudioEngineEvent.TransitionedToNext(url))
                        }
                    }

                    MpvConstants.EVENT_FILE_LOADED -> {
                        updateSignalPathSnapshot()
                        val durSec = getProperty("duration")?.toDoubleOrNull()
                        if (durSec != null) {
                            _state.value = _state.value.copy(durationMs = (durSec * 1000.0).toLong())
                        }
                        if (isPlaying) {
                            setProperty("pause", "no")
                            _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
                            startProgressUpdates()
                        }
                    }

                    MpvConstants.EVENT_END_FILE -> {
                        val eofReached = getProperty("eof-reached") == "yes"
                        val playlistCount = getProperty("playlist-count")?.toIntOrNull() ?: 1
                        val playlistPos = getProperty("playlist-pos")?.toIntOrNull() ?: 0

                        if (eofReached && playlistPos >= playlistCount - 1 && isPlaying) {
                            isPlaying = false
                            stopProgressUpdates()
                            _state.value = _state.value.copy(status = PlaybackStatus.COMPLETED)
                            _events.emit(AudioEngineEvent.TrackCompleted)
                        }
                    }

                    MpvConstants.EVENT_PROPERTY_CHANGE -> {
                        handlePropertyChange()
                    }

                    MpvConstants.EVENT_SHUTDOWN -> {
                        break
                    }
                }
            }
        }
    }

    private fun handlePropertyChange() {
        val timePosSec = getProperty("time-pos")?.toDoubleOrNull()
        if (timePosSec != null) {
            val ms = (timePosSec * 1000.0).toLong()
            _state.value = _state.value.copy(
                positionMs = ms,
                status = if (isPlaying) PlaybackStatus.PLAYING else _state.value.status
            )
        }

        val durSec = getProperty("duration")?.toDoubleOrNull()
        if (durSec != null) {
            _state.value = _state.value.copy(durationMs = (durSec * 1000.0).toLong())
        }

        val cacheDurSec = getProperty("demuxer-cache-duration")?.toDoubleOrNull()
        val cacheTimeSec = getProperty("demuxer-cache-time")?.toDoubleOrNull()
        val bufMs = when {
            cacheDurSec != null && cacheDurSec > 0.0 && timePosSec != null -> ((timePosSec + cacheDurSec) * 1000.0).toLong()
                .coerceAtLeast((timePosSec * 1000.0).toLong())

            cacheTimeSec != null && cacheTimeSec > 0.0 -> (cacheTimeSec * 1000.0).toLong()
            else -> null
        }
        if (bufMs != null) {
            _state.value = _state.value.copy(bufferedPositionMs = bufMs)
        }

        val pauseVal = getProperty("pause")
        if (pauseVal == "yes") {
            isPlaying = false
            stopProgressUpdates()
            _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
        } else if (pauseVal == "no") {
            isPlaying = true
            _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
            startProgressUpdates()
        }

        val eofVal = getProperty("eof-reached")
        if (eofVal == "yes" && isPlaying) {
            val playlistCount = getProperty("playlist-count")?.toIntOrNull() ?: 1
            val playlistPos = getProperty("playlist-pos")?.toIntOrNull() ?: 0
            if (playlistPos >= playlistCount - 1) {
                isPlaying = false
                _state.value = _state.value.copy(status = PlaybackStatus.COMPLETED)
                scope.launch {
                    _events.emit(AudioEngineEvent.TrackCompleted)
                }
            }
        }

        updateSignalPathSnapshot()
    }

    private fun updateSignalPathSnapshot() {
        val codec = getProperty("audio-codec-name") ?: ""
        val sampleRate = getProperty("audio-params/samplerate")?.toIntOrNull() ?: 0
        val channels = getProperty("audio-params/channels")?.toIntOrNull() ?: 2
        val hrChannels = getProperty("audio-params/hr-channels") ?: ""
        val format = getProperty("audio-params/format") ?: ""
        val bitrate = getProperty("audio-bitrate")?.toIntOrNull()?.let { it / 1000 }
        val outputDevice = getProperty("audio-out-detected-device")
            ?: getProperty("audio-device")
            ?: "System Default"

        if (sampleRate == 0 && codec.isEmpty()) return

        val bitDepth = when {
            format.contains("32") -> 32
            format.contains("24") -> 24
            format.contains("float") || format.contains("f32") -> 32
            else -> 16
        }

        val lowerCodec = codec.lowercase(Locale.ROOT)
        val isAtmos = lowerCodec.contains("eac3") ||
                lowerCodec.contains("ac4") ||
                lowerCodec.contains("truehd") ||
                hrChannels.contains("atmos", ignoreCase = true) ||
                hrChannels.contains("7.1.4") ||
                hrChannels.contains("5.1.2")

        val channelLayoutDesc = when {
            isAtmos && hrChannels.isNotBlank() -> "$hrChannels (Dolby Atmos)"
            isAtmos -> "Dolby Atmos Multichannel"
            hrChannels.isNotBlank() -> hrChannels
            channels == 2 -> "Stereo"
            channels == 6 -> "5.1 Surround"
            channels == 8 -> "7.1 Surround"
            else -> "$channels Channels"
        }

        val formattedCodec = when (lowerCodec) {
            "alac" -> "ALAC"
            "flac" -> "FLAC"
            "eac3" -> "Dolby Digital Plus (E-AC-3)"
            "ac4" -> "Dolby AC-4"
            "truehd" -> "Dolby TrueHD"
            "aac" -> "AAC"
            "opus" -> "Opus"
            else -> codec.uppercase(Locale.ROOT)
        }

        val sourceFmt = "$formattedCodec $bitDepth-bit / ${sampleRate / 1000.0} kHz"

        _signalPath.value = SignalPathSnapshot(
            sourceFormat = sourceFmt,
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            channels = channels,
            channelLayout = channelLayoutDesc,
            bitRateKbps = bitrate,
            decoder = "libmpv (ffmpeg/$codec)",
            outputSink = outputDevice,
            isBitPerfect = true,
            isDolbyAtmos = isAtmos
        )
    }

    override fun prepare(
        url: String,
        headers: Map<String, String>,
        title: String?,
        artist: String?,
        artworkUrl: String?
    ) {
        currentTrackTitle = title
        nextPreloadedUrl = null
        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)

        scope.launch {
            if (!initMpv()) return@launch

            if (headers.isNotEmpty()) {
                val headerStr = headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
                setProperty("http-header-fields", headerStr)
            }

            setProperty("pause", if (isPlaying) "no" else "yes")
            executeCommand("loadfile", url, "replace")
            setVolume(currentVolume)
        }
    }

    override fun prepareNext(url: String?, headers: Map<String, String>) {
        nextPreloadedUrl = url
        if (url == null) {
            val count = getProperty("playlist-count")?.toIntOrNull() ?: 1
            if (count > 1) {
                executeCommand("playlist-remove", "1")
            }
            return
        }

        scope.launch {
            if (!initMpv()) return@launch
            if (headers.isNotEmpty()) {
                val headerStr = headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
                setProperty("http-header-fields", headerStr)
            }
            executeCommand("loadfile", url, "append")
        }
    }

    private var progressJob: Job? = null

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch(Dispatchers.IO) {
            while (isActive && isPlaying) {
                val timePosSec = getProperty("time-pos")?.toDoubleOrNull()
                val durSec = getProperty("duration")?.toDoubleOrNull()
                val cacheDurSec = getProperty("demuxer-cache-duration")?.toDoubleOrNull()
                val cacheTimeSec = getProperty("demuxer-cache-time")?.toDoubleOrNull()

                if (timePosSec != null) {
                    val ms = (timePosSec * 1000.0).toLong()
                    val durMs =
                        if (durSec != null && durSec > 0.0) (durSec * 1000.0).toLong() else _state.value.durationMs
                    val bufferedMs = when {
                        cacheDurSec != null && cacheDurSec > 0.0 -> ((timePosSec + cacheDurSec) * 1000.0).toLong()
                            .coerceAtLeast(ms)

                        cacheTimeSec != null && cacheTimeSec > 0.0 -> (cacheTimeSec * 1000.0).toLong().coerceAtLeast(ms)
                        durMs > 0L && _state.value.bufferedPositionMs >= durMs -> durMs
                        else -> maxOf(ms, _state.value.bufferedPositionMs)
                    }

                    _state.value = _state.value.copy(
                        positionMs = ms,
                        durationMs = if (durMs > 0L) durMs else _state.value.durationMs,
                        bufferedPositionMs = bufferedMs,
                        status = PlaybackStatus.PLAYING
                    )
                }
                delay(100)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun play() {
        isPlaying = true
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
        startProgressUpdates()
        scope.launch {
            if (initMpv()) {
                setProperty("pause", "no")
            }
        }
    }

    override fun pause() {
        isPlaying = false
        stopProgressUpdates()
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
        scope.launch {
            setProperty("pause", "yes")
        }
    }

    override fun stop() {
        isPlaying = false
        stopProgressUpdates()
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE, positionMs = 0L)
        scope.launch {
            executeCommand("stop")
        }
    }

    override fun seekTo(positionMs: Long) {
        val targetMs = positionMs.coerceAtLeast(0L)
        _state.value = _state.value.copy(positionMs = targetMs)
        val secStr = String.format(Locale.US, "%.3f", targetMs / 1000.0)
        scope.launch {
            executeCommand("seek", secStr, "absolute")
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        currentVolume = clamped
        val volPercent = (clamped * 100).toInt().toString()
        scope.launch {
            setProperty("volume", volPercent)
        }
    }

    override fun release() {
        if (isReleased.getAndSet(true)) return
        isPlaying = false
        stopProgressUpdates()
        eventLoopJob?.cancel()

        val ctx = mpvContext
        val lib = libMpvInstance
        if (ctx != null && lib != null) {
            try {
                lib.mpv_terminate_destroy(ctx)
            } catch (_: Throwable) {
            }
        }
        mpvContext = null
        libMpvInstance = null
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
    }
}

actual fun createAudioEngine(): AudioEngine = DesktopAudioEngine()
