package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
fun NavToggleMorphIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isExpanded) "Collapse rail" else "Expand rail",
    flipHorizontal: Boolean = false,
) {
    val progress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "NavToggleMorphProgress"
    )

    val layoutDirection = LocalLayoutDirection.current

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = descModifier.size(size)) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val shouldFlip = if (flipHorizontal) !isRtl else isRtl
        if (shouldFlip) {
            scale(scaleX = -1f, scaleY = 1f, pivot = center) {
                drawNavToggleMorph(progress = progress, tint = tint)
            }
        } else {
            drawNavToggleMorph(progress = progress, tint = tint)
        }
    }
}

private fun DrawScope.drawNavToggleMorph(
    progress: Float,
    tint: Color,
) {
    val p = progress.coerceIn(0f, 1f)

    val elasticScale = 1f - 0.05f * sin(p * PI.toFloat())

    val baseSize = min(size.width, size.height)
    val s = (baseSize / 24f) * elasticScale
    val cx = size.width / 2f
    val cy = size.height / 2f

    fun toCanvas(x: Float, y: Float): Offset {
        return Offset(
            cx + (x - 12f) * s,
            cy + (y - 12f) * s
        )
    }

    val strokeWidth = 2.0f * s

    val leftBarStartX = 4f

    val topBarEndX = lerp(20f, 11.5f, p)
    drawLine(
        color = tint,
        start = toCanvas(leftBarStartX, 7f),
        end = toCanvas(topBarEndX, 7f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val middleBarEndX = lerp(20f, 9.5f, p)
    drawLine(
        color = tint,
        start = toCanvas(leftBarStartX, 12f),
        end = toCanvas(middleBarEndX, 12f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val bottomBarEndX = lerp(20f, 11.5f, p)
    drawLine(
        color = tint,
        start = toCanvas(leftBarStartX, 17f),
        end = toCanvas(bottomBarEndX, 17f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    if (p > 0.005f) {
        val wingAlpha = (p * 1.5f).coerceIn(0f, 1f)
        val chevronColor = tint.copy(alpha = tint.alpha * wingAlpha)

        val chevronTip = Offset(lerp(20f, 14.5f, p), 12f)
        val topWingEnd = Offset(lerp(20f, 19.5f, p), lerp(12f, 7f, p))
        val bottomWingEnd = Offset(lerp(20f, 19.5f, p), lerp(12f, 17f, p))

        drawLine(
            color = chevronColor,
            start = toCanvas(chevronTip.x, chevronTip.y),
            end = toCanvas(topWingEnd.x, topWingEnd.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = chevronColor,
            start = toCanvas(chevronTip.x, chevronTip.y),
            end = toCanvas(bottomWingEnd.x, bottomWingEnd.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}
