package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SearchMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Search",
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(selected) {
        if (selected) {
            animatable.snapTo(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1167,
                    easing = LinearEasing
                )
            )
        } else {
            animatable.snapTo(0f)
        }
    }

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val lensPath = remember {
        PathParser().parsePathString(SEARCH_LENS_PATH_DATA).toNodes().toPath()
    }
    val handlePath = remember {
        PathParser().parsePathString(SEARCH_HANDLE_PATH_DATA).toNodes().toPath()
    }

    val progress = animatable.value
    val frameFloat = (progress * 27f).coerceIn(0f, 27f)
    val lowerIndex = frameFloat.toInt().coerceIn(0, 26)
    val upperIndex = lowerIndex + 1
    val frac = frameFloat - lowerIndex

    val lowerOffset = lowerIndex * 3
    val upperOffset = upperIndex * 3

    val dx = SEARCH_FRAMES[lowerOffset] + (SEARCH_FRAMES[upperOffset] - SEARCH_FRAMES[lowerOffset]) * frac
    val dy = SEARCH_FRAMES[lowerOffset + 1] + (SEARCH_FRAMES[upperOffset + 1] - SEARCH_FRAMES[lowerOffset + 1]) * frac
    val rot = SEARCH_FRAMES[lowerOffset + 2] + (SEARCH_FRAMES[upperOffset + 2] - SEARCH_FRAMES[lowerOffset + 2]) * frac

    Canvas(modifier = descModifier.size(size)) {
        val scale = this.size.minDimension / 24f
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f

        translate(left = centerX + dx * scale, top = centerY + dy * scale) {
            rotate(degrees = rot, pivot = Offset.Zero) {
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                    drawPath(path = lensPath, color = tint.copy(alpha = tint.alpha * 0.35f))
                    drawPath(path = handlePath, color = tint)
                }
            }
        }
    }
}

private const val SEARCH_LENS_PATH_DATA =
    "M -1 -10 C 3.971 -10.000 8.000 -5.971 8.000 -1.000 C 8.000 0.760 7.495 2.403 6.621 3.790 C 6.621 3.790 6.358 6.248 6.358 6.248 C 6.358 6.248 3.790 6.621 3.790 6.621 C 2.403 7.495 0.760 8.000 -1.000 8.000 C -5.971 8.000 -10.000 3.971 -10.000 -1.000 C -10.000 -5.971 -5.971 -10.000 -1.000 -10.000 Z"

private const val SEARCH_HANDLE_PATH_DATA =
    "M 9.414 6.586 C 9.127 6.299 7.472 4.644 6.613 3.785 C 5.894 4.927 4.927 5.894 3.785 6.613 C 4.644 7.472 6.299 9.127 6.586 9.414 C 7.367 10.195 8.633 10.195 9.414 9.414 C 10.195 8.633 10.195 7.367 9.414 6.586 Z"

private val SEARCH_FRAMES = floatArrayOf(
    0.000f, 0.000f, 0.000f,
    0.000f, 0.000f, 0.000f,
    0.000f, 0.000f, 0.000f,
    0.000f, 0.000f, 0.000f,
    -0.223f, 0.051f, -1.000f,
    -0.485f, 0.092f, -2.000f,
    -0.764f, 0.115f, -3.000f,
    -1.040f, 0.113f, -4.000f,
    -1.293f, 0.077f, -5.000f,
    -1.500f, 0.000f, -6.000f,
    -1.683f, -0.180f, -4.800f,
    -1.796f, -0.446f, -3.600f,
    -1.827f, -0.747f, -2.400f,
    -1.766f, -1.032f, -1.200f,
    -1.600f, -1.250f, 0.000f,
    -1.327f, -1.386f, 1.200f,
    -0.971f, -1.461f, 2.400f,
    -0.581f, -1.469f, 3.600f,
    -0.208f, -1.401f, 4.800f,
    0.100f, -1.250f, 6.000f,
    0.218f, -1.085f, 5.000f,
    0.289f, -0.875f, 4.000f,
    0.308f, -0.639f, 3.000f,
    0.269f, -0.400f, 2.000f,
    0.168f, -0.180f, 1.000f,
    0.000f, 0.000f, 0.000f,
    0.000f, 0.000f, 0.000f,
    0.000f, 0.000f, 0.000f
)
