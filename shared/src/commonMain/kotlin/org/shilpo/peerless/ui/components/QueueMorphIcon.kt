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

private const val QUEUE_MORPH_SAMPLES = 512
private const val QUEUE_NORMAL_VIEWBOX_SCALE = 4f / 3f
private const val QUEUE_VIEWBOX_SIZE = 128f
private const val QUEUE_FIRST_BAR_CONTOUR = 2
private const val QUEUE_SECOND_BAR_CONTOUR = 3

private const val QUEUE_NORMAL_BODY_PATH =
    "M10 25v46h1v3h1v2h1v1h1v1h1v1h2v1h1v1h4v1h27v-1h4v-1h2v-1h1v-1h2v-2h1v-1h1v-2h1v-3h1V26h-1v-3h-1v-2h-1v-1h-1v-2h-2v-1h-1v-1h-2v-1h-3v-1H22v1h-3v1h-2v1h-2v1h-1v1h-1v1h-1v2h-1v3Z " +
            "M23 52h1v-2h1v-2h1v-1h1v-1h2v-1h8V32h1v-1h5v2h1v1h1v1h1v1h2v1h2v5h-1v1h-5v14h-1v3h-1v2h-1v1h-2v1h-1v1h-9v-1h-1v-1h-2v-1h-1v-2h-1v-2h-1v-4Z " +
            "M69 25v46h1v1h4V24h-4v1Z " +
            "M81 34v28h1v1h3v-1h1V34h-1v-1h-3v1Z " +
            "M30 53v4h1v1h5v-2h1v-3h-1v-1h-2v-1h-1v1h-2v1Z"

private const val QUEUE_SELECTED_BODY_PATH =
    "M13 34v60h1v3h1v2h1v2h1v1h1v1h1v1h1v1h1v1h1v1h2v1h3v1h41v-1h3v-1h2v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-2h1v-3h1v-5h1V38h-1v-6h-1v-2h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-3v-1H28v1h-4v1h-1v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v4Z " +
            "M31 70h1v-3h1v-2h1v-1h1v-1h1v-1h2v-1h2v-1h9V45h1v-2h1v-1h1v-1h3v1h2v1h1v2h1v1h1v1h1v1h2v1h2v1h1v1h1v4h-1v1h-1v1h-5v-1h-2v21h-1v2h-1v2h-1v1h-1v1h-1v1h-1v1h-1v1h-2v1h-2v1h-6v-1h-3v-1h-2v-1h-1v-1h-1v-1h-1v-1h-1v-2h-1v-2h-1v-6Z " +
            "M92 33v62h1v1h2v1h2v-1h1v-1h1V32h-2v-1h-2v1h-2v1Z " +
            "M108 45v38h2v1h3v-1h1v-1h1V46h-1v-1h-1v-1h-4v1Z " +
            "M40 71v5h1v1h1v1h5v-1h1v-1h1v-5h-1v-2h-3v-1h-1v1h-2v1h-1v1Z"

private const val QUEUE_NORMAL_NOTE_PATH =
    "M24 52v6h1v2h1v1h1v1h1v1h1v1h1v1h8v-1h2v-1h1v-1h1v-1h1v-2h1V41h1v1h5v-4h-1v-1h-3v-1h-1v-1h-1v-2h-1v-1h-3v1h-1v13h-4v-1h-1v1h-4v1h-1v1h-1v1h-1v1h-1v1h-1v2Z " +
            "M29 53h1v-1h1v-1h5v1h1v1h1v4h-1v1h-1v1h-5v-1h-1v-1h-1v-2Z"

@Composable
fun QueueMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Queue",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "QueueMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val bodyMorph = remember {
        val startContours = sampleQueueContours(
            QUEUE_NORMAL_BODY_PATH,
            QUEUE_NORMAL_VIEWBOX_SCALE,
        )
        val rawEndContours = sampleQueueContours(QUEUE_SELECTED_BODY_PATH, 1f)
        check(startContours.size == 5 && rawEndContours.size == 5) {
            "Queue SVG states must contain a body, a note, and two queue bars with a note counter."
        }
        val endContours = startContours.indices.map { index ->
            alignQueueContour(startContours[index], rawEndContours[index])
        }
        QueueBodyMorph(
            start = startContours,
            end = endContours,
            barsTouchOffsetX = endContours[0].maxOf { it.x } -
                    endContours[QUEUE_FIRST_BAR_CONTOUR].minOf { it.x },
        )
    }
    val glyphMorph = remember(bodyMorph) {
        val initialNoteContours = sampleQueueContours(
            QUEUE_NORMAL_NOTE_PATH,
            QUEUE_NORMAL_VIEWBOX_SCALE,
        )
        check(initialNoteContours.size == 2) { "The normal Queue SVG must contain two note contours." }
        val expandedNote = alignQueueContour(initialNoteContours.first(), bodyMorph.end.first())
        QueueGlyphMorph(
            initialNote = initialNoteContours.first(),
            expandedNote = expandedNote,
            selectedNote = alignQueueContour(expandedNote, bodyMorph.end[1]),
            initialCounter = initialNoteContours[1],
            selectedCounter = alignQueueContour(initialNoteContours[1], bodyMorph.end[4]),
        )
    }
    val bodyPath = remember { Path() }
    val glyphPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawQueueMorph(
            progress = progress,
            tint = tint,
            bodyMorph = bodyMorph,
            glyphMorph = glyphMorph,
            bodyPath = bodyPath,
            glyphPath = glyphPath,
        )
    }
}

private data class QueueBodyMorph(
    val start: List<List<Offset>>,
    val end: List<List<Offset>>,
    val barsTouchOffsetX: Float,
)

private data class QueueGlyphMorph(
    val initialNote: List<Offset>,
    val expandedNote: List<Offset>,
    val selectedNote: List<Offset>,
    val initialCounter: List<Offset>,
    val selectedCounter: List<Offset>,
)

private fun sampleQueueContours(pathData: String, viewBoxScale: Float): List<List<Offset>> =
    splitQueueContours(pathData).map { contourData ->
        val sourcePath = PathParser().parsePathString(contourData).toNodes().toPath()
        val measure = PathMeasure().apply {
            setPath(sourcePath, forceClosed = true)
        }
        val contourLength = measure.length

        List(QUEUE_MORPH_SAMPLES) { index ->
            val position = measure.getPosition(contourLength * index / QUEUE_MORPH_SAMPLES)
            Offset(position.x * viewBoxScale, position.y * viewBoxScale)
        }
    }

private fun splitQueueContours(pathData: String): List<String> {
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

private fun alignQueueContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawQueueMorph(
    progress: Float,
    tint: Color,
    bodyMorph: QueueBodyMorph,
    glyphMorph: QueueGlyphMorph,
    bodyPath: Path,
    glyphPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    val barsTouchProgress = if (p <= 0.5f) p * 2f else (1f - p) * 2f
    val barsTouchOffsetX = bodyMorph.barsTouchOffsetX * barsTouchProgress
    updateQueueBodyPath(bodyPath, bodyMorph, bodyProgress, barsTouchOffsetX)
    updateQueueGlyphPath(glyphPath, glyphMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / QUEUE_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = bodyPath,
            color = tint.copy(alpha = tint.alpha * queueLerp(0.4f, 1f, bodyProgress)),
        )
        if (p < 1f) {
            drawPath(
                path = glyphPath,
                color = tint.copy(alpha = tint.alpha * queueGlyphOpacity(p)),
            )
        }
    }
}

private fun updateQueueBodyPath(
    path: Path,
    morph: QueueBodyMorph,
    progress: Float,
    barsTouchOffsetX: Float,
) {
    path.reset()
    path.fillType = PathFillType.EvenOdd
    morph.start.indices.forEach { contourIndex ->
        appendQueueMorphedContour(
            path = path,
            start = morph.start[contourIndex],
            end = morph.end[contourIndex],
            progress = progress,
            offsetX = if (contourIndex == QUEUE_FIRST_BAR_CONTOUR ||
                contourIndex == QUEUE_SECOND_BAR_CONTOUR
            ) {
                barsTouchOffsetX
            } else {
                0f
            },
        )
    }
}

private fun updateQueueGlyphPath(path: Path, morph: QueueGlyphMorph, progress: Float) {
    val phaseProgress = if (progress <= 0.5f) progress * 2f else (progress - 0.5f) * 2f
    val noteStart = if (progress <= 0.5f) morph.initialNote else morph.expandedNote
    val noteEnd = if (progress <= 0.5f) morph.expandedNote else morph.selectedNote

    path.reset()
    path.fillType = PathFillType.NonZero
    appendQueueMorphedContour(path, noteStart, noteEnd, phaseProgress)
    appendQueueMorphedContour(path, morph.initialCounter, morph.selectedCounter, progress)
}

private fun appendQueueMorphedContour(
    path: Path,
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
    offsetX: Float = 0f,
) {
    path.moveTo(
        queueLerp(start[0].x, end[0].x, progress) + offsetX,
        queueLerp(start[0].y, end[0].y, progress),
    )
    for (pointIndex in 1 until QUEUE_MORPH_SAMPLES) {
        path.lineTo(
            queueLerp(start[pointIndex].x, end[pointIndex].x, progress) + offsetX,
            queueLerp(start[pointIndex].y, end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun queueGlyphOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun queueLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
