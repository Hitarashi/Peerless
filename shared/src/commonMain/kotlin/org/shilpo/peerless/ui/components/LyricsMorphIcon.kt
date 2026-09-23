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

private const val LYRICS_MORPH_SAMPLES = 512
private const val LYRICS_NORMAL_VIEWBOX_SCALE = 4f / 3f
private const val LYRICS_VIEWBOX_SIZE = 128f

private const val LYRICS_NORMAL_BODY_PATH =
    "M10 30v36h1v5h1v3h1v2h1v1h1v2h1v1h1v1h1v1h2v1h2v1h2v1h5v1h37v-1h6v-1h2v-1h2v-1h2v-1h1v-1h1v-1h1v-1h1v-2h1v-2h1v-3h1v-5h1V30h-1v-5h-1v-3h-1v-2h-1v-1h-1v-2h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-2v-1h-6v-1H30v1h-6v1h-2v1h-2v1h-2v1h-1v1h-1v1h-1v1h-1v2h-1v2h-1v3h-1v5Z " +
            "M26 49h1v-1h1v-1h20v1h1v1h1v3h-1v1h-1v1H28v-1h-1v-1h-1v-2Z " +
            "M26 64h1v-1h1v1h8v1h2v5h-1v1h-9v-1h-2v-2Z " +
            "M42 64h1v-1h1v-1h24v1h2v4h-1v1h-1v1H44v-1h-1v-1h-1v-2Z " +
            "M54 48h2v-1h12v1h1v1h1v4h-1v1H55v-1h-1v-4Z"

private const val LYRICS_NORMAL_GLYPH_PATH =
    "M27 49v3h1v1h20v-1h1v-3h-1v-1H28v1Z " +
            "M27 64v3h1v1h8v-1h1v-3h-1v-1h-8v1Z " +
            "M43 64v3h1v1h24v-1h1v-3h-1v-1H44v1Z " +
            "M55 49v3h1v1h12v-1h1v-3h-1v-1H56v1Z"

private const val LYRICS_SELECTED_BODY_PATH =
    "M14 35v57h1v4h1v3h1v2h1v2h1v1h1v1h1v1h1v1h1v1h1v1h1v1h2v1h2v1h2v1h3v1h59v-1h4v-1h2v-1h2v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-2h1v-2h1v-3h1v-3h1V36h-1v-4h-1v-3h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-2v-1h-2v-1h-3v-1H35v1h-4v1h-2v1h-2v1h-2v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v2h-1v2h-1v3h-1v3Z " +
            "M35 66h1v-2h2v-1h25v1h2v1h1v5h-1v1h-1v1H38v-1h-2v-2h-1v-2Z " +
            "M35 85h1v-1h1v-1h11v1h1v1h1v5h-1v1h-2v1h-9v-1h-2v-1h-1v-4Z " +
            "M56 86h1v-2h2v-1h32v1h1v1h1v4h-1v2h-2v1H60v-1h-2v-1h-1v-1h-1v-2Z " +
            "M72 65h1v-1h1v-1h16v1h2v1h1v5h-1v1h-1v1H75v-1h-2v-1h-1v-4Z"

@Composable
fun LyricsMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Lyrics",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LyricsMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val bodyMorph = remember {
        val startContours = sampleLyricsContours(
            LYRICS_NORMAL_BODY_PATH,
            LYRICS_NORMAL_VIEWBOX_SCALE,
        )
        val rawEndContours = sampleLyricsContours(LYRICS_SELECTED_BODY_PATH, 1f)
        check(startContours.size == 5 && rawEndContours.size == 5) {
            "Lyrics SVG states must contain a body and four lyric bars."
        }
        LyricsBodyMorph(
            start = startContours,
            end = startContours.indices.map { index ->
                alignLyricsContour(startContours[index], rawEndContours[index])
            },
        )
    }
    val glyphMorph = remember(bodyMorph) {
        val initialBars = sampleLyricsContours(
            LYRICS_NORMAL_GLYPH_PATH,
            LYRICS_NORMAL_VIEWBOX_SCALE,
        )
        check(initialBars.size == 4) { "The normal Lyrics SVG must contain four lyric bars." }
        val expandedBars = initialBars.map { bar ->
            alignLyricsContour(bar, bodyMorph.end.first())
        }
        LyricsGlyphMorph(
            initialBars = initialBars,
            expandedBars = expandedBars,
            selectedBars = bodyMorph.end.drop(1).mapIndexed { index, selectedBar ->
                alignLyricsContour(expandedBars[index], selectedBar)
            },
        )
    }
    val bodyPath = remember { Path() }
    val glyphPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawLyricsMorph(
            progress = progress,
            tint = tint,
            bodyMorph = bodyMorph,
            glyphMorph = glyphMorph,
            bodyPath = bodyPath,
            glyphPath = glyphPath,
        )
    }
}

private data class LyricsBodyMorph(
    val start: List<List<Offset>>,
    val end: List<List<Offset>>,
)

private data class LyricsGlyphMorph(
    val initialBars: List<List<Offset>>,
    val expandedBars: List<List<Offset>>,
    val selectedBars: List<List<Offset>>,
)

private fun sampleLyricsContours(pathData: String, viewBoxScale: Float): List<List<Offset>> =
    splitLyricsContours(pathData).map { contourData ->
        val sourcePath = PathParser().parsePathString(contourData).toNodes().toPath()
        val measure = PathMeasure().apply {
            setPath(sourcePath, forceClosed = true)
        }
        val contourLength = measure.length

        List(LYRICS_MORPH_SAMPLES) { index ->
            val position = measure.getPosition(contourLength * index / LYRICS_MORPH_SAMPLES)
            Offset(position.x * viewBoxScale, position.y * viewBoxScale)
        }
    }

private fun splitLyricsContours(pathData: String): List<String> {
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

private fun alignLyricsContour(start: List<Offset>, end: List<Offset>): List<Offset> {
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

private fun DrawScope.drawLyricsMorph(
    progress: Float,
    tint: Color,
    bodyMorph: LyricsBodyMorph,
    glyphMorph: LyricsGlyphMorph,
    bodyPath: Path,
    glyphPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    updateLyricsBodyPath(bodyPath, bodyMorph, bodyProgress)
    updateLyricsGlyphPath(glyphPath, glyphMorph, p)

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / LYRICS_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = bodyPath,
            color = tint.copy(alpha = tint.alpha * lyricsLerp(0.4f, 1f, bodyProgress)),
        )
        if (p < 1f) {
            drawPath(
                path = glyphPath,
                color = tint.copy(alpha = tint.alpha * lyricsGlyphOpacity(p)),
            )
        }
    }
}

private fun updateLyricsBodyPath(path: Path, morph: LyricsBodyMorph, progress: Float) {
    path.reset()
    path.fillType = PathFillType.EvenOdd
    morph.start.indices.forEach { contourIndex ->
        appendLyricsMorphedContour(
            path = path,
            start = morph.start[contourIndex],
            end = morph.end[contourIndex],
            progress = progress,
        )
    }
}

private fun updateLyricsGlyphPath(path: Path, morph: LyricsGlyphMorph, progress: Float) {
    val phaseProgress = if (progress <= 0.5f) progress * 2f else (progress - 0.5f) * 2f

    path.reset()
    path.fillType = PathFillType.NonZero
    morph.initialBars.indices.forEach { index ->
        val start = if (progress <= 0.5f) morph.initialBars[index] else morph.expandedBars[index]
        val end = if (progress <= 0.5f) morph.expandedBars[index] else morph.selectedBars[index]
        appendLyricsMorphedContour(path, start, end, phaseProgress)
    }
}

private fun appendLyricsMorphedContour(
    path: Path,
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
) {
    path.moveTo(
        lyricsLerp(start[0].x, end[0].x, progress),
        lyricsLerp(start[0].y, end[0].y, progress),
    )
    for (pointIndex in 1 until LYRICS_MORPH_SAMPLES) {
        path.lineTo(
            lyricsLerp(start[pointIndex].x, end[pointIndex].x, progress),
            lyricsLerp(start[pointIndex].y, end[pointIndex].y, progress),
        )
    }
    path.close()
}

private fun lyricsGlyphOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun lyricsLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
