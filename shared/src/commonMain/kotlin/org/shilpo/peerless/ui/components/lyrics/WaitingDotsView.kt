package org.shilpo.peerless.ui.components.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private val YosEasing = CubicBezierEasing(0.75f, 0f, 0.25f, 1f)

/**
 * Animated 3-dot musical waiting pulse widget ported from XMusic's XWaitingDotsView.
 * Rhythmically pulses and progressively fills to anticipate vocal entry during instrumental breaks.
 */
@Composable
fun WaitingDotsView(
    startTime: Long,
    endTime: Long,
    currentProgressMs: Long,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val inactiveColor = primaryColor.copy(alpha = 0.30f)

    Canvas(
        modifier = modifier
            .width(80.dp)
            .height(40.dp)
    ) {
        val density = this.density
        val dotRadius = 5.5f * density
        val spacing = 19f * density
        val startX = dotRadius + 8f * density
        val centerY = size.height / 2f

        val duration = (endTime - startTime).coerceAtLeast(1L)
        val timeInto = currentProgressMs - startTime
        val timeLeft = endTime - currentProgressMs

        var exitDuration = 250L
        var readyDuration = 400L
        var fillDuration = duration - exitDuration - readyDuration

        if (fillDuration < 500L) {
            exitDuration = (duration / 4).coerceAtLeast(1L)
            readyDuration = (duration / 4).coerceAtLeast(1L)
            fillDuration = duration - exitDuration - readyDuration
        }

        val fillProgress = if (fillDuration > 0) {
            (timeInto.toFloat() / fillDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        var actualScale = 1.0f
        var globalAlphaMult = 1.0f

        when {
            timeLeft <= exitDuration -> {
                val exitP = 1.0f - (timeLeft.coerceAtLeast(0L).toFloat() / exitDuration.toFloat())
                actualScale = 1.4f - (1.4f * exitP)
                globalAlphaMult = (1.0f - exitP).coerceIn(0f, 1f)
            }

            timeLeft <= exitDuration + readyDuration -> {
                val readyP = 1.0f - ((timeLeft - exitDuration).toFloat() / readyDuration.toFloat())
                actualScale = 0.85f + (0.55f * YosEasing.transform(readyP.coerceIn(0f, 1f)))
            }

            else -> {
                val targetCycle = 3000f
                val cycles = (fillDuration.toFloat() / targetCycle).roundToInt().coerceAtLeast(1)
                val actualCycleTime = fillDuration.toFloat() / cycles.toFloat()
                val cycleProgress = if (actualCycleTime > 0) {
                    ((timeInto.toFloat() % actualCycleTime) / actualCycleTime).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val rawWave = -cos(cycleProgress * PI.toFloat() * 2f)
                actualScale = 1.0f + (0.15f * rawWave)
            }
        }

        scale(
            scaleX = actualScale,
            scaleY = actualScale,
            pivot = Offset(startX, centerY)
        ) {
            for (i in 1..3) {
                val average = 1f / 3f
                val beforePadding = (i - 1) * average
                val segmentProgress = ((fillProgress - beforePadding) / average).coerceIn(0f, 1f)

                val dotColor = lerp(inactiveColor, primaryColor, segmentProgress)
                val finalAlpha = (dotColor.alpha * globalAlphaMult).coerceIn(0f, 1f)

                drawCircle(
                    color = dotColor.copy(alpha = finalAlpha),
                    radius = dotRadius,
                    center = Offset(startX + (i - 1) * spacing, centerY)
                )
            }
        }
    }
}
