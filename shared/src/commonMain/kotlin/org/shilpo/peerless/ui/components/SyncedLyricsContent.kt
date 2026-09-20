package org.shilpo.peerless.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.shilpo.peerless.lyrics.LyricsLoadState
import org.shilpo.peerless.model.LyricsLineDto
import org.shilpo.peerless.model.LyricsResponse
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.LocalWindowWidthSizeClass
import org.shilpo.peerless.theme.WindowWidthSizeClass
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SyncedLyricsContent(
    state: LyricsLoadState,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showTranslations: Boolean = true,
    showRomanization: Boolean = true,
    accentColors: List<Color> = emptyList(),
    animationOptions: LyricsAnimationOptions = LyricsAnimationOptions()
) {
    when (state) {
        LyricsLoadState.Idle -> LyricsMessage(
            message = "Lyrics will appear here",
            modifier = modifier
        )

        LyricsLoadState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading lyrics…",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        LyricsLoadState.NotFound -> LyricsMessage(
            message = "No lyrics available for this track",
            modifier = modifier
        )

        is LyricsLoadState.Error -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Couldn’t load lyrics",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }

        is LyricsLoadState.Available -> LyricsLines(
            lyrics = state.lyrics,
            positionMs = positionMs,
            onSeekTo = onSeekTo,
            compact = compact,
            showTranslations = showTranslations,
            showRomanization = showRomanization,
            accentColors = accentColors,
            animationOptions = animationOptions,
            modifier = modifier
        )
    }
}

@Composable
private fun LyricsMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = ExpressiveTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LyricsLines(
    lyrics: LyricsResponse,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    compact: Boolean,
    showTranslations: Boolean,
    showRomanization: Boolean,
    accentColors: List<Color>,
    animationOptions: LyricsAnimationOptions,
    modifier: Modifier = Modifier
) {
    val lines = remember(lyrics) { lyrics.displayLines() }
    val desktopText = LocalWindowWidthSizeClass.current == WindowWidthSizeClass.EXPANDED
    val voiceAgents = remember(lyrics.track_id, lines) {
        lines.mapNotNull { it.agent }.distinct()
    }
    val isSynced = remember(lyrics, lines) {
        !lyrics.format.equals("plain", ignoreCase = true) &&
                lines.any { it.end_ms > it.start_ms || it.is_instrumental }
    }
    val activeLineIndex = if (isSynced) {
        lines.indexOfLast { line ->
            positionMs >= line.start_ms &&
                    (line.end_ms <= line.start_ms || positionMs < line.end_ms)
        }
    } else {
        -1
    }

    val listState = rememberLazyListState()
    var isFollowingPlayback by remember(lyrics.track_id) { mutableStateOf(true) }
    var showResumeButton by remember(lyrics.track_id) { mutableStateOf(false) }
    val isProgrammaticScroll = remember(lyrics.track_id) { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to isProgrammaticScroll.value }
            .distinctUntilChanged()
            .collectLatest { (isScrolling, isProgrammatic) ->
                if (isScrolling && !isProgrammatic) {
                    isFollowingPlayback = false
                    showResumeButton = true
                } else if (!isScrolling && !isFollowingPlayback && showResumeButton) {
                    delay(3_000)
                    isFollowingPlayback = true
                    showResumeButton = false
                }
            }
    }

    LaunchedEffect(activeLineIndex, isFollowingPlayback) {
        if (!isFollowingPlayback || activeLineIndex < 0) return@LaunchedEffect

        val viewportHeight = listState.layoutInfo.viewportSize.height
        isProgrammaticScroll.value = true
        try {
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = if (viewportHeight > 0) -(viewportHeight / 2) else 0
            )
        } finally {
            isProgrammaticScroll.value = false
        }
    }

    val lyricsListModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .then(
            if (animationOptions.edgeFeathering) {
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.045f to Color.Black.copy(alpha = 0.45f),
                                0.13f to Color.Black,
                                0.87f to Color.Black,
                                0.955f to Color.Black.copy(alpha = 0.45f),
                                1f to Color.Transparent
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            } else {
                Modifier
            }
        )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = lyricsListModifier,
            contentPadding = PaddingValues(
                start = if (compact) 8.dp else 24.dp,
                top = if (compact) 28.dp else 48.dp,
                end = if (compact) 8.dp else 24.dp,
                bottom = if (lyrics.attribution.isNullOrBlank()) 48.dp else 76.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                when {
                    compact && desktopText -> 20.dp
                    compact -> 16.dp
                    desktopText -> 32.dp
                    else -> 28.dp
                }
            )
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "${lyrics.track_id}_${index}_${line.start_ms}" }
            ) { index, line ->
                val distance =
                    if (activeLineIndex >= 0) kotlin.math.abs(index - activeLineIndex) else Int.MAX_VALUE
                val isActive = distance == 0
                val voiceAccent = when {
                    line.agent != null -> accentColors.getOrNull(voiceAgents.indexOf(line.agent))
                        ?: when (voiceAgents.indexOf(line.agent) % 3) {
                            0 -> MaterialTheme.colorScheme.primary
                            1 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }

                    line.alignment.equals("right", ignoreCase = true) ->
                        accentColors.getOrNull(1) ?: MaterialTheme.colorScheme.secondary

                    else -> accentColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
                }
                val targetColor = if (line.is_instrumental && isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val lineColor by animateColorAsState(targetColor, label = "LyricLineColor")
                val lineAlpha by animateFloatAsState(
                    targetValue = when {
                        activeLineIndex < 0 || !animationOptions.fadeOut -> 1f
                        distance == 0 -> 1f
                        distance == 1 -> 0.62f
                        distance == 2 -> 0.38f
                        distance == 3 -> 0.24f
                        else -> 0.14f
                    },
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                    label = "LyricLineAlpha"
                )
                val lineScale by animateFloatAsState(
                    targetValue = when {
                        activeLineIndex < 0 || !animationOptions.outOfSightScale -> 1f
                        distance == 0 -> 1f
                        distance == 1 -> 0.985f
                        distance == 2 -> 0.96f
                        else -> 0.93f
                    },
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                    label = "LyricLineScale"
                )
                val lineBlur by animateFloatAsState(
                    targetValue = when {
                        !animationOptions.blur -> 0f
                        activeLineIndex < 0 || distance == 0 -> 0f
                        distance == 1 -> 0.5f
                        distance == 2 -> 1.4f
                        distance == 3 -> 2.4f
                        else -> 3.2f
                    },
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                    label = "LyricLineBlur"
                )
                val lineIsSeekable = isSynced && line.end_ms > line.start_ms
                val lyricAlignment = when (line.alignment?.lowercase()) {
                    "left" -> TextAlign.Start
                    "right" -> TextAlign.End
                    else -> TextAlign.Center
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(lineBlur.dp)
                        .graphicsLayer {
                            alpha = lineAlpha
                            scaleX = lineScale
                            scaleY = lineScale
                        }
                        .then(
                            if (lineIsSeekable) {
                                Modifier.clickable {
                                    onSeekTo(line.start_ms)
                                    isFollowingPlayback = true
                                    showResumeButton = false
                                }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalAlignment = when (line.alignment?.lowercase()) {
                        "left" -> Alignment.Start
                        "right" -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                ) {
                    if (line.is_instrumental) {
                        Text(
                            text = "♫  Instrumental  ♫",
                            style = if (desktopText) {
                                ExpressiveTypography.bodyLarge
                            } else {
                                ExpressiveTypography.bodyMedium
                            },
                            fontWeight = FontWeight.Medium,
                            color = lineColor,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val mainText = line.text.ifBlank { line.words.joinToString("") { it.text } }
                        if (mainText.isNotBlank()) {
                            val hasWordTiming = line.words.any { word ->
                                word.end_ms > word.start_ms &&
                                        word.text.isNotBlank() && mainText.contains(word.text.trim())
                            }
                            val unplayedColor = if (isActive && isSynced && hasWordTiming) {
                                lineColor.copy(alpha = 0.30f)
                            } else {
                                lineColor
                            }
                            val lyricStyleTarget = when {
                                compact && isActive -> if (desktopText) {
                                    ExpressiveTypography.titleLarge
                                } else {
                                    ExpressiveTypography.titleMedium
                                }

                                compact -> if (desktopText) {
                                    ExpressiveTypography.bodyLarge
                                } else {
                                    ExpressiveTypography.bodyMedium
                                }

                                isActive -> if (desktopText) {
                                    ExpressiveTypography.headlineMedium
                                } else {
                                    ExpressiveTypography.headlineSmall
                                }

                                desktopText -> ExpressiveTypography.titleLarge
                                else -> ExpressiveTypography.titleMedium
                            }
                            val lyricFontSize by animateFloatAsState(
                                targetValue = lyricStyleTarget.fontSize.value,
                                animationSpec = tween(
                                    durationMillis = 420,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "LyricFontSize"
                            )
                            val lyricStyle = lyricStyleTarget.copy(
                                fontSize = lyricFontSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                            LyricsKaraokeText(
                                text = mainText,
                                words = if (animationOptions.wordByWord) line.words else emptyList(),
                                positionMs = positionMs,
                                baseColor = unplayedColor,
                                activeColor = voiceAccent,
                                scaleEnabled = animationOptions.scale,
                                floatEnabled = animationOptions.float,
                                glowEnabled = animationOptions.glow,
                                shadowEnabled = animationOptions.shadow,
                                textAlign = lyricAlignment,
                                style = lyricStyle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val backgroundText = remember(line) {
                            line.background_words.joinToString("") { it.text }
                        }
                        if (backgroundText.isNotBlank()) {
                            LyricsKaraokeText(
                                text = backgroundText,
                                words = if (animationOptions.wordByWord) line.background_words else emptyList(),
                                positionMs = positionMs,
                                baseColor = lineColor.copy(alpha = if (isActive) 0.3f else 0.68f),
                                activeColor = voiceAccent.copy(alpha = 0.9f),
                                scaleEnabled = animationOptions.scale,
                                floatEnabled = animationOptions.float,
                                glowEnabled = animationOptions.glow,
                                shadowEnabled = animationOptions.shadow,
                                textAlign = lyricAlignment,
                                style = (if (desktopText) {
                                    ExpressiveTypography.bodyLarge
                                } else {
                                    ExpressiveTypography.bodyMedium
                                }).copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                            )
                        }

                        if (showRomanization) line.romanization?.takeIf(String::isNotBlank)
                            ?.let { romanization ->
                                Text(
                                    text = romanization,
                                    style = if (desktopText) {
                                        ExpressiveTypography.bodyMedium
                                    } else {
                                        ExpressiveTypography.bodySmall
                                    },
                                    color = lineColor.copy(alpha = 0.6f),
                                    textAlign = lyricAlignment,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        if (showTranslations) line.translations.forEach { translation ->
                            Text(
                                text = translation.text,
                                style = if (desktopText) {
                                    ExpressiveTypography.bodyMedium
                                } else {
                                    ExpressiveTypography.bodySmall
                                },
                                color = lineColor.copy(alpha = 0.6f),
                                textAlign = lyricAlignment,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = showResumeButton) {
                TextButton(
                    onClick = {
                        isFollowingPlayback = true
                        showResumeButton = false
                    }
                ) {
                    Text("Resume")
                }
            }
            lyrics.attribution?.takeIf(String::isNotBlank)?.let { attribution ->
                Text(
                    text = attribution,
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun LyricsResponse.displayLines(): List<LyricsLineDto> {
    val nonBlankLines = lines.filter {
        it.text.isNotBlank() || it.background_words.isNotEmpty() || it.is_instrumental
    }
    if (nonBlankLines.isNotEmpty()) return nonBlankLines

    return plain_text.orEmpty()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { text ->
            LyricsLineDto(
                text = text,
                start_ms = 0L,
                end_ms = 0L
            )
        }
        .toList()
}

@Composable
private fun LyricsKaraokeText(
    text: String,
    words: List<org.shilpo.peerless.model.LyricsWordDto>,
    positionMs: Long,
    baseColor: Color,
    activeColor: Color,
    scaleEnabled: Boolean,
    floatEnabled: Boolean,
    glowEnabled: Boolean,
    shadowEnabled: Boolean,
    textAlign: TextAlign,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val currentWord = words.firstOrNull { word ->
        word.end_ms > word.start_ms && positionMs >= word.start_ms && positionMs < word.end_ms
    }
    val targetProgress = currentWord?.let { word ->
        ((positionMs - word.start_ms).toFloat() / (word.end_ms - word.start_ms).toFloat())
            .coerceIn(0f, 1f)
    } ?: 0f
    val currentProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "LyricWordProgress"
    )
    val longWordPulse by animateFloatAsState(
        targetValue = if (
            scaleEnabled && currentWord != null && currentWord.end_ms - currentWord.start_ms >= 700L
        ) {
            sin(currentProgress * PI).toFloat()
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "LongSyllableScale"
    )
    val wordFloat by animateFloatAsState(
        targetValue = if (floatEnabled && currentWord != null) {
            sin(currentProgress * PI).toFloat()
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "LyricWordFloat"
    )
    val wordRanges = remember(text, words) {
        var searchFrom = 0
        words.map { word ->
            val visibleWord = word.text.trim()
            if (visibleWord.isEmpty()) {
                searchFrom to searchFrom
            } else {
                val found = text.indexOf(visibleWord, searchFrom)
                if (found < 0) {
                    searchFrom to searchFrom
                } else {
                    val range = found to (found + visibleWord.length).coerceAtMost(text.length)
                    searchFrom = range.second
                    range
                }
            }
        }
    }
    var textLayout by remember(text, words, style) { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = baseColor,
            textAlign = textAlign,
            onTextLayout = { textLayout = it },
            modifier = Modifier.fillMaxWidth()
        )
        Canvas(Modifier.matchParentSize()) {
            val layout = textLayout ?: return@Canvas
            var textOffset = 0
            words.forEachIndexed { index, word ->
                val range = wordRanges.getOrNull(index) ?: (textOffset to textOffset)
                val start = range.first
                val end = range.second
                textOffset = end
                if (end <= start || positionMs < word.start_ms) return@forEachIndexed

                val fillFraction = when {
                    word.end_ms <= word.start_ms -> 0f
                    positionMs >= word.end_ms -> 1f
                    word == currentWord -> currentProgress
                    else -> 0f
                }
                if (fillFraction <= 0f) return@forEachIndexed

                var left = Float.POSITIVE_INFINITY
                var top = Float.POSITIVE_INFINITY
                var right = Float.NEGATIVE_INFINITY
                var bottom = Float.NEGATIVE_INFINITY
                for (offset in start until end) {
                    if (text[offset].isWhitespace()) continue
                    val bounds = layout.getBoundingBox(offset)
                    left = minOf(left, bounds.left)
                    top = minOf(top, bounds.top)
                    right = maxOf(right, bounds.right)
                    bottom = maxOf(bottom, bounds.bottom)
                }
                if (!left.isFinite() || right <= left) return@forEachIndexed

                val isCurrentWord = currentWord == word
                val isLongCurrentWord = isCurrentWord && word.end_ms - word.start_ms >= 700L
                val revealRight = left + (right - left) * fillFraction
                val featherWidth = if (isCurrentWord) {
                    minOf(12.dp.toPx(), (right - left) * 0.18f)
                } else {
                    0f
                }
                val solidRight = (revealRight - featherWidth).coerceAtLeast(left)
                val wordShadow = when {
                    glowEnabled && isLongCurrentWord -> Shadow(
                        color = activeColor.copy(alpha = 0.38f),
                        blurRadius = 7.dp.toPx()
                    )

                    shadowEnabled -> Shadow(
                        color = Color.Black.copy(alpha = 0.24f),
                        blurRadius = 2.dp.toPx()
                    )

                    else -> null
                }

                withTransform({
                    if (isCurrentWord && floatEnabled) {
                        translate(top = -8f * wordFloat)
                    }
                    if (isCurrentWord && scaleEnabled && isLongCurrentWord) {
                        val wordCenter = Offset(
                            (left + right) / 2f,
                            (top + bottom) / 2f
                        )
                        val wordScale = 1f + 0.15f * longWordPulse
                        scale(wordScale, wordScale, wordCenter)
                    }
                }) {
                    if (solidRight > left) {
                        clipRect(left = left, top = top, right = solidRight, bottom = bottom) {
                            drawText(layout, color = activeColor, shadow = wordShadow)
                        }
                    }
                    if (isCurrentWord && revealRight > solidRight) {
                        val featherBrush = Brush.linearGradient(
                            colors = listOf(activeColor, Color.Transparent),
                            start = Offset(solidRight, top),
                            end = Offset(revealRight.coerceAtLeast(solidRight + 1f), top)
                        )
                        clipRect(
                            left = solidRight,
                            top = top,
                            right = revealRight,
                            bottom = bottom
                        ) {
                            drawText(layout, brush = featherBrush, shadow = wordShadow)
                        }
                    }
                }
            }
        }
    }
}
