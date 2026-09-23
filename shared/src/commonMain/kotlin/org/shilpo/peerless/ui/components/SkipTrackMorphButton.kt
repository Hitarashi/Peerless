package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val SKIP_TRACK_VIEWBOX_SIZE = 96f
private const val SKIP_TRACK_TRIANGLE_TOUCH_DISTANCE = 7f
private const val SKIP_TRACK_MOTION_DURATION_MILLIS = 120

private const val NEXT_TRIANGLE_PATH =
    "M17 44v8h1v13h1v3h1v1h1v2h2v1h1v1h2v1h7v-1h2v-1h3v-1h2v-1h2v-1h2v-1h1v-1h2v-1h2v-1h1v-1h1v-1h2v-1h1v-1h1v-1h1v-1h2v-1h1v-1h1v-2h1v-1h1v-3h1v-6h-1v-2h-1v-2h-1v-1h-1v-2h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-2v-1h-2v-1h-1v-1h-2v-1h-2v-1h-2v-1h-2v-1h-3v-1h-7v1h-2v1h-2v1h-1v2h-1v1h-1v3h-1v13Z"
private const val NEXT_BAR_PATH =
    "M71 26v44h1v2h1v1h4v-1h1v-2h1V26h-1v-2h-1v-1h-4v1h-1v2Z"

private const val PREVIOUS_TRIANGLE_PATH =
    "M33 45v6h1v3h1v1h1v2h1v1h1v1h2v1h1v1h1v1h1v1h2v1h1v1h1v1h2v1h2v1h1v1h2v1h2v1h2v1h2v1h3v1h7v-1h2v-1h1v-1h2v-2h1v-1h1v-3h1V52h1v-7h-1V31h-1v-3h-1v-1h-1v-2h-1v-1h-2v-1h-2v-1h-7v1h-2v1h-3v1h-2v1h-2v1h-2v1h-1v1h-2v1h-1v1h-2v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v2h-1v1h-1v3Z"
private const val PREVIOUS_BAR_PATH =
    "M17 26v44h1v2h1v1h4v-1h1v-2h1V26h-1v-2h-1v-1h-4v1h-1v2Z"

@Composable
internal fun SkipTrackMorphButton(
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isNext) "Next Track" else "Previous Track",
    containerColor: Color? = null,
) {
    var animationTrigger by remember { mutableIntStateOf(0) }
    val colors = if (containerColor == null) {
        IconButtonDefaults.iconButtonColors()
    } else {
        IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = tint,
        )
    }

    IconButton(
        onClick = {
            animationTrigger += 1
            onClick()
        },
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(),
        colors = colors,
    ) {
        SkipTrackMorphIcon(
            isNext = isNext,
            animationTrigger = animationTrigger,
            size = iconSize,
            tint = tint,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun SkipTrackMorphIcon(
    isNext: Boolean,
    animationTrigger: Int,
    size: Dp,
    tint: Color,
    contentDescription: String?,
) {
    val triangleTravel = remember { Animatable(0f) }

    LaunchedEffect(animationTrigger) {
        if (animationTrigger > 0) {
            triangleTravel.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SKIP_TRACK_MOTION_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            triangleTravel.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = SKIP_TRACK_MOTION_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val trianglePath = remember(isNext) {
        parseSkipTrackPath(if (isNext) NEXT_TRIANGLE_PATH else PREVIOUS_TRIANGLE_PATH)
    }
    val barPath = remember(isNext) {
        parseSkipTrackPath(if (isNext) NEXT_BAR_PATH else PREVIOUS_BAR_PATH)
    }
    val iconModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(modifier = iconModifier.size(size)) {
        drawSkipTrackIcon(
            isNext = isNext,
            triangleTravel = triangleTravel.value,
            tint = tint,
            trianglePath = trianglePath,
            barPath = barPath,
        )
    }
}

private fun parseSkipTrackPath(pathData: String): Path =
    PathParser().parsePathString(pathData).toNodes().toPath()

private fun DrawScope.drawSkipTrackIcon(
    isNext: Boolean,
    triangleTravel: Float,
    tint: Color,
    trianglePath: Path,
    barPath: Path,
) {
    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / SKIP_TRACK_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f
    val direction = if (isNext) 1f else -1f
    val triangleOffsetX = direction * SKIP_TRACK_TRIANGLE_TOUCH_DISTANCE * triangleTravel

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        withTransform({ translate(left = triangleOffsetX, top = 0f) }) {
            drawPath(path = trianglePath, color = tint)
        }
        drawPath(path = barPath, color = tint)
    }
}
