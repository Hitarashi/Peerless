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

private const val LIBRARY_MORPH_SAMPLES = 512
private const val LIBRARY_NORMAL_VIEWBOX_SCALE = 4f / 3f
private const val LIBRARY_VIEWBOX_SIZE = 128f

private const val LIBRARY_NORMAL_BODY_PATH =
    "M33 11v2h1v1h1v1h26v-1h1v-1h1v-2h-1v-1h-1V9H35v1h-1v1ZM24 22v4h47v-1h1v-3h-1v-1H25v1ZM13 46v27h1v4h1v2h1v2h1v1h1v1h1v1h2v1h2v1h4v1h42v-1h4v-1h2v-1h2v-1h1v-1h1v-1h1v-2h1v-2h1v-3h1V46h-1v-4h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-3v-1H24v1h-2v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v4ZM31 69h1v-2h1v-2h1v-1h3v-1h1V49h1v-2h9v-1h5v-1h3v-1h2v-1h2v1h2v1h1v24h-1v2h-1v1h-1v1h-1v1h-6v-1h-2v-1h-1v-2h-1v-6h1v-1h1v-1h1v-1h2v-1h2v-8h-2v1h-7v1h-2v20h-1v1h-1v1h-1v1h-7v-1h-1v-1h-1v-1h-1v-2h-1v-2Z"
private const val LIBRARY_NORMAL_OVERLAY_PATH =
    "M32 69v3h1v2h1v1h1v1h6v-1h2v-2h1V53h4v-1h6v-1h3v10h-3v1h-2v1h-1v1h-1v6h1v1h1v1h1v1h5v-1h2v-1h1v-2h1V46h-1v-1h-1v-1h-2v1h-2v1h-3v1h-6v1h-7v1h-1v15h-2v1h-2v1h-1v1h-1v2Z"
private const val LIBRARY_FILLED_BODY_PATH =
    "M44 14v4h1v1h2v1h34v-1h2v-1h1v-4h-1v-1h-2v-1H47v1h-2v1ZM31 30v3h1v1h1v1h62v-1h1v-1h1v-2h-1v-2h-1v-1H33v1h-1v1ZM18 58v43h1v3h1v2h1v1h1v2h1v1h1v1h2v1h1v1h2v1h3v1h5v1h54v-1h6v-1h2v-1h2v-1h1v-1h2v-1h1v-2h1v-1h1v-1h1v-2h1v-3h1V58h-1v-3h-1v-2h-1v-1h-1v-1h-1v-2h-2v-1h-1v-1h-1v-1h-2v-1h-3v-1H32v1h-3v1h-2v1h-1v1h-1v1h-2v2h-1v1h-1v1h-1v2h-1v3ZM42 92h1v-3h1v-1h1v-1h1v-1h2v-1h3V66h1v-2h1v-1h2v-1h13v-1h4v-1h3v-1h2v-1h4v1h1v1h1v33h-1v2h-1v1h-1v1h-2v1h-3v1h-1v-1h-4v-1h-1v-1h-1v-1h-1v-1h-1v-4h-1v-1h1v-4h1v-1h1v-1h1v-1h2v-1h4V69h-3v1h-4v1h-8v26h-1v2h-1v1h-1v1h-1v1h-3v1h-4v-1h-2v-1h-2v-1h-1v-2h-1v-2h-1v-2Z"

@Composable
fun LibraryMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Library",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LibraryMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val bodyMorph = remember {
        val startContours = sampleSvgContours(
            LIBRARY_NORMAL_BODY_PATH,
            LIBRARY_NORMAL_VIEWBOX_SCALE,
        )
        val rawEndContours = sampleSvgContours(LIBRARY_FILLED_BODY_PATH, 1f)
        check(startContours.size == rawEndContours.size) {
            "Library SVG states must have matching contour counts."
        }
        val endContours = startContours.indices.map { contourIndex ->
            alignLibraryContour(startContours[contourIndex], rawEndContours[contourIndex])
        }
        LibrarySvgPathMorph(start = startContours, end = endContours)
    }
    val noteMorph = remember(bodyMorph) {
        val initialNote = sampleSvgContours(
            LIBRARY_NORMAL_OVERLAY_PATH,
            LIBRARY_NORMAL_VIEWBOX_SCALE,
        ).single()
        val fullLibrary = alignLibraryContour(initialNote, bodyMorph.end[2])
        val selectedNote = alignLibraryContour(fullLibrary, bodyMorph.end[3])
        LibraryNoteMorph(
            initialNote = initialNote,
            fullLibrary = fullLibrary,
            selectedNote = selectedNote,
        )
    }
    val bodyPath = remember { Path() }
    val notePath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawLibraryMorph(
            progress = progress,
            tint = tint,
            bodyMorph = bodyMorph,
            noteMorph = noteMorph,
            bodyPath = bodyPath,
            notePath = notePath,
        )
    }
}

private data class LibrarySvgPathMorph(
    val start: List<List<Offset>>,
    val end: List<List<Offset>>,
)

private data class LibraryNoteMorph(
    val initialNote: List<Offset>,
    val fullLibrary: List<Offset>,
    val selectedNote: List<Offset>,
)

private fun sampleSvgContours(pathData: String, viewBoxScale: Float): List<List<Offset>> =
    splitSvgContours(pathData).map { contourData ->
        val sourcePath = PathParser().parsePathString(contourData).toNodes().toPath()
        val measure = PathMeasure().apply {
            setPath(sourcePath, forceClosed = true)
        }
        val contourLength = measure.length

        List(LIBRARY_MORPH_SAMPLES) { index ->
            val position = measure.getPosition(contourLength * index / LIBRARY_MORPH_SAMPLES)
            Offset(position.x * viewBoxScale, position.y * viewBoxScale)
        }
    }

private fun splitSvgContours(pathData: String): List<String> {
    val contours = mutableListOf<String>()
    var contourStart = 0
    var previousNonWhitespace: Char? = null

    pathData.forEachIndexed { index, command ->
        if (command == 'M' && index > contourStart && previousNonWhitespace == 'Z') {
            contours += pathData.substring(contourStart, index)
            contourStart = index
        }
        if (!command.isWhitespace()) previousNonWhitespace = command
    }
    contours += pathData.substring(contourStart)
    return contours
}

private fun alignLibraryContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawLibraryMorph(
    progress: Float,
    tint: Color,
    bodyMorph: LibrarySvgPathMorph,
    noteMorph: LibraryNoteMorph,
    bodyPath: Path,
    notePath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    val barTouchProgress = if (p <= 0.5f) p * 2f else (1f - p) * 2f
    val secondBarBottom = bodyMorph.end[1].maxOf { it.y }
    val libraryTop = bodyMorph.end[2].minOf { it.y }
    val barTouchOffset = (libraryTop - secondBarBottom).coerceAtLeast(0f) * barTouchProgress

    updateMorphedPath(
        path = bodyPath,
        startContours = bodyMorph.start,
        endContours = bodyMorph.end,
        progress = bodyProgress,
        topBarsOffsetY = barTouchOffset,
    )
    updateLibraryNotePath(notePath, noteMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / LIBRARY_VIEWBOX_SIZE
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
                path = notePath,
                color = tint.copy(alpha = tint.alpha * libraryNoteOpacity(p)),
            )
        }
    }
}

private fun updateLibraryNotePath(path: Path, morph: LibraryNoteMorph, progress: Float) {
    val phaseProgress = if (progress <= 0.5f) {
        progress * 2f
    } else {
        (progress - 0.5f) * 2f
    }
    val start = if (progress <= 0.5f) morph.initialNote else morph.fullLibrary
    val end = if (progress <= 0.5f) morph.fullLibrary else morph.selectedNote

    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        lerp(start[0].x, end[0].x, phaseProgress),
        lerp(start[0].y, end[0].y, phaseProgress),
    )
    for (pointIndex in 1 until LIBRARY_MORPH_SAMPLES) {
        path.lineTo(
            lerp(start[pointIndex].x, end[pointIndex].x, phaseProgress),
            lerp(start[pointIndex].y, end[pointIndex].y, phaseProgress),
        )
    }
    path.close()
}

private fun libraryNoteOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun updateMorphedPath(
    path: Path,
    startContours: List<List<Offset>>,
    endContours: List<List<Offset>>,
    progress: Float,
    topBarsOffsetY: Float,
) {
    path.reset()
    path.fillType = PathFillType.EvenOdd

    startContours.indices.forEach { contourIndex ->
        val start = startContours[contourIndex]
        val end = endContours[contourIndex]
        val offsetY = if (contourIndex < 2) topBarsOffsetY else 0f
        path.moveTo(
            lerp(start[0].x, end[0].x, progress),
            lerp(start[0].y, end[0].y, progress) + offsetY,
        )
        for (pointIndex in 1 until LIBRARY_MORPH_SAMPLES) {
            path.lineTo(
                lerp(start[pointIndex].x, end[pointIndex].x, progress),
                lerp(start[pointIndex].y, end[pointIndex].y, progress) + offsetY,
            )
        }
        path.close()
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
