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

private const val SEARCH_MORPH_SAMPLES = 512
private const val SEARCH_NORMAL_VIEWBOX_SCALE = 0.75f
private const val SEARCH_VIEWBOX_SIZE = 96f

private const val SEARCH_NORMAL_CIRCLE_PATH =
    "M15 50v19h1v3h1v4h1v2h1v2h1v2h1v2h1v2h1v1h1v1h1v1h1v1h1v1h1v2h1v1h2v1h1v1h1v1h1v1h1v1h2v1h2v1h2v1h2v1h2v1h4v1h3v1h17v-1h4v-1h3v-1h3v-1h2v-1h2v-1h2v-1h2v-1h1v-1h1v-1h1v-1h1v-1h2v-1h1v-2h1v-1h1v-1h1v-1h1v-1h1v-2h1v-2h1v-2h1v-1h1v-3h1v-3h1v-4h1V51h-1v-4h-1v-3h-1v-3h-1v-1h-1v-2h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-2v-1h-1v-1h-2v-1h-2v-1h-3v-1h-3v-1h-3v-1h-9v-1h-2v1h-9v1h-3v1h-3v1h-3v1h-1v1h-2v1h-2v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v2h-1v1h-1v1h-1v1h-1v2h-1v2h-1v1h-1v2h-1v3h-1v3h-1v3Z"
private const val SEARCH_NORMAL_HANDLE_PATH =
    "M95 104v1h1v4h1v1h1v1h1v1h1v1h4v1h1v-1h4v-1h1v-1h1v-1h1v-1h1v-8h-1v-2h-1v-1h-1v-1h-2v-1h-7v1h-2v1h-1v1h-1v1h-1v4Z"
private const val SEARCH_FILL_CIRCLE_PATH =
    "M11 40v10h1v4h1v3h1v3h1v2h1v1h1v2h1v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h2v1h1v1h2v1h3v1h3v1h4v1h10v-1h4v-1h3v-1h3v-1h1v-1h2v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-2h1v-1h1v-2h1v-2h1v-3h1v-5h1V39h-1v-4h-1v-3h-1v-2h-1v-2h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-2v-1h-3v-1h-4v-1H39v1h-4v1h-3v1h-2v1h-2v1h-1v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v1h-1v2h-1v2h-1v3h-1v3h-1v5Z"
private const val SEARCH_FILL_HANDLE_PATH =
    "M72 75v6h1v2h1v1h2v1h5v-1h1v-1h1v-1h1v-1h1v-5h-1v-2h-1v-1h-2v-1h-5v1h-2v1h-1v1Z"

@Composable
fun SearchMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Search",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SearchMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val inactiveLens = remember {
        searchPathFromContour(
            sampleSvgContour(SEARCH_NORMAL_CIRCLE_PATH, SEARCH_NORMAL_VIEWBOX_SCALE)
        )
    }
    val dotMorph = remember {
        val dot = sampleSvgContour(SEARCH_NORMAL_HANDLE_PATH, SEARCH_NORMAL_VIEWBOX_SCALE)
        SearchContourMorph(
            start = dot,
            end = alignSearchContour(dot, sampleSvgContour(SEARCH_FILL_CIRCLE_PATH, 1f)),
        )
    }
    val handleMorph = remember {
        val normalHandle = sampleSvgContour(
            SEARCH_NORMAL_HANDLE_PATH,
            SEARCH_NORMAL_VIEWBOX_SCALE,
        )
        SearchContourMorph(
            start = normalHandle,
            end = alignSearchContour(
                normalHandle,
                sampleSvgContour(SEARCH_FILL_HANDLE_PATH, 1f),
            ),
        )
    }
    val dotPath = remember { Path() }
    val handlePath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawSearchMorph(
            progress = progress,
            tint = tint,
            inactiveLens = inactiveLens,
            dotMorph = dotMorph,
            handleMorph = handleMorph,
            dotPath = dotPath,
            handlePath = handlePath,
        )
    }
}

private data class SearchContourMorph(
    val start: List<Offset>,
    val end: List<Offset>,
)

private fun sampleSvgContour(pathData: String, viewBoxScale: Float): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply {
        setPath(sourcePath, forceClosed = true)
    }
    val contourLength = measure.length

    return List(SEARCH_MORPH_SAMPLES) { index ->
        val position = measure.getPosition(contourLength * index / SEARCH_MORPH_SAMPLES)
        Offset(position.x * viewBoxScale, position.y * viewBoxScale)
    }
}

private fun searchPathFromContour(points: List<Offset>): Path = Path().apply {
    fillType = PathFillType.NonZero
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    close()
}

private fun alignSearchContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawSearchMorph(
    progress: Float,
    tint: Color,
    inactiveLens: Path,
    dotMorph: SearchContourMorph,
    handleMorph: SearchContourMorph,
    dotPath: Path,
    handlePath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    updateMorphedContour(dotPath, dotMorph, p)
    updateMorphedContour(handlePath, handleMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / SEARCH_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = inactiveLens,
            color = tint.copy(alpha = tint.alpha * 0.4f * (1f - p)),
        )
        drawPath(path = handlePath, color = tint.copy(alpha = tint.alpha * p))
        drawPath(path = dotPath, color = tint)
    }
}

private fun updateMorphedContour(
    path: Path,
    morph: SearchContourMorph,
    progress: Float,
) {
    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        lerp(morph.start[0].x, morph.end[0].x, progress),
        lerp(morph.start[0].y, morph.end[0].y, progress),
    )
    for (pointIndex in 1 until SEARCH_MORPH_SAMPLES) {
        path.lineTo(
            lerp(morph.start[pointIndex].x, morph.end[pointIndex].x, progress),
            lerp(morph.start[pointIndex].y, morph.end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
