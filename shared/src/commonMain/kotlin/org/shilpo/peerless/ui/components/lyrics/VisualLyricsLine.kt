package org.shilpo.peerless.ui.components.lyrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.model.LyricsWordDto

/**
 * Word cluster visual metadata computed from lyrics timing.
 */
private class VisualWordCluster(
    val word: LyricsWordDto,
    val startIndex: Int,
    val endIndex: Int,
    val isHeavy: Boolean,
    val intensity: Float
)

/**
 * High-fidelity kinetic lyric line component directly implementing XMusic's signature
 * visual effects:
 * 1. Fluid gradient sweep karaoke with spring physics
 * 2. Held-vocal ("heavy") syllable elevation, elastic perspective scaling, and ambient glow
 * 3. Luminous sparkle particle emitter
 */
@Composable
fun VisualLyricsLine(
    text: String,
    words: List<LyricsWordDto>,
    positionMs: Long,
    isActive: Boolean,
    baseColor: Color,
    activeColor: Color,
    style: TextStyle,
    textAlign: TextAlign = TextAlign.Start,
    isPast: Boolean = false,
    config: VisualLyricsConfig = VisualLyricsConfig(),
    modifier: Modifier = Modifier
) {
    var textLayout by remember(text, style) { mutableStateOf<TextLayoutResult?>(null) }
    val particleEmitter = remember { SparkleParticleEmitter() }

    // Map words to character indices and compute "heavy" sustained syllable properties
    val clusters = remember(text, words) {
        var searchFrom = 0
        words.map { word ->
            val visibleText = word.text.trim()
            val range = if (visibleText.isEmpty()) {
                searchFrom to searchFrom
            } else {
                val found = text.indexOf(visibleText, searchFrom, ignoreCase = true)
                if (found >= 0) {
                    val end = (found + visibleText.length).coerceAtMost(text.length)
                    searchFrom = end
                    found to end
                } else {
                    val fallback = text.indexOf(visibleText, 0, ignoreCase = true)
                    if (fallback >= 0) {
                        val end = (fallback + visibleText.length).coerceAtMost(text.length)
                        searchFrom = end
                        fallback to end
                    } else {
                        searchFrom to searchFrom
                    }
                }
            }

            val durationMs = (word.end_ms - word.start_ms).coerceAtLeast(0L)
            val len = visibleText.length.coerceAtLeast(1)
            val msPerChar = durationMs.toFloat() / len.toFloat()
            val isHeavy = msPerChar >= 200f && len <= 9
            val intensity = if (isHeavy) {
                (0.3f + 0.7f * ((msPerChar - 200f) / 300f).coerceIn(0f, 1f))
            } else {
                0f
            }

            VisualWordCluster(
                word = word,
                startIndex = range.first,
                endIndex = range.second,
                isHeavy = isHeavy,
                intensity = intensity
            )
        }
    }

    // Active word determination
    val currentCluster = clusters.firstOrNull { cluster ->
        cluster.word.end_ms > cluster.word.start_ms &&
                positionMs >= cluster.word.start_ms &&
                positionMs < cluster.word.end_ms
    }

    // High-frequency frame ticker for particle updates and smooth elastic deformations
    var frameTick by remember { mutableStateOf(0L) }
    LaunchedEffect(isActive, config.enableSparkles) {
        if (!isActive) {
            particleEmitter.clear()
            return@LaunchedEffect
        }
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = now
            particleEmitter.update(dt)
            frameTick = now
        }
    }

    // If no word timestamps are provided (line-synced or plain LRC), render crisp text
    val isSimpleLrc = words.isEmpty()
    if (isSimpleLrc) {
        val displayColor = if (isActive || isPast) activeColor else baseColor
        Text(
            text = text,
            style = style,
            color = displayColor,
            textAlign = textAlign,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Base text rendering:
        // When the line has already been sung in the past, render directly in activeColor so it remains
        // 100% full and unbroken without cutoff artifacts.
        Text(
            text = text,
            style = style,
            color = if (isPast) activeColor else baseColor,
            textAlign = textAlign,
            onTextLayout = { textLayout = it },
            modifier = Modifier.fillMaxWidth()
        )

        Canvas(Modifier.matchParentSize()) {
            if (!isActive) return@Canvas

            // Read frameTick to trigger frame redraw during active singing
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick

            val layout = textLayout ?: return@Canvas
            val density = this.density
            val gradientWidthPx = 18.dp.toPx() // ~36-45px sweep transition width

            // For active lines, render each text line with continuous gradient sweep on top of base text
            for (lineIdx in 0 until layout.lineCount) {
                val lTop = layout.getLineTop(lineIdx)
                val lBottom = layout.getLineBottom(lineIdx)
                val lLeft = layout.getLineLeft(lineIdx)
                val lRight = layout.getLineRight(lineIdx)
                val lineStartOffset = layout.getLineStart(lineIdx)
                val lineEndOffset = layout.getLineEnd(lineIdx)

                val lineClusters =
                    clusters.filter { it.startIndex < lineEndOffset && it.endIndex > lineStartOffset }

                if (lineClusters.isEmpty()) {
                    // If no clusters on this line, highlight the entire line in activeColor
                    clipRect(
                        left = -200f,
                        top = lTop,
                        right = lRight + gradientWidthPx * 2f,
                        bottom = lBottom
                    ) {
                        drawText(
                            textLayoutResult = layout,
                            color = activeColor
                        )
                    }
                    continue
                }

                val lineStartMs = lineClusters.first().word.start_ms
                val lineEndMs = lineClusters.last().word.end_ms

                when {
                    // 1. Line already completed singing -> 100% activeColor
                    positionMs >= lineEndMs -> {
                        clipRect(
                            left = -200f,
                            top = lTop,
                            right = lRight + gradientWidthPx * 2f,
                            bottom = lBottom
                        ) {
                            drawText(
                                textLayoutResult = layout,
                                color = activeColor
                            )
                        }
                    }

                    // 2. Line not yet reached -> base text is already showing underneath
                    positionMs < lineStartMs -> {
                        // Inactive line ahead: do nothing, underlying baseColor text is already rendered
                    }

                    // 3. Line is currently being sung -> needle with LTR gradient opacity
                    else -> {
                        val needleWidthPx = 14.dp.toPx()
                        val headX = computeLineHeadX(
                            positionMs = positionMs,
                            lineClusters = lineClusters,
                            layout = layout,
                            defaultLeft = lLeft,
                            leadOffset = 0f
                        )

                        val needleStart = headX.coerceAtLeast(lLeft)
                        val needleEnd = needleStart + needleWidthPx

                        // 3a. Solid highlight behind the needle (100% solid activeColor)
                        if (needleStart > lLeft) {
                            clipRect(
                                left = -200f,
                                top = lTop,
                                right = needleStart + 2f,
                                bottom = lBottom
                            ) {
                                drawText(
                                    textLayoutResult = layout,
                                    color = activeColor,
                                    alpha = 1.0f
                                )
                            }
                        }

                        // 3b. Needle: LTR gradient from 100% activeColor down to 0% opacity (unclipped right edge)
                        val needleBrush = Brush.linearGradient(
                            colors = listOf(
                                activeColor,
                                activeColor.copy(alpha = 0f)
                            ),
                            start = Offset(needleStart, lTop),
                            end = Offset(needleEnd, lTop)
                        )
                        clipRect(
                            left = needleStart,
                            top = lTop,
                            right = lRight + 200f,
                            bottom = lBottom
                        ) {
                            drawText(
                                textLayoutResult = layout,
                                brush = needleBrush,
                                alpha = 1.0f
                            )
                        }

                        // Spawn sparkles at the leading brush head during active singing
                        if (config.enableSparkles && headX in lLeft..lRight) {
                            particleEmitter.spawn(
                                headX = headX,
                                topY = lTop,
                                bottomY = lBottom,
                                density = density
                            )
                        }
                    }
                }
            }

            // Draw floating sparkles over the active line if enabled
            if (config.enableSparkles) {
                particleEmitter.draw(this, activeColor)
            }
        }
    }
}

/**
 * Calculates continuous pixel coordinates for the brush head along a line of text,
 * smoothly interpolating across syllable durations and inter-word gaps.
 */
private fun computeLineHeadX(
    positionMs: Long,
    lineClusters: List<VisualWordCluster>,
    layout: TextLayoutResult,
    defaultLeft: Float,
    leadOffset: Float
): Float {
    val textLen = layout.layoutInput.text.length
    if (textLen == 0 || lineClusters.isEmpty()) return defaultLeft + leadOffset

    val currentCluster = lineClusters.firstOrNull { cluster ->
        cluster.word.end_ms > cluster.word.start_ms &&
                positionMs >= cluster.word.start_ms &&
                positionMs < cluster.word.end_ms
    }

    if (currentCluster != null) {
        val sIdx = currentCluster.startIndex.coerceIn(0, textLen - 1)
        val eIdx = (currentCluster.endIndex - 1).coerceIn(sIdx, textLen - 1)
        val startBox = layout.getBoundingBox(sIdx)
        val endBox = layout.getBoundingBox(eIdx)
        val wordStart = startBox.left
        val wordEnd = endBox.right
        val duration =
            (currentCluster.word.end_ms - currentCluster.word.start_ms).coerceAtLeast(1L).toFloat()
        val progress =
            ((positionMs - currentCluster.word.start_ms).toFloat() / duration).coerceIn(0f, 1f)
        return (wordStart + leadOffset) + (wordEnd - wordStart) * progress
    }

    // Between words on this line: smoothly interpolate across the whitespace gap
    val lastWord = lineClusters.lastOrNull { it.word.end_ms <= positionMs }
    val nextWord = lineClusters.firstOrNull { it.word.start_ms > positionMs }

    return when {
        lastWord != null && nextWord != null -> {
            val lIdx = (lastWord.endIndex - 1).coerceIn(0, textLen - 1)
            val nIdx = nextWord.startIndex.coerceIn(0, textLen - 1)
            val lastBox = layout.getBoundingBox(lIdx)
            val nextBox = layout.getBoundingBox(nIdx)
            val gapDuration =
                (nextWord.word.start_ms - lastWord.word.end_ms).coerceAtLeast(1L).toFloat()
            val gapProgress =
                ((positionMs - lastWord.word.end_ms).toFloat() / gapDuration).coerceIn(0f, 1f)
            (lastBox.right + leadOffset) + (nextBox.left - lastBox.right) * gapProgress
        }

        lastWord != null -> {
            val lIdx = (lastWord.endIndex - 1).coerceIn(0, textLen - 1)
            layout.getBoundingBox(lIdx).right + leadOffset
        }

        nextWord != null -> {
            val nIdx = nextWord.startIndex.coerceIn(0, textLen - 1)
            layout.getBoundingBox(nIdx).left + leadOffset
        }

        else -> defaultLeft + leadOffset
    }
}
