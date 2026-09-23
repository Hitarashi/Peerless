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

private const val HOME_SVG_MORPH_SAMPLES = 512
private const val HOME_NORMAL_VIEWBOX_SCALE = 4f / 3f
private const val HOME_SOURCE_VIEWBOX_SIZE = 128f

private const val HOME_NORMAL_OUTER_PATH =
    "M11 33v45h1v3h1v1h1v1h1v1h2v1h3v1h15V59h1v-3h1v-1h1v-2h2v-1h1v-1h2v-1h10v1h2v1h1v1h2v2h1v1h1v3h1v27h15v-1h3v-1h2v-1h1v-1h1v-1h1v-2h1V34h-1v-3h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-1v-1h-2v-1h-2v-1h-1v-1h-2v-1h-2v-1h-2v-1h-1v-1h-2v-1h-2v-1h-2v-1h-1v-1h-2v-1h-4v-1h-2v1h-4v1h-2v1h-1v1h-2v1h-2v1h-2v1h-1v1h-2v1h-2v1h-1v1h-2v1h-2v1h-2v1h-1v1h-2v1h-2v1h-1v1h-2v2h-1v1h-1v2Z"
private const val HOME_NORMAL_DOOR_PATH =
    "M36 59v27h24V59h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-2v-1h-4v-1h-2v1h-4v1h-2v1h-1v1h-1v1h-1v2h-1v2Z"
private const val HOME_SELECTED_OUTER_PATH =
    "M12 55v52h1v4h1v1h1v1h1v1h1v1h3v1h20v-1h4v-1h1v-1h1v-1h1v-1h1v-3h1V84h1v-2h1v-2h1v-2h1v-1h1v-1h1v-1h2v-1h2v-1h10h2v1h2v1h1v1h1v1h1v1h1v1h1v2h1v6h1v23h1v1h1v1h1v1h1v1h4v1h19v-1h4v-1h1v-1h1v-1h1v-1h1v-4h1V55h-1v-5h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-5v-1h-4v1h-5v1h-2v1h-2v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v1h-1v1h-1v2h-1v2h-1v5Z"
private const val HOME_SELECTED_ARCH_PATH =
    "M49 108V84h1v-2h1v-2h1v-2h1v-1h1v-1h1v-1h2v-1h2v-1h10h2v1h2v1h1v1h1v1h1v1h1v1h1v2h1v6h1v23Z"

@Composable
fun HomeMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Home",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "HomeMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val bodyMorph = remember {
        val normalOuter = sampleSvgContour(HOME_NORMAL_OUTER_PATH, HOME_NORMAL_VIEWBOX_SCALE)
        val selectedOuter = sampleSvgContour(HOME_SELECTED_OUTER_PATH, 1f)
        SvgPathMorph(
            start = listOf(normalOuter),
            end = listOf(alignHomeContour(normalOuter, selectedOuter)),
        )
    }
    val doorMorph = remember(bodyMorph) {
        val initialDoor = sampleSvgContour(HOME_NORMAL_DOOR_PATH, HOME_NORMAL_VIEWBOX_SCALE)
        val selectedShell = alignHomeContour(
            initialDoor,
            bodyMorph.end.single(),
        )
        val selectedDoor = alignHomeContour(
            selectedShell,
            sampleSvgContour(HOME_SELECTED_ARCH_PATH, 1f),
        )
        HomeDoorMorph(
            initialDoor = initialDoor,
            selectedShell = selectedShell,
            selectedDoor = selectedDoor,
        )
    }
    val bodyPath = remember { Path() }
    val doorPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawHomeMorph(
            progress = progress,
            tint = tint,
            bodyMorph = bodyMorph,
            doorMorph = doorMorph,
            bodyPath = bodyPath,
            doorPath = doorPath,
        )
    }
}

private data class SvgPathMorph(
    val start: List<List<Offset>>,
    val end: List<List<Offset>>,
)

private data class HomeDoorMorph(
    val initialDoor: List<Offset>,
    val selectedShell: List<Offset>,
    val selectedDoor: List<Offset>,
)

private fun sampleSvgContour(pathData: String, viewBoxScale: Float): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply {
        setPath(sourcePath, forceClosed = true)
    }
    val contourLength = measure.length

    return List(HOME_SVG_MORPH_SAMPLES) { index ->
        val position = measure.getPosition(contourLength * index / HOME_SVG_MORPH_SAMPLES)
        Offset(position.x * viewBoxScale, position.y * viewBoxScale)
    }
}

private fun alignHomeContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawHomeMorph(
    progress: Float,
    tint: Color,
    bodyMorph: SvgPathMorph,
    doorMorph: HomeDoorMorph,
    bodyPath: Path,
    doorPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    updateMorphedPath(
        path = bodyPath,
        startContours = bodyMorph.start,
        endContours = bodyMorph.end,
        progress = bodyProgress,
    )
    updateHomeDoorPath(doorPath, doorMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / HOME_SOURCE_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = bodyPath,
            color = tint.copy(alpha = tint.alpha * lerp(0.4f, 1f, bodyProgress)),
        )
        if (p < 1f) {
            drawPath(
                path = doorPath,
                color = tint.copy(alpha = tint.alpha * homeDoorOpacity(p)),
            )
        }
    }
}

private fun updateHomeDoorPath(path: Path, morph: HomeDoorMorph, progress: Float) {
    val phaseProgress = if (progress <= 0.5f) {
        progress * 2f
    } else {
        (progress - 0.5f) * 2f
    }
    path.reset()
    path.fillType = PathFillType.NonZero
    val shellStart = if (progress <= 0.5f) morph.initialDoor else morph.selectedShell
    val shellEnd = if (progress <= 0.5f) morph.selectedShell else morph.selectedDoor
    appendMorphedContour(path, shellStart, shellEnd, phaseProgress)

    val doorStart = if (progress <= 0.5f) morph.initialDoor else morph.selectedDoor
    appendMorphedContour(path, doorStart, morph.selectedDoor, phaseProgress)
}

private fun appendMorphedContour(
    path: Path,
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
) {
    path.moveTo(
        lerp(start[0].x, end[0].x, progress),
        lerp(start[0].y, end[0].y, progress),
    )
    for (pointIndex in 1 until HOME_SVG_MORPH_SAMPLES) {
        path.lineTo(
            lerp(start[pointIndex].x, end[pointIndex].x, progress),
            lerp(start[pointIndex].y, end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun homeDoorOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun updateMorphedPath(
    path: Path,
    startContours: List<List<Offset>>,
    endContours: List<List<Offset>>,
    progress: Float,
) {
    path.reset()
    path.fillType = PathFillType.NonZero

    startContours.indices.forEach { contourIndex ->
        val start = startContours[contourIndex]
        val end = endContours[contourIndex]
        path.moveTo(
            lerp(start[0].x, end[0].x, progress),
            lerp(start[0].y, end[0].y, progress),
        )
        for (pointIndex in 1 until HOME_SVG_MORPH_SAMPLES) {
            path.lineTo(
                lerp(start[pointIndex].x, end[pointIndex].x, progress),
                lerp(start[pointIndex].y, end[pointIndex].y, progress),
            )
        }
        path.close()
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
