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

private const val PLAY_PAUSE_VIEWBOX_SIZE = 128f
private const val PLAY_PAUSE_MORPH_SAMPLES = 512

private const val PLAY_PAUSE_PAUSE_PATH =
    "M26 29v70h1v3h1v1h1v2h2v1h1v1h2v1h2v1h7v-1h3v-1h2v-1h1v-1h1v-1h1v-2h1v-2h1V28h-1v-2h-1v-2h-1v-1h-1v-1h-2v-1h-1v-1h-3v-1h-7v1h-3v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v1h-1v3Z M75 28v72h1v2h1v2h1v1h1v1h2v1h1v1h3v1h7v-1h3v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-3h1V29h-1v-3h-1v-1h-1v-2h-2v-1h-1v-1h-2v-1h-2v-1h-7v1h-3v1h-2v1h-1v1h-1v1h-1v2h-1v2Z"

private const val PLAY_PAUSE_PLAY_PATH =
    "M27 49v29h1v13h1v4h1v2h1v2h1v2h1v1h1v1h2v1h1v1h3v1h10v-1h3v-1h3v-1h2v-1h2v-1h2v-1h2v-1h2v-1h2v-1h2v-1h1v-1h2v-1h2v-1h1v-1h2v-1h1v-1h2v-1h1v-1h1v-1h1v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-1h1v-2h1v-2h1v-3h1v-8h-1v-3h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-1v-1h-1v-1h-2v-1h-1v-1h-2v-1h-1v-1h-1v-1h-2v-1h-2v-1h-1v-1h-2v-1h-2v-1h-2v-1h-1v-1h-3v-1h-2v-1h-2v-1h-2v-1h-2v-1h-3v-1H40v1h-2v1h-2v1h-1v1h-2v2h-1v1h-1v2h-1v2h-1v4h-1v12Z"

@Composable
fun PlayPauseMorphIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isPlaying) "Pause" else "Play",
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = 0.001f,
        ),
        label = "PlayPauseMorphProgress",
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val morphContours = remember {
        val playContour = playPauseSampleContour(PLAY_PAUSE_PLAY_PATH)
        val pauseContours = playPauseSplitContours(PLAY_PAUSE_PAUSE_PATH)
            .map(::playPauseSampleContour)
        check(pauseContours.size == 2) {
            "Pause SVG must contain its two bar contours."
        }
        pauseContours.map { pauseContour ->
            PlayPauseContourMorph(
                play = playContour,
                pause = playPauseAlignContour(playContour, pauseContour),
            )
        }
    }
    val paths = remember { morphContours.map { Path() } }

    Canvas(modifier = descModifier.size(size)) {
        drawPlayPauseMorph(
            progress = progress,
            tint = tint,
            morphContours = morphContours,
            paths = paths,
        )
    }
}

private data class PlayPauseContourMorph(
    val play: List<Offset>,
    val pause: List<Offset>,
)

private fun playPauseSampleContour(pathData: String): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply { setPath(sourcePath, forceClosed = true) }
    val contourLength = measure.length

    return List(PLAY_PAUSE_MORPH_SAMPLES) { index ->
        measure.getPosition(contourLength * index / PLAY_PAUSE_MORPH_SAMPLES)
    }
}

private fun playPauseSplitContours(pathData: String): List<String> {
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

private fun playPauseAlignContour(
    play: List<Offset>,
    pause: List<Offset>,
): List<Offset> {
    val count = play.size
    var bestOffset = 0
    var bestReversed = false
    var bestScore = Float.POSITIVE_INFINITY

    for (reversed in listOf(false, true)) {
        for (offset in 0 until count) {
            var score = 0f
            for (index in 0 until count) {
                val pauseIndex = if (reversed) {
                    (offset - index + count) % count
                } else {
                    (offset + index) % count
                }
                val dx = play[index].x - pause[pauseIndex].x
                val dy = play[index].y - pause[pauseIndex].y
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
        val pauseIndex = if (bestReversed) {
            (bestOffset - index + count) % count
        } else {
            (bestOffset + index) % count
        }
        pause[pauseIndex]
    }
}

private fun DrawScope.drawPlayPauseMorph(
    progress: Float,
    tint: Color,
    morphContours: List<PlayPauseContourMorph>,
    paths: List<Path>,
) {
    val p = progress.coerceIn(0f, 1f)
    morphContours.forEachIndexed { contourIndex, morph ->
        playPauseUpdatePath(paths[contourIndex], morph, p)
    }

    val iconSize = min(size.width, size.height)
    val scale = iconSize / PLAY_PAUSE_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
    }) {
        paths.forEach { path -> drawPath(path = path, color = tint) }
    }
}

private fun playPauseUpdatePath(
    path: Path,
    morph: PlayPauseContourMorph,
    progress: Float,
) {
    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        playPauseLerp(morph.play[0].x, morph.pause[0].x, progress),
        playPauseLerp(morph.play[0].y, morph.pause[0].y, progress),
    )
    for (index in 1 until PLAY_PAUSE_MORPH_SAMPLES) {
        path.lineTo(
            playPauseLerp(morph.play[index].x, morph.pause[index].x, progress),
            playPauseLerp(morph.play[index].y, morph.pause[index].y, progress),
        )
    }
    path.close()
}

private fun playPauseLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
