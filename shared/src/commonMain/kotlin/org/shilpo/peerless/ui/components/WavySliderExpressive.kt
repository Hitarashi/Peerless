/*
 * Peerless (2026)
 * Expressive Wavy Slider & Progress Indicator based on Material3 Expressive & ArchiveTune
 */

package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.times
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Expressive Wavy Slider modeled directly after ArchiveTune's playback scrubber.
 *
 * Features:
 * - Official Material 3 Expressive [LinearWavyProgressIndicator] with dynamic wave animation.
 * - Morphing thumb indicator: circular resting dot that fluidly elongates into a vertical scrub pill line on interaction.
 * - Dynamic gap around the thumb that expands during interaction.
 * - Frame-interpolated smooth progress using [rememberSmoothProgress].
 * - Instant scrubbing with haptic feedback and tap-to-seek support.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WavySliderExpressive(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    onValueCommit: ((Float) -> Unit)? = null,
    bufferedValue: (() -> Float)? = null,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    bufferedTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
    thumbColor: Color = MaterialTheme.colorScheme.primary,

    isPlaying: Boolean = true,
    isVisible: Boolean = true,
    strokeWidth: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    idleGap: Dp = 3.5.dp,
    trackEdgePadding: Dp = thumbRadius,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength / 2f,

    waveAmplitudeWhenPlaying: Dp = 4.dp,
    thumbLineHeightWhenInteracting: Dp = 20.dp,
    semanticsLabel: String? = null,
    semanticsProgressStep: Float = 0.01f
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val trackEdgePaddingPx = with(density) { trackEdgePadding.coerceAtLeast(0.dp).toPx() }
    val thumbLineHeightPx = with(density) { thumbLineHeightWhenInteracting.toPx() }

    val stroke = remember(strokeWidthPx) {
        Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    }

    val normalizedValueState = remember(valueRange) {
        derivedStateOf {
            val v = value()
            if (valueRange.endInclusive == valueRange.start) 0f
            else ((v - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        }
    }

    val safeSemanticsStep = semanticsProgressStep.coerceIn(0.005f, 0.25f)
    val semanticNormalizedValueState = remember(safeSemanticsStep) {
        derivedStateOf {
            val norm = normalizedValueState.value
            ((norm / safeSemanticsStep).roundToInt() * safeSemanticsStep).coerceIn(0f, 1f)
        }
    }
    val semanticSliderValueState = remember(valueRange) {
        derivedStateOf {
            valueRange.start + semanticNormalizedValueState.value * (valueRange.endInclusive - valueRange.start)
        }
    }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val latestOnValueCommit by rememberUpdatedState(onValueCommit)
    var isPointerSeeking by remember { mutableStateOf(false) }
    val isInteracting = isPointerSeeking

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "ThumbInteractionAnim"
    )
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (enabled && isPlaying && !isInteracting) 1f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "amplitude"
    )

    val currentHalfWidth = remember(thumbRadius, strokeWidth) {
        derivedStateOf {
            val fraction = thumbInteractionFraction
            val radius = thumbRadius
            val halfStroke = strokeWidth * 0.6f
            radius * (1f - fraction) + halfStroke * fraction
        }
    }

    val dynamicGapSize = remember(idleGap, currentHalfWidth) {
        derivedStateOf {
            currentHalfWidth.value + idleGap
        }
    }

    val normalizedBufferedState = remember(valueRange, bufferedValue) {
        derivedStateOf {
            val b = bufferedValue?.invoke() ?: 0f
            if (valueRange.endInclusive == valueRange.start) 0f
            else ((b - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        }
    }

    val animatedBufferedProgress by animateFloatAsState(
        targetValue = normalizedBufferedState.value,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "BufferedProgressAnim"
    )

    val renderedNormalizedProgress = remember {
        val initialVal = value()
        val initialNorm = if (valueRange.endInclusive == valueRange.start) 0f
        else ((initialVal - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        mutableFloatStateOf(initialNorm)
    }
    var lastProgressUpdateNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isInteracting, enabled) {
        snapshotFlow { normalizedValueState.value }.collect { target ->
            if (!enabled || isInteracting) {
                renderedNormalizedProgress.floatValue = target
                lastProgressUpdateNanos = withFrameNanos { it }
                return@collect
            }

            val start = renderedNormalizedProgress.floatValue
            if (abs(start - target) > 0.1f) {
                renderedNormalizedProgress.floatValue = target
                lastProgressUpdateNanos = withFrameNanos { it }
                return@collect
            }

            val nowNanos = withFrameNanos { it }
            val intervalMs = if (lastProgressUpdateNanos == 0L) {
                180L
            } else {
                ((nowNanos - lastProgressUpdateNanos) / 1_000_000L).coerceIn(1L, 250L)
            }
            lastProgressUpdateNanos = nowNanos

            if (abs(start - target) <= 0.0001f) {
                renderedNormalizedProgress.floatValue = target
                return@collect
            }

            val durationNanos = (intervalMs * 900_000L).coerceAtLeast(1_000_000L)
            var startFrameNanos = 0L
            while (isActive) {
                val frameNanos = withFrameNanos { it }
                if (startFrameNanos == 0L) startFrameNanos = frameNanos
                val elapsedNanos = (frameNanos - startFrameNanos).coerceAtLeast(0L)
                val fraction = (elapsedNanos.toDouble() / durationNanos.toDouble()).toFloat().coerceIn(0f, 1f)
                renderedNormalizedProgress.floatValue = start + (target - start) * fraction
                if (fraction >= 1f) break
            }
            renderedNormalizedProgress.floatValue = target
        }
    }

    val containerHeight =
        max(WavyProgressIndicatorDefaults.LinearContainerHeight, max(thumbRadius * 2, thumbLineHeightWhenInteracting))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .clearAndSetSemantics {
                if (!semanticsLabel.isNullOrBlank()) {
                    contentDescription = semanticsLabel
                }
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = semanticSliderValueState.value,
                    range = valueRange.start..valueRange.endInclusive,
                    steps = 0
                )
                if (enabled) {
                    setProgress { requested ->
                        val coerced = requested.coerceIn(valueRange.start, valueRange.endInclusive)
                        latestOnValueChange(coerced)
                        latestOnValueCommit?.invoke(coerced)
                            ?: latestOnValueChangeFinished?.invoke()
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isVisible) {
            LinearWavyProgressIndicator(
                progress = { renderedNormalizedProgress.floatValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = trackEdgePadding.coerceAtLeast(0.dp))
                    .clearAndSetSemantics { },
                color = activeTrackColor,
                trackColor = inactiveTrackColor,
                stroke = stroke,
                trackStroke = stroke,
                gapSize = 2f * dynamicGapSize.value,
                stopSize = 0.dp,
                amplitude = { progress -> if (progress > 0f) animatedAmplitude else 0f },
                wavelength = wavelength,
                waveSpeed = waveSpeed
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth().height(containerHeight))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isVisible) return@Canvas
            val edgePaddingPx = trackEdgePaddingPx.coerceIn(0f, size.width / 2f)
            val trackStart = edgePaddingPx
            val trackEnd = size.width - edgePaddingPx
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)
            val thumbY = size.height / 2
            val renderedProgress = renderedNormalizedProgress.floatValue

            fun lerp(start: Float, stop: Float, fraction: Float): Float {
                return start + (stop - start) * fraction
            }

            val currentWidth = lerp(thumbRadiusPx * 2f, strokeWidthPx * 1.2f, thumbInteractionFraction)
            val currentHeight = lerp(thumbRadiusPx * 2f, thumbLineHeightPx, thumbInteractionFraction)
            val rawThumbX = trackStart + (trackWidth * renderedProgress)
            val minThumbCenter = (currentWidth / 2f).coerceAtMost(size.width / 2f)
            val maxThumbCenter = (size.width - currentWidth / 2f).coerceAtLeast(minThumbCenter)
            val thumbX = rawThumbX.coerceIn(minThumbCenter, maxThumbCenter)

            // BUFFERED TRACK (YouTube / mpv style)
            val bufferedProgress = animatedBufferedProgress.coerceIn(0f, 1f)
            if (bufferedProgress > renderedProgress && trackWidth > 0f) {
                val gapHalfPx = with(density) { dynamicGapSize.value.toPx() }
                val bufferStartX = (thumbX + gapHalfPx).coerceIn(trackStart, trackEnd)
                val bufferEndX = (trackStart + trackWidth * bufferedProgress).coerceIn(trackStart, trackEnd)

                if (bufferEndX > bufferStartX) {
                    drawLine(
                        color = bufferedTrackColor,
                        start = Offset(bufferStartX, thumbY),
                        end = Offset(bufferEndX, thumbY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            // THUMB INDICATOR
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(
                    thumbX - currentWidth / 2f,
                    thumbY - currentHeight / 2f
                ),
                size = Size(currentWidth, currentHeight),
                cornerRadius = CornerRadius(currentWidth / 2f)
            )
        }

        val haptics = LocalHapticFeedback.current
        val currentHaptics = rememberUpdatedState(haptics)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, valueRange, trackEdgePaddingPx) {
                    if (!enabled) return@pointerInput

                    fun valueForX(rawX: Float): Float {
                        val edgePadding = trackEdgePaddingPx.coerceIn(0f, size.width / 2f)
                        val trackStart = edgePadding
                        val trackEnd = size.width - edgePadding
                        val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                        val normalized = ((rawX - trackStart) / trackWidth).coerceIn(0f, 1f)
                        return valueRange.start +
                                normalized * (valueRange.endInclusive - valueRange.start)
                    }

                    val lastHapticStep = intArrayOf(-1)

                    awaitEachGesture {
                        try {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPointerSeeking = true
                            down.consume()
                            var latestGestureValue = valueForX(down.position.x)
                            latestOnValueChange(latestGestureValue)

                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break

                                pointerId = change.id
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }

                                if (change.position != change.previousPosition) {
                                    change.consume()
                                    latestGestureValue = valueForX(change.position.x)
                                    val quantized = (latestGestureValue.coerceIn(0f, 1f) * 20f).toInt()
                                    if (quantized != lastHapticStep[0]) {
                                        lastHapticStep[0] = quantized
                                        try {
                                            currentHaptics.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Throwable) {
                                        }
                                    }
                                    latestOnValueChange(latestGestureValue)
                                }
                            }

                            latestOnValueCommit?.invoke(latestGestureValue)
                                ?: latestOnValueChangeFinished?.invoke()
                        } finally {
                            isPointerSeeking = false
                        }
                    }
                }
        )
    }
}

/**
 * Samples playback position and returns smoothly interpolated progress fraction and position.
 * Directly sourced from ArchiveTune.
 */
@Composable
fun rememberSmoothProgress(
    isPlayingProvider: () -> Boolean,
    currentPositionProvider: () -> Long,
    totalDuration: Long,
    sampleWhilePlayingMs: Long = 100L,
    sampleWhilePausedMs: Long = 800L,
    isVisible: Boolean = true
): Pair<State<Float>, State<Long>> {
    var sampledPosition by remember { mutableLongStateOf(0L) }
    var sampledFraction by remember { mutableFloatStateOf(0f) }

    val latestPositionProvider by rememberUpdatedState(newValue = currentPositionProvider)
    val latestIsPlayingProvider by rememberUpdatedState(newValue = isPlayingProvider)
    val latestSampleWhilePlayingMs by rememberUpdatedState(sampleWhilePlayingMs)
    val latestSampleWhilePausedMs by rememberUpdatedState(sampleWhilePausedMs)
    val latestIsVisible by rememberUpdatedState(isVisible)

    LaunchedEffect(totalDuration) {
        fun sampleNow() {
            val rawPosition = latestPositionProvider()
            val safeUpperBound = totalDuration.coerceAtLeast(0L)
            val safeDuration = totalDuration.coerceAtLeast(1L).toFloat()
            val clampedPosition = rawPosition.coerceIn(0L, safeUpperBound)
            sampledPosition = clampedPosition
            sampledFraction = (clampedPosition / safeDuration).coerceIn(0f, 1f)
        }

        sampleNow()

        while (isActive) {
            val isVisible = latestIsVisible
            val isPlaying = latestIsPlayingProvider()

            if (!isVisible || !isPlaying) {
                val initialPos = latestPositionProvider()
                snapshotFlow {
                    latestIsVisible && (latestIsPlayingProvider() || latestPositionProvider() != initialPos)
                }.first { it }

                sampleNow()
                if (!latestIsVisible || !latestIsPlayingProvider()) {
                    continue
                }
            }

            val delayMillis = latestSampleWhilePlayingMs
            kotlinx.coroutines.delay(delayMillis.coerceAtLeast(1L))
            sampleNow()
        }
    }

    val fractionState = remember {
        derivedStateOf { sampledFraction }
    }

    val displayedPositionState = remember(totalDuration) {
        derivedStateOf {
            sampledPosition.coerceIn(0L, totalDuration.coerceAtLeast(0L))
        }
    }

    return fractionState to displayedPositionState
}
