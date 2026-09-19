package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * An expressive, physics-based vector morph icon supporting continuous volume morphing
 * and animated mute states.
 *
 * Modeled using exact Google Material Symbols path geometry (960x960 viewBox):
 * - Cone glides smoothly along X (460 -> 380 -> 300) as volume increases.
 * - Inner wave (Wave 1) blooms smoothly as volume moves from 0 to 0.5, then shifts at higher volumes.
 * - Outer wave (Wave 2) blooms smoothly as volume moves from 0.5 to 1.0.
 * - When muted, animates a diagonal slash across the cone with spring bounce and collapses waves.
 * - Zero allocations on draw frames via remembered geometry.
 */
@Composable
fun VolumeMorphIcon(
    volume: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    cutoutColor: Color? = null,
    contentDescription: String? = if (isMuted || volume <= 0.001f) "Muted" else "Volume ${(volume * 100).roundToInt()}%",
) {
    val targetVolume = if (isMuted) 0f else volume.coerceIn(0f, 1f)

    val animVolume by animateFloatAsState(
        targetValue = targetVolume,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VolumeMorphVolume"
    )

    val muteProgress by animateFloatAsState(
        targetValue = if (isMuted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VolumeMorphMute"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    // Pre-allocated paths for zero allocation on draw frames
    val conePath = remember {
        Path().apply {
            moveTo(440f, -360f)
            lineTo(320f, -360f)
            quadraticTo(303f, -360f, 291.5f, -371.5f)
            quadraticTo(280f, -383f, 280f, -400f)
            lineTo(280f, -560f)
            quadraticTo(280f, -577f, 291.5f, -588.5f)
            quadraticTo(303f, -600f, 320f, -600f)
            lineTo(440f, -600f)
            lineTo(572f, -732f)
            quadraticTo(591f, -751f, 615.5f, -740.5f)
            quadraticTo(640f, -730f, 640f, -703f)
            lineTo(640f, -257f)
            quadraticTo(640f, -230f, 615.5f, -219.5f)
            quadraticTo(591f, -209f, 572f, -228f)
            close()
        }
    }

    val wave1Path = remember {
        Path().apply {
            moveTo(740f, -480f)
            quadraticTo(740f, -438f, 721f, -400.5f)
            quadraticTo(702f, -363f, 671f, -339f)
            quadraticTo(661f, -333f, 650.5f, -338.5f)
            quadraticTo(640f, -344f, 640f, -356f)
            lineTo(640f, -606f)
            quadraticTo(640f, -618f, 650.5f, -623.5f)
            quadraticTo(661f, -629f, 671f, -623f)
            quadraticTo(702f, -598f, 721f, -560f)
            quadraticTo(740f, -522f, 740f, -480f)
            close()
        }
    }

    val wave2Path = remember {
        Path().apply {
            moveTo(760f, -481f)
            quadraticTo(760f, -564f, 716f, -632.5f)
            quadraticTo(672f, -701f, 598f, -735f)
            quadraticTo(583f, -742f, 576f, -756.5f)
            quadraticTo(569f, -771f, 574f, -786f)
            quadraticTo(580f, -802f, 595.5f, -809f)
            quadraticTo(611f, -816f, 627f, -809f)
            quadraticTo(724f, -766f, 782f, -677.5f)
            quadraticTo(840f, -589f, 840f, -481f)
            quadraticTo(840f, -373f, 782f, -284.5f)
            quadraticTo(724f, -196f, 627f, -153f)
            quadraticTo(611f, -146f, 595.5f, -153f)
            quadraticTo(580f, -160f, 574f, -176f)
            quadraticTo(569f, -191f, 576f, -205.5f)
            quadraticTo(583f, -220f, 598f, -227f)
            quadraticTo(672f, -261f, 716f, -329.5f)
            quadraticTo(760f, -398f, 760f, -481f)
            close()
        }
    }

    Canvas(modifier = descModifier.size(size)) {
        drawVolumeMorph(
            volume = animVolume,
            muteProgress = muteProgress,
            tint = tint,
            cutoutColor = cutoutColor,
            conePath = conePath,
            wave1Path = wave1Path,
            wave2Path = wave2Path,
        )
    }
}

private fun DrawScope.drawVolumeMorph(
    volume: Float,
    muteProgress: Float,
    tint: Color,
    cutoutColor: Color?,
    conePath: Path,
    wave1Path: Path,
    wave2Path: Path,
) {
    val v = volume.coerceIn(0f, 1f)
    val mp = muteProgress.coerceIn(0f, 1f)

    // Squash and stretch during mute toggle transition
    val elasticScale = 1f - 0.04f * sin(mp * PI.toFloat())

    val baseSize = min(this.size.width, this.size.height)
    val s = (baseSize / 960f) * elasticScale
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f

    // 1. Cone glide:
    // In SVG 1 (v=0): cone center = 460 (dx = 0)
    // In SVG 2 (v=0.5): cone center = 380 (dx = -80)
    // In SVG 3 (v=1.0): cone center = 300 (dx = -160)
    val coneDx = -160f * v

    // 2. Wave 1 bloom and shift:
    // In 0..0.5: blooms from scale 0.3 to 1.0, alpha 0 to 1
    // In 0.5..1.0: stays full scale/alpha, shifts from x=640 to x=560 (delta -80)
    val w1Scale: Float
    val w1Alpha: Float
    val w1Dx: Float

    if (v <= 0.5f) {
        val p = (v / 0.5f).coerceIn(0f, 1f)
        w1Scale = lerp(0.3f, 1.0f, p)
        w1Alpha = (p * 1.5f).coerceIn(0f, 1f) * (1f - mp)
        w1Dx = 0f
    } else {
        val p = ((v - 0.5f) / 0.5f).coerceIn(0f, 1f)
        w1Scale = 1.0f
        w1Alpha = (1f - mp)
        w1Dx = -80f * p
    }

    // 3. Wave 2 bloom:
    // In 0..0.5: invisible
    // In 0.5..1.0: blooms from scale 0.3 to 1.0, alpha 0 to 1
    val w2Scale: Float
    val w2Alpha: Float

    if (v <= 0.5f) {
        w2Scale = 0.3f
        w2Alpha = 0f
    } else {
        val p = ((v - 0.5f) / 0.5f).coerceIn(0f, 1f)
        w2Scale = lerp(0.3f, 1.0f, p)
        w2Alpha = (p * 1.5f).coerceIn(0f, 1f) * (1f - mp)
    }

    // Transform coordinate system to 960x960 viewBox centered at (480, -480)
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
        translate(left = -480f, top = 480f)
    }) {
        // Draw Wave 1 if visible
        if (w1Alpha > 0.001f) {
            withTransform({
                translate(left = w1Dx, top = 0f)
                scale(scaleX = w1Scale, scaleY = w1Scale, pivot = Offset(640f + w1Dx, -480f))
            }) {
                drawPath(wave1Path, color = tint.copy(alpha = w1Alpha))
            }
        }

        // Draw Wave 2 if visible
        if (w2Alpha > 0.001f) {
            withTransform({
                scale(scaleX = w2Scale, scaleY = w2Scale, pivot = Offset(760f, -481f))
            }) {
                drawPath(wave2Path, color = tint.copy(alpha = w2Alpha))
            }
        }

        // Draw Cone with glide translation
        withTransform({
            translate(left = coneDx, top = 0f)
        }) {
            drawPath(conePath, color = tint)
        }

        // Draw Mute Slash across the speaker cone
        if (mp > 0.001f) {
            val slashStartX = 84f
            val slashStartY = -764f
            val slashTargetEndX = 764f
            val slashTargetEndY = -84f

            val slashEndX = lerp(slashStartX, slashTargetEndX, mp)
            val slashEndY = lerp(slashStartY, slashTargetEndY, mp)

            // Optional background cutout gap for crisp SVG-like notch effect
            if (cutoutColor != null && cutoutColor.isSpecified) {
                drawLine(
                    color = cutoutColor,
                    start = Offset(slashStartX, slashStartY),
                    end = Offset(slashEndX, slashEndY),
                    strokeWidth = 84f,
                    cap = StrokeCap.Round
                )
            }

            // Foreground diagonal slash stroke
            drawLine(
                color = tint,
                start = Offset(slashStartX, slashStartY),
                end = Offset(slashEndX, slashEndY),
                strokeWidth = 54f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
