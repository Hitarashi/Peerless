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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val LIVE_RIPS_MORPH_SAMPLES = 512
private const val LIVE_RIPS_NORMAL_VIEWBOX_SCALE = 8f / 3f
private const val LIVE_RIPS_VIEWBOX_SIZE = 128f

private const val LIVE_RIPS_NORMAL_BODY_PATH =
    "M6 22v9h1v1h1v1h1v1h1v1h3v-4h1v-2h2v-1h3v-6h1v-1h1v-1h2v-1h2v1h2v1h1v1h1v1h1v6h3v1h2v2h1v4h3v-1h1v-1h1v-1h1v-1h1v-8h-1v-2h-1v-1h-1v-1h-2v-1h-3v-3h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1V9h-2V8h-6v1h-2v1h-1v1h-1v1h-1v1h-1v2h-1v3h-3v1H9v1H8v1H7v1Z"

private const val LIVE_RIPS_SELECTED_BODY_PATH =
    "M14 66v10h1v4h1v2h1v1h1v1h1v2h1v1h1v1h1v1h1v1h2v1h1v1h2v1h2v1h3v1h5v-2h-1v-9h1v-2h1v-2h1v-1h1v-1h2v-1h1v-1h3v-1h4V64h1v-3h1v-2h1v-1h1v-2h2v-1h1v-1h2v-1h10v1h2v1h1v1h2v2h1v1h1v2h1v3h1v11h4v1h3v1h2v1h1v1h1v2h1v1h1v2h1v10h-1v1h5v-1h3v-1h3v-1h1v-1h2v-1h1v-1h1v-1h1v-1h1v-2h1v-1h1v-2h1v-2h1v-4h1V65h-1v-3h-1v-2h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-2v-1h-4v-1h-2v-1h-1v-3h-1v-3h-1v-3h-1v-2h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-3v-1h-3v-1H58v1h-4v1h-2v1h-2v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v2h-1v2h-1v2h-1v4h-1v3h-3v1h-4v1h-3v1h-1v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v2h-1v2h-1v4Z"

private const val LIVE_RIPS_NORMAL_DOWNLOAD_ARROW_PATH =
    "M17 33v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h-1v-1h-1v1h-1v1h-1v1h-2V24h-2v11h-2v-1h-1v-1h-1v-1h-1v1Z"

@Composable
fun LiveRipsMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Live Rips",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LiveRipsMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val morph = remember {
        val normalBody = sampleLiveRipsContour(
            LIVE_RIPS_NORMAL_BODY_PATH,
            LIVE_RIPS_NORMAL_VIEWBOX_SCALE,
        )
        val selectedBody = sampleLiveRipsContour(LIVE_RIPS_SELECTED_BODY_PATH, 1f)
        val normalArrow = sampleLiveRipsContour(
            LIVE_RIPS_NORMAL_DOWNLOAD_ARROW_PATH,
            LIVE_RIPS_NORMAL_VIEWBOX_SCALE,
        )
        check(normalBody.size == LIVE_RIPS_MORPH_SAMPLES && selectedBody.size == LIVE_RIPS_MORPH_SAMPLES)
        LiveRipsMorph(
            normalBody = normalBody,
            selectedBody = alignLiveRipsContour(normalBody, selectedBody),
            normalArrow = normalArrow,
            expandedArrow = alignLiveRipsContour(normalArrow, selectedBody),
        )
    }
    val bodyPath = remember { Path() }
    val arrowPath = remember { Path() }
    val selectedArrowPath = remember {
        PathParser().parsePathString(LIVE_RIPS_NORMAL_DOWNLOAD_ARROW_PATH).toNodes().toPath()
    }

    Canvas(modifier = descModifier.size(size)) {
        drawLiveRipsMorph(
            progress = progress,
            tint = tint,
            morph = morph,
            bodyPath = bodyPath,
            arrowPath = arrowPath,
            selectedArrowPath = selectedArrowPath,
        )
    }
}

private data class LiveRipsMorph(
    val normalBody: List<Offset>,
    val selectedBody: List<Offset>,
    val normalArrow: List<Offset>,
    val expandedArrow: List<Offset>,
)

private fun sampleLiveRipsContour(pathData: String, viewBoxScale: Float): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply {
        setPath(sourcePath, forceClosed = true)
    }
    val contourLength = measure.length

    return List(LIVE_RIPS_MORPH_SAMPLES) { index ->
        val position = measure.getPosition(contourLength * index / LIVE_RIPS_MORPH_SAMPLES)
        Offset(position.x * viewBoxScale, position.y * viewBoxScale)
    }
}

private fun alignLiveRipsContour(start: List<Offset>, end: List<Offset>): List<Offset> {
    val count = start.size
    var bestOffset = 0
    var bestReversed = false
    var bestScore = Float.POSITIVE_INFINITY

    for (reversed in listOf(false, true)) {
        for (offset in 0 until count) {
            var score = 0f
            for (index in 0 until count) {
                val endIndex = if (reversed) {
                    (offset - index + count) % count
                } else {
                    (offset + index) % count
                }
                val dx = start[index].x - end[endIndex].x
                val dy = start[index].y - end[endIndex].y
                score += dx * dx + dy * dy
            }
            if (score < bestScore) {
                bestScore = score
                bestOffset = offset
                bestReversed = reversed
            }
        }
    }

    return List(count) { index ->
        val endIndex = if (bestReversed) {
            (bestOffset - index + count) % count
        } else {
            (bestOffset + index) % count
        }
        end[endIndex]
    }
}

private fun DrawScope.drawLiveRipsMorph(
    progress: Float,
    tint: Color,
    morph: LiveRipsMorph,
    bodyPath: Path,
    arrowPath: Path,
    selectedArrowPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    val arrowProgress = if (p <= 0.5f) p * 2f else (p - 0.5f) * 2f
    val arrowStart = if (p <= 0.5f) morph.normalArrow else morph.expandedArrow
    val arrowEnd = if (p <= 0.5f) morph.expandedArrow else morph.normalArrow

    updateLiveRipsContour(bodyPath, morph.normalBody, morph.selectedBody, bodyProgress)
    updateLiveRipsContour(arrowPath, arrowStart, arrowEnd, arrowProgress)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / LIVE_RIPS_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = bodyPath,
            color = tint.copy(alpha = tint.alpha * liveRipsLerp(0.4f, 1f, bodyProgress)),
        )
        if (p >= 0.999f) {
            withTransform({
                scale(
                    scaleX = LIVE_RIPS_NORMAL_VIEWBOX_SCALE,
                    scaleY = LIVE_RIPS_NORMAL_VIEWBOX_SCALE,
                    pivot = Offset.Zero,
                )
            }) {
                drawPath(path = selectedArrowPath, color = tint)
            }
        } else {
            drawPath(path = arrowPath, color = tint)
        }
    }
}

private fun updateLiveRipsContour(
    path: Path,
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
) {
    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        liveRipsLerp(start[0].x, end[0].x, progress),
        liveRipsLerp(start[0].y, end[0].y, progress),
    )
    for (pointIndex in 1 until LIVE_RIPS_MORPH_SAMPLES) {
        path.lineTo(
            liveRipsLerp(start[pointIndex].x, end[pointIndex].x, progress),
            liveRipsLerp(start[pointIndex].y, end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun liveRipsLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
