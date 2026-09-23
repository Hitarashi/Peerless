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

private const val INFO_MORPH_SAMPLES = 512
private const val INFO_VIEWBOX_SIZE = 128f

private const val INFO_NORMAL_BODY_PATH =
    "M14 55v18h1v4h1v3h1v3h1v2h1v2h1v2h1v2h1v1h1v2h1v1h1v1h1v1h1v2h1v1h1v1h2v1h1v1h1v1h1v1h2v1h1v1h2v1h1v1h3v1h2v1h2v1h4v1h4v1h18v-1h4v-1h3v-1h3v-1h2v-1h2v-1h2v-1h2v-1h1v-1h2v-1h1v-1h1v-1h1v-1h2v-1h1v-1h1v-2h1v-1h1v-1h1v-1h1v-2h1v-1h1v-2h1v-1h1v-3h1v-2h1v-2h1v-4h1v-4h1V55h-1v-4h-1v-3h-1v-3h-1v-2h-1v-2h-1v-2h-1v-2h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-1v-2h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-2v-1h-1v-1h-3v-1h-2v-1h-2v-1h-4v-1h-4v-1H55v1h-4v1h-3v1h-3v1h-2v1h-2v1h-2v1h-2v1h-1v1h-2v1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v2h-1v1h-1v1h-1v1h-1v2h-1v1h-1v2h-1v1h-1v3h-1v2h-1v2h-1v4Z M60 42h1v-1h1v-1h4v1h1v1h1v5h-1v1h-2v1h-3v-1h-1v-1h-1v-4Z M60 58h1v-1h3v-1h1v1h2v1h1v28h-1v1h-1v1h-4v-1h-1v-1h-1V60Z"
private const val INFO_SELECTED_BODY_PATH =
    "M14 55v18h1v4h1v3h1v3h1v2h1v2h1v2h1v2h1v1h1v2h1v1h1v1h1v1h1v2h1v1h1v1h2v1h1v1h1v1h1v1h2v1h1v1h2v1h1v1h3v1h2v1h2v1h4v1h4v1h18v-1h4v-1h3v-1h3v-1h2v-1h2v-1h2v-1h2v-1h1v-1h2v-1h1v-1h1v-1h1v-1h2v-1h1v-1h1v-2h1v-1h1v-1h1v-1h1v-2h1v-1h1v-2h1v-1h1v-3h1v-2h1v-2h1v-4h1v-4h1V55h-1v-4h-1v-3h-1v-3h-1v-2h-1v-2h-1v-2h-1v-2h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-1v-2h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-2v-1h-1v-1h-3v-1h-2v-1h-2v-1h-4v-1h-4v-1H55v1h-4v1h-3v1h-3v1h-2v1h-2v1h-2v1h-2v1h-1v1h-2v-1h-1v1h-1v1h-1v1h-2v1h-1v1h-1v2h-1v1h-1v1h-1v1h-1v2h-1v1h-1v2h-1v1h-1v3h-1v2h-1v2h-1v4Z M60 44v1h1v2h1v1h4v-1h1v-2h1v-2h-1v-1h-1v-1h-4v1h-1v2Z M60 60v24h1v2h1v1h4v-1h1v-2h1V60h-1v-2h-1v-1h-4v1h-1v2Z"
private const val INFO_NORMAL_DOT_PATH = "M60 44v1h1v2h1v1h4v-1h1v-2h1v-2h-1v-1h-1v-1h-4v1h-1v2Z"
private const val INFO_NORMAL_STEM_PATH = "M60 60v24h1v2h1v1h4v-1h1v-2h1V60h-1v-2h-1v-1h-4v1h-1v2Z"

@Composable
fun InfoMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Info",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "InfoMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val bodyMorph = remember {
        val startContours = sampleInfoContours(INFO_NORMAL_BODY_PATH)
        val rawEndContours = sampleInfoContours(INFO_SELECTED_BODY_PATH)
        check(startContours.size == 3 && rawEndContours.size == 3) {
            "Info SVG states must contain an outer circle and two glyph contours."
        }
        InfoBodyMorph(
            start = startContours,
            end = startContours.indices.map { index ->
                alignInfoContour(startContours[index], rawEndContours[index])
            },
        )
    }
    val glyphMorph = remember(bodyMorph) {
        val initialDot = sampleInfoContour(INFO_NORMAL_DOT_PATH)
        val initialStem = sampleInfoContour(INFO_NORMAL_STEM_PATH)
        val fullCircleForDot = alignInfoContour(initialDot, bodyMorph.end[0])
        val fullCircleForStem = alignInfoContour(initialStem, bodyMorph.end[0])
        InfoGlyphMorph(
            initialDot = initialDot,
            fullCircleForDot = fullCircleForDot,
            selectedDot = alignInfoContour(fullCircleForDot, bodyMorph.end[1]),
            initialStem = initialStem,
            fullCircleForStem = fullCircleForStem,
            selectedStem = alignInfoContour(fullCircleForStem, bodyMorph.end[2]),
        )
    }
    val bodyPath = remember { Path() }
    val glyphPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawInfoMorph(
            progress = progress,
            tint = tint,
            bodyMorph = bodyMorph,
            glyphMorph = glyphMorph,
            bodyPath = bodyPath,
            glyphPath = glyphPath,
        )
    }
}

private data class InfoBodyMorph(
    val start: List<List<Offset>>,
    val end: List<List<Offset>>,
)

private data class InfoGlyphMorph(
    val initialDot: List<Offset>,
    val fullCircleForDot: List<Offset>,
    val selectedDot: List<Offset>,
    val initialStem: List<Offset>,
    val fullCircleForStem: List<Offset>,
    val selectedStem: List<Offset>,
)

private fun sampleInfoContours(pathData: String): List<List<Offset>> =
    splitInfoContours(pathData).map(::sampleInfoContour)

private fun sampleInfoContour(pathData: String): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply {
        setPath(sourcePath, forceClosed = true)
    }
    val contourLength = measure.length

    return List(INFO_MORPH_SAMPLES) { index ->
        measure.getPosition(contourLength * index / INFO_MORPH_SAMPLES)
    }
}

private fun splitInfoContours(pathData: String): List<String> {
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

private fun alignInfoContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawInfoMorph(
    progress: Float,
    tint: Color,
    bodyMorph: InfoBodyMorph,
    glyphMorph: InfoGlyphMorph,
    bodyPath: Path,
    glyphPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    updateInfoBodyPath(bodyPath, bodyMorph, bodyProgress)
    updateInfoGlyphPath(glyphPath, glyphMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / INFO_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = bodyPath,
            color = tint.copy(alpha = tint.alpha * infoLerp(0.4f, 1f, bodyProgress)),
        )
        if (p < 1f) {
            drawPath(
                path = glyphPath,
                color = tint.copy(alpha = tint.alpha * infoGlyphOpacity(p)),
            )
        }
    }
}

private fun updateInfoBodyPath(path: Path, morph: InfoBodyMorph, progress: Float) {
    path.reset()
    path.fillType = PathFillType.EvenOdd
    morph.start.indices.forEach { contourIndex ->
        appendInfoMorphedContour(
            path = path,
            start = morph.start[contourIndex],
            end = morph.end[contourIndex],
            progress = progress,
        )
    }
}

private fun updateInfoGlyphPath(path: Path, morph: InfoGlyphMorph, progress: Float) {
    val phaseProgress = if (progress <= 0.5f) progress * 2f else (progress - 0.5f) * 2f
    val firstStart = if (progress <= 0.5f) morph.initialDot else morph.fullCircleForDot
    val firstEnd = if (progress <= 0.5f) morph.fullCircleForDot else morph.selectedDot
    val secondStart = if (progress <= 0.5f) morph.initialStem else morph.fullCircleForStem
    val secondEnd = if (progress <= 0.5f) morph.fullCircleForStem else morph.selectedStem

    path.reset()
    path.fillType = PathFillType.NonZero
    appendInfoMorphedContour(path, firstStart, firstEnd, phaseProgress)
    appendInfoMorphedContour(path, secondStart, secondEnd, phaseProgress)
}

private fun appendInfoMorphedContour(
    path: Path,
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
) {
    path.moveTo(
        infoLerp(start[0].x, end[0].x, progress),
        infoLerp(start[0].y, end[0].y, progress),
    )
    for (pointIndex in 1 until INFO_MORPH_SAMPLES) {
        path.lineTo(
            infoLerp(start[pointIndex].x, end[pointIndex].x, progress),
            infoLerp(start[pointIndex].y, end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun infoGlyphOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun infoLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
