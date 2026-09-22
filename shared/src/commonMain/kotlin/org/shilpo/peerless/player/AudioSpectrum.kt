package org.shilpo.peerless.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioSpectrumFrame(
    val bands: List<Float>,
    val bassEnergy: Float,
    val sequence: Long
)

internal val EmptyAudioSpectrum: StateFlow<AudioSpectrumFrame?> =
    MutableStateFlow<AudioSpectrumFrame?>(null).asStateFlow()

class AudioSpectrumAnalyzer(
    private val bandCount: Int = 48,
    private val fftSize: Int = 2048,
    private val hopSize: Int = 512
) {
    private val _frame = MutableStateFlow<AudioSpectrumFrame?>(null)
    val frame: StateFlow<AudioSpectrumFrame?> = _frame.asStateFlow()
    private val resetGeneration = MutableStateFlow(0L)

    private val ring = FloatArray(fftSize)
    private val real = FloatArray(fftSize)
    private val imaginary = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
    }
    private val smoothedBands = FloatArray(bandCount)
    private var writeIndex = 0
    private var samplesReceived = 0L
    private var samplesSinceFrame = 0
    private var sampleRateHz = 0
    private var sequence = 0L
    private var appliedResetGeneration = 0L

    init {
        require(fftSize > 0 && fftSize and (fftSize - 1) == 0) { "fftSize must be a power of two" }
        require(bandCount > 0) { "bandCount must be positive" }
        require(hopSize > 0 && hopSize <= fftSize) { "hopSize must be in 1..fftSize" }
    }

    fun acceptInterleavedPcm(
        samples: FloatArray,
        channels: Int,
        sampleRate: Int,
        sampleCount: Int = samples.size
    ) {
        if (channels <= 0 || sampleRate <= 0 || sampleCount <= 0) return
        applyRequestedReset()
        if (sampleRateHz != sampleRate) {
            clearAnalysisState()
            sampleRateHz = sampleRate
            _frame.value = null
        }

        val frameCount = sampleCount.coerceAtMost(samples.size) / channels
        for (frameIndex in 0 until frameCount) {
            var mono = 0f
            val base = frameIndex * channels
            for (channel in 0 until channels) {
                mono += samples[base + channel].coerceIn(-1f, 1f)
            }
            ring[writeIndex] = mono / channels
            writeIndex = (writeIndex + 1) % fftSize
            samplesReceived += 1
            samplesSinceFrame += 1

            if (samplesReceived >= fftSize && samplesSinceFrame >= hopSize) {
                analyzeWindow()
                samplesSinceFrame = 0
            }
        }
    }

    fun reset() {
        resetGeneration.update { it + 1L }
        _frame.value = null
    }

    private fun applyRequestedReset() {
        val requestedGeneration = resetGeneration.value
        if (requestedGeneration == appliedResetGeneration) return
        clearAnalysisState()
        appliedResetGeneration = requestedGeneration
    }

    private fun clearAnalysisState() {
        ring.fill(0f)
        smoothedBands.fill(0f)
        writeIndex = 0
        samplesReceived = 0L
        samplesSinceFrame = 0
        sampleRateHz = 0
        sequence = 0L
    }

    private fun analyzeWindow() {
        val frameGeneration = appliedResetGeneration
        for (index in 0 until fftSize) {
            val sampleIndex = (writeIndex + index) % fftSize
            real[index] = ring[sampleIndex] * window[index]
            imaginary[index] = 0f
        }
        fft()

        val nyquist = sampleRateHz / 2f
        val lowestFrequency = 30f
        val highestFrequency = min(16_000f, nyquist).coerceAtLeast(lowestFrequency + 1f)
        val frequencyRatio = highestFrequency / lowestFrequency
        val binCount = fftSize / 2
        for (band in 0 until bandCount) {
            val lowHz = lowestFrequency * frequencyRatio.toDouble().pow(band.toDouble() / bandCount)
                .toFloat()
            val highHz =
                lowestFrequency * frequencyRatio.toDouble().pow((band + 1).toDouble() / bandCount)
                    .toFloat()
            val firstBin = max(1, (lowHz * fftSize / sampleRateHz).toInt())
            val lastBin =
                min(binCount - 1, max(firstBin, (highHz * fftSize / sampleRateHz).toInt()))
            var peak = 0f
            for (bin in firstBin..lastBin) {
                val magnitude = sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin])
                peak = max(peak, magnitude * (4f / fftSize))
            }

            val decibels = 20f * log10(peak.coerceAtLeast(0.000001f))
            val target = ((decibels + 54f) / 54f).coerceIn(0f, 1f)
            val current = smoothedBands[band]
            smoothedBands[band] = if (target > current) {
                current * 0.52f + target * 0.48f
            } else {
                current * 0.88f + target * 0.12f
            }
        }

        val bassBandCount = min(6, bandCount)
        var bass = 0f
        for (index in 0 until bassBandCount) bass += smoothedBands[index]
        if (resetGeneration.value != frameGeneration) return
        sequence += 1
        _frame.value = AudioSpectrumFrame(
            bands = smoothedBands.toList(),
            bassEnergy = bass / bassBandCount,
            sequence = sequence
        )
    }

    private fun fft() {
        var reversed = 0
        for (index in 1 until fftSize) {
            var bit = fftSize shr 1
            while ((reversed and bit) != 0) {
                reversed = reversed xor bit
                bit = bit shr 1
            }
            reversed = reversed xor bit
            if (index < reversed) {
                val realValue = real[index]
                real[index] = real[reversed]
                real[reversed] = realValue
                val imaginaryValue = imaginary[index]
                imaginary[index] = imaginary[reversed]
                imaginary[reversed] = imaginaryValue
            }
        }

        var blockSize = 2
        while (blockSize <= fftSize) {
            val angle = -2.0 * PI / blockSize
            val stepReal = cos(angle).toFloat()
            val stepImaginary = sin(angle).toFloat()
            val halfBlock = blockSize / 2
            var blockStart = 0
            while (blockStart < fftSize) {
                var twiddleReal = 1f
                var twiddleImaginary = 0f
                for (offset in 0 until halfBlock) {
                    val evenIndex = blockStart + offset
                    val oddIndex = evenIndex + halfBlock
                    val oddReal =
                        real[oddIndex] * twiddleReal - imaginary[oddIndex] * twiddleImaginary
                    val oddImaginary =
                        real[oddIndex] * twiddleImaginary + imaginary[oddIndex] * twiddleReal
                    real[oddIndex] = real[evenIndex] - oddReal
                    imaginary[oddIndex] = imaginary[evenIndex] - oddImaginary
                    real[evenIndex] += oddReal
                    imaginary[evenIndex] += oddImaginary

                    val nextTwiddleReal = twiddleReal * stepReal - twiddleImaginary * stepImaginary
                    twiddleImaginary = twiddleReal * stepImaginary + twiddleImaginary * stepReal
                    twiddleReal = nextTwiddleReal
                }
                blockStart += blockSize
            }
            blockSize = blockSize shl 1
        }
    }
}
