package org.shilpo.peerless.player

import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/** Captures the default Windows render endpoint with WASAPI loopback. */
internal class WindowsSpectrumCapture(
    private val analyzer: AudioSpectrumAnalyzer,
    private val onUnavailable: (Throwable) -> Unit
) {
    private val running = AtomicBoolean(false)

    @Volatile
    private var captureThread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        captureThread = Thread(::captureLoop, "Peerless-WASAPI-spectrum").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        captureThread?.interrupt()
        captureThread = null
    }

    private fun captureLoop() {
        val ole32 = runCatching { Native.load("Ole32", Ole32::class.java) }.getOrElse { error ->
            running.set(false)
            onUnavailable(error)
            return
        }
        var comInitialized = false
        var enumerator: Pointer? = null
        var device: Pointer? = null
        var audioClient: Pointer? = null
        var captureClient: Pointer? = null
        var mixFormat: Pointer? = null
        var clientStarted = false

        try {
            val initializeResult = ole32.CoInitializeEx(null, COINIT_MULTITHREADED)
            checkHresult(initializeResult, "CoInitializeEx")
            comInitialized = true

            val enumeratorRef = PointerByReference()
            checkHresult(
                ole32.CoCreateInstance(
                    guidPointer(CLSID_MM_DEVICE_ENUMERATOR),
                    null,
                    CLSCTX_ALL,
                    guidPointer(IID_IMM_DEVICE_ENUMERATOR),
                    enumeratorRef
                ),
                "CoCreateInstance(IMMDeviceEnumerator)"
            )
            enumerator = enumeratorRef.value

            val deviceRef = PointerByReference()
            checkHresult(
                invoke(
                    enumerator,
                    4,
                    E_RENDER,
                    E_MULTIMEDIA,
                    deviceRef
                ),
                "IMMDeviceEnumerator.GetDefaultAudioEndpoint"
            )
            device = deviceRef.value

            val audioClientRef = PointerByReference()
            checkHresult(
                invoke(
                    device,
                    3,
                    guidPointer(IID_IAUDIO_CLIENT),
                    CLSCTX_ALL,
                    null,
                    audioClientRef
                ),
                "IMMDevice.Activate(IAudioClient)"
            )
            audioClient = audioClientRef.value

            val formatRef = PointerByReference()
            checkHresult(invoke(audioClient, 8, formatRef), "IAudioClient.GetMixFormat")
            mixFormat = formatRef.value
            val format = parseWaveFormat(mixFormat)

            checkHresult(
                invoke(
                    audioClient,
                    3,
                    AUDCLNT_SHAREMODE_SHARED,
                    AUDCLNT_STREAMFLAGS_LOOPBACK,
                    DEFAULT_BUFFER_DURATION_HNS,
                    0L,
                    mixFormat,
                    null
                ),
                "IAudioClient.Initialize"
            )
            ole32.CoTaskMemFree(mixFormat)
            mixFormat = null

            val captureClientRef = PointerByReference()
            checkHresult(
                invoke(
                    audioClient,
                    14,
                    guidPointer(IID_IAUDIO_CAPTURE_CLIENT),
                    captureClientRef
                ),
                "IAudioClient.GetService(IAudioCaptureClient)"
            )
            captureClient = captureClientRef.value

            checkHresult(invoke(audioClient, 10), "IAudioClient.Start")
            clientStarted = true
            readLoop(captureClient, format)
        } catch (error: Throwable) {
            if (running.get()) onUnavailable(error)
        } finally {
            running.set(false)
            mixFormat?.let { ole32.CoTaskMemFree(it) }
            if (clientStarted) audioClient?.let { runCatching { invoke(it, 11) } }
            captureClient?.let(::releaseComObject)
            audioClient?.let(::releaseComObject)
            device?.let(::releaseComObject)
            enumerator?.let(::releaseComObject)
            if (comInitialized) ole32.CoUninitialize()
        }
    }

    private fun readLoop(captureClient: Pointer, format: WaveFormat) {
        while (running.get()) {
            val packetFrames = IntByReference()
            checkHresult(
                invoke(captureClient, 5, packetFrames),
                "IAudioCaptureClient.GetNextPacketSize"
            )
            if (packetFrames.value <= 0) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    if (!running.get()) return
                }
                continue
            }

            val dataRef = PointerByReference()
            val framesRef = IntByReference()
            val flagsRef = IntByReference()
            val devicePositionRef = LongByReference()
            val qpcPositionRef = LongByReference()
            checkHresult(
                invoke(
                    captureClient,
                    3,
                    dataRef,
                    framesRef,
                    flagsRef,
                    devicePositionRef,
                    qpcPositionRef
                ),
                "IAudioCaptureClient.GetBuffer"
            )

            val frames = framesRef.value.coerceAtLeast(0)
            try {
                if (frames > 0) {
                    val samples = if (flagsRef.value and AUDCLNT_BUFFERFLAGS_SILENT != 0) {
                        FloatArray(frames * format.channels)
                    } else {
                        decodeSamples(dataRef.value, frames, format)
                    }
                    analyzer.acceptInterleavedPcm(samples, format.channels, format.sampleRate)
                }
            } finally {
                checkHresult(invoke(captureClient, 4, frames), "IAudioCaptureClient.ReleaseBuffer")
            }
        }
    }

    private fun decodeSamples(data: Pointer?, frames: Int, format: WaveFormat): FloatArray {
        if (data == null) return FloatArray(frames * format.channels)
        val byteCount = frames * format.blockAlign
        val bytes = data.getByteArray(0, byteCount)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(frames * format.channels)
        for (index in samples.indices) {
            samples[index] = when {
                format.isFloat && format.bitsPerSample == 32 -> buffer.float.coerceIn(-1f, 1f)
                format.bitsPerSample == 8 -> ((buffer.get().toInt() and 0xff) - 128) / 128f
                format.bitsPerSample == 16 -> buffer.short / 32768f
                format.bitsPerSample == 24 -> {
                    val first = buffer.get().toInt() and 0xff
                    val second = buffer.get().toInt() and 0xff
                    val third = buffer.get().toInt() and 0xff
                    val value = first or (second shl 8) or (third shl 16)
                    val signedValue = if (value and 0x800000 != 0) value or -0x1000000 else value
                    signedValue / 8_388_608f
                }

                format.bitsPerSample == 32 -> buffer.int / 2_147_483_648f
                else -> 0f
            }
        }
        return samples
    }

    private fun parseWaveFormat(pointer: Pointer): WaveFormat {
        val tag = pointer.getShort(0).toInt() and 0xffff
        val channels = pointer.getShort(2).toInt() and 0xffff
        val sampleRate = pointer.getInt(4)
        val blockAlign = pointer.getShort(12).toInt() and 0xffff
        val bitsPerSample = pointer.getShort(14).toInt() and 0xffff
        val isExtensible = tag == WAVE_FORMAT_EXTENSIBLE
        val subFormatTag = if (isExtensible) pointer.getInt(24) else tag
        val isFloat = subFormatTag == WAVE_FORMAT_IEEE_FLOAT
        require(channels > 0 && sampleRate > 0 && blockAlign > 0) { "Invalid WASAPI mix format" }
        require(isFloat || subFormatTag == WAVE_FORMAT_PCM) { "Unsupported WASAPI mix format tag $subFormatTag" }
        require(
            bitsPerSample in setOf(
                8,
                16,
                24,
                32
            )
        ) { "Unsupported WASAPI mix format depth $bitsPerSample" }
        return WaveFormat(channels, sampleRate, blockAlign, bitsPerSample, isFloat)
    }

    private fun guidPointer(value: String): Pointer =
        WasapiGuid.fromString(value).apply { write() }.pointer

    private fun invoke(pointer: Pointer, vtableIndex: Int, vararg arguments: Any?): Int {
        val vtable = pointer.getPointer(0) ?: error("COM interface has no vtable")
        val method = vtable.getPointer(vtableIndex.toLong() * Native.POINTER_SIZE)
            ?: error("COM interface method $vtableIndex is null")
        return Function.getFunction(method, Function.ALT_CONVENTION)
            .invokeInt(arrayOf(pointer, *arguments))
    }

    private fun releaseComObject(pointer: Pointer) {
        runCatching { invoke(pointer, 2) }
    }

    private fun checkHresult(result: Int, operation: String) {
        check(result >= 0) { "$operation failed with HRESULT 0x${result.toUInt().toString(16)}" }
    }

    private data class WaveFormat(
        val channels: Int,
        val sampleRate: Int,
        val blockAlign: Int,
        val bitsPerSample: Int,
        val isFloat: Boolean
    )

    @Suppress("FunctionName")
    private interface Ole32 : StdCallLibrary {
        fun CoInitializeEx(reserved: Pointer?, coInit: Int): Int
        fun CoCreateInstance(
            classId: Pointer,
            outer: Pointer?,
            context: Int,
            interfaceId: Pointer,
            output: PointerByReference
        ): Int

        fun CoTaskMemFree(pointer: Pointer?)
        fun CoUninitialize()
    }

    @Structure.FieldOrder("data1", "data2", "data3", "data4")
    private class WasapiGuid(
        @JvmField var data1: Int = 0,
        @JvmField var data2: Short = 0,
        @JvmField var data3: Short = 0,
        @JvmField var data4: ByteArray = ByteArray(8)
    ) : Structure() {
        companion object {
            fun fromString(value: String): WasapiGuid {
                val parts = value.removePrefix("{").removeSuffix("}").split('-')
                val tail =
                    (parts[3] + parts[4]).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                return WasapiGuid(
                    data1 = parts[0].toLong(16).toInt(),
                    data2 = parts[1].toInt(16).toShort(),
                    data3 = parts[2].toInt(16).toShort(),
                    data4 = tail
                )
            }
        }
    }

    private companion object {
        const val COINIT_MULTITHREADED = 0
        const val CLSCTX_ALL = 0x17
        const val E_RENDER = 0
        const val E_MULTIMEDIA = 1
        const val AUDCLNT_SHAREMODE_SHARED = 0
        const val AUDCLNT_STREAMFLAGS_LOOPBACK = 0x00020000
        const val AUDCLNT_BUFFERFLAGS_SILENT = 0x00000002
        const val DEFAULT_BUFFER_DURATION_HNS = 1_000_000L
        const val POLL_INTERVAL_MS = 5L
        const val WAVE_FORMAT_PCM = 1
        const val WAVE_FORMAT_IEEE_FLOAT = 3
        const val WAVE_FORMAT_EXTENSIBLE = 0xfffe

        const val CLSID_MM_DEVICE_ENUMERATOR = "BCDE0395-E52F-467C-8E3D-C4579291692E"
        const val IID_IMM_DEVICE_ENUMERATOR = "A95664D2-9614-4F35-A746-DE8DB63617E6"
        const val IID_IAUDIO_CLIENT = "1CB9AD4C-DBFA-4c32-B178-C2F568A703B2"
        const val IID_IAUDIO_CAPTURE_CLIENT = "C8ADBD64-E71E-48a0-A4DE-185C395CD317"
    }
}
