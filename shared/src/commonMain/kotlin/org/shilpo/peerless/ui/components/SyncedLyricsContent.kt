package org.shilpo.peerless.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import org.shilpo.peerless.ui.components.lyrics.VisualLyricsConfig
import org.shilpo.peerless.ui.components.lyrics.VisualLyricsLine
import org.shilpo.peerless.ui.components.lyrics.WaitingDotsView
import org.shilpo.peerless.ui.components.lyrics.isInstrumentalLineVisible

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
    animationOptions: LyricsAnimationOptions = LyricsAnimationOptions(),
    readableMode: Boolean = false,
    config: VisualLyricsConfig = VisualLyricsConfig()
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

        is LyricsLoadState.Available -> VisualLyricsLines(
            lyrics = state.lyrics,
            positionMs = positionMs,
            onSeekTo = onSeekTo,
            compact = compact,
            showTranslations = showTranslations,
            showRomanization = showRomanization,
            accentColors = accentColors,
            animationOptions = animationOptions,
            readableMode = readableMode,
            config = config,
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
private fun ReadableLyricsLine(
    text: String,
    words: List<org.shilpo.peerless.model.LyricsWordDto>,
    positionMs: Long,
    isActive: Boolean,
    isPast: Boolean,
    baseColor: Color,
    activeColor: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val annotatedText = remember(text, words, positionMs, isActive, baseColor, activeColor) {
        buildAnnotatedString {
            var cursor = 0
            words.forEach { word ->
                val spokenWord = word.text
                if (spokenWord.isBlank()) return@forEach
                val start = text.indexOf(spokenWord, cursor, ignoreCase = true)
                if (start < cursor) return@forEach

                append(text.substring(cursor, start))
                val end = (start + spokenWord.length).coerceAtMost(text.length)
                withStyle(
                    SpanStyle(
                        color = if (isActive && positionMs >= word.start_ms) activeColor else baseColor
                    )
                ) {
                    append(text.substring(start, end))
                }
                cursor = end
            }
            append(text.substring(cursor))
        }
    }
    Text(
        text = annotatedText,
        style = style.copy(color = if (isActive || isPast) activeColor else baseColor),
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun VisualLyricsLines(
    lyrics: LyricsResponse,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    compact: Boolean,
    showTranslations: Boolean,
    showRomanization: Boolean,
    accentColors: List<Color>,
    animationOptions: LyricsAnimationOptions,
    readableMode: Boolean,
    config: VisualLyricsConfig,
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

    val effectivePositionMs = (positionMs + config.syncOffsetMs).coerceAtLeast(0L)

    val activeLineIndex = if (isSynced) {
        val directIndex = lines.indexOfLast { line ->
            effectivePositionMs >= line.start_ms &&
                    (line.end_ms <= line.start_ms || effectivePositionMs < line.end_ms)
        }
        if (directIndex >= 0) {
            directIndex
        } else {
            lines.indexOfLast { line -> effectivePositionMs >= line.start_ms }
        }
    } else {
        -1
    }

    val listState = rememberLazyListState()
    val lineSpacing = when {
        compact && desktopText -> 22.dp
        compact -> 18.dp
        desktopText -> 32.dp
        else -> 26.dp
    }
    var isFollowingPlayback by remember(lyrics.track_id) { mutableStateOf(true) }
    var isUserInteracting by remember(lyrics.track_id) { mutableStateOf(false) }
    var showResumeButton by remember(lyrics.track_id) { mutableStateOf(false) }
    val isProgrammaticScroll = remember(lyrics.track_id) { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to isProgrammaticScroll.value }
            .distinctUntilChanged()
            .collectLatest { (isScrolling, isProgrammatic) ->
                if (isScrolling && !isProgrammatic) {
                    isUserInteracting = true
                    isFollowingPlayback = false
                    showResumeButton = true
                } else if (!isScrolling && !isFollowingPlayback && showResumeButton) {
                    delay(config.idleRecoveryMs)
                    isUserInteracting = false
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
                scrollOffset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
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
                                0.12f to Color.Black,
                                0.88f to Color.Black,
                                0.96f to Color.Black.copy(alpha = 0.45f),
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
                start = if (compact) 24.dp else 36.dp,
                top = if (compact) 36.dp else 56.dp,
                end = if (compact) 24.dp else 36.dp,
                bottom = if (lyrics.attribution.isNullOrBlank()) 64.dp else 96.dp
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "${lyrics.track_id}_${index}_${line.start_ms}" }
            ) { index, line ->
                val rowBottomSpacing = if (index < lines.lastIndex) lineSpacing else 0.dp
                val distance = if (activeLineIndex >= 0) {
                    kotlin.math.abs(index - activeLineIndex)
                } else {
                    Int.MAX_VALUE
                }
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

                val lineColor = MaterialTheme.colorScheme.onSurface

                val effectiveBlur = when {
                    isUserInteracting || !config.enableBlur || !animationOptions.blur -> 0f
                    activeLineIndex < 0 || distance == 0 -> 0f
                    distance == 1 -> 1.5f
                    distance == 2 -> 3.0f
                    else -> config.maxBlurRadiusDp
                }
                val lineBlur by animateFloatAsState(
                    targetValue = effectiveBlur,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "VisualLineBlur"
                )

                val lineAlpha by animateFloatAsState(
                    targetValue = when {
                        readableMode || activeLineIndex < 0 || !animationOptions.fadeOut -> 1f
                        isActive -> 1.0f
                        else -> 0.35f
                    },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "VisualLineAlpha"
                )

                val lineScale by animateFloatAsState(
                    targetValue = when {
                        activeLineIndex < 0 || !animationOptions.outOfSightScale -> 1f
                        isActive -> 1.01f
                        else -> 1.0f
                    },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "VisualLineScale"
                )

                val lineIsSeekable = isSynced && line.end_ms > line.start_ms

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(lineBlur.dp)
                        .graphicsLayer {
                            alpha = lineAlpha
                            scaleX = lineScale
                            scaleY = lineScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                        .then(
                            if (lineIsSeekable) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val targetSeekMs =
                                        (line.start_ms - config.syncOffsetMs).coerceAtLeast(0L)
                                    onSeekTo(targetSeekMs)
                                    isFollowingPlayback = true
                                    isUserInteracting = false
                                    showResumeButton = false
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(bottom = if (line.is_instrumental) 0.dp else rowBottomSpacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (line.is_instrumental) {
                        val instrumentalEndMs = remember(lines, index, lyrics.duration_ms) {
                            lines.drop(index + 1)
                                .firstOrNull { it.start_ms > line.start_ms }
                                ?.start_ms
                                ?: lyrics.duration_ms.takeIf { it > line.start_ms }
                        }
                        AnimatedVisibility(
                            visible = isInstrumentalLineVisible(
                                line = line,
                                fallbackEndMs = instrumentalEndMs,
                                positionMs = effectivePositionMs
                            ),
                            enter = expandVertically(
                                expandFrom = Alignment.CenterVertically,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeIn(
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ) + slideInVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) { height -> height / 3 },
                            exit = shrinkVertically(
                                shrinkTowards = Alignment.CenterVertically,
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ) + fadeOut(
                                animationSpec = tween(140, easing = FastOutSlowInEasing)
                            ) + slideOutVertically(
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ) { height -> -height / 4 }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = rowBottomSpacing)
                            ) {
                                if (config.enableWaitingDots) {
                                    WaitingDotsView(
                                        startTime = line.start_ms,
                                        endTime = line.end_ms.takeIf { it > line.start_ms }
                                            ?: instrumentalEndMs
                                            ?: line.end_ms,
                                        currentProgressMs = effectivePositionMs,
                                        primaryColor = voiceAccent
                                    )
                                } else {
                                    Text(
                                        text = "♫  Instrumental  ♫",
                                        style = if (desktopText) {
                                            ExpressiveTypography.bodyLarge
                                        } else {
                                            ExpressiveTypography.bodyMedium
                                        },
                                        fontWeight = FontWeight.Medium,
                                        color = lineColor,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    } else {
                        val mainText = line.text.ifBlank { line.words.joinToString("") { it.text } }
                        if (mainText.isNotBlank()) {
                            val baseFontSize = if (compact) 30.sp else 36.sp
                            val lyricStyle = TextStyle(
                                fontSize = baseFontSize * config.fontSizeScale,
                                lineHeight = (baseFontSize * config.fontSizeScale) * 1.25f,
                                fontWeight = config.fontWeight
                            )

                            val isPast = activeLineIndex >= 0 && index < activeLineIndex
                            val unplayedColor =
                                lineColor.copy(alpha = if (readableMode) 1f else 0.35f)
                            val singingActiveColor =
                                if (line.agent != null) voiceAccent else lineColor

                            if (readableMode) {
                                ReadableLyricsLine(
                                    text = mainText,
                                    words = line.words,
                                    positionMs = effectivePositionMs,
                                    isActive = isActive,
                                    isPast = isPast,
                                    baseColor = unplayedColor,
                                    activeColor = singingActiveColor,
                                    style = lyricStyle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                VisualLyricsLine(
                                    text = mainText,
                                    words = if (animationOptions.wordByWord) line.words else emptyList(),
                                    positionMs = effectivePositionMs,
                                    isActive = isActive,
                                    isPast = isPast,
                                    baseColor = unplayedColor,
                                    activeColor = singingActiveColor,
                                    style = lyricStyle,
                                    textAlign = TextAlign.Start,
                                    config = config,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val backgroundText = remember(line) {
                            line.background_words.joinToString("") { it.text }
                        }
                        if (backgroundText.isNotBlank()) {
                            val bgIsPast = activeLineIndex >= 0 && index < activeLineIndex
                            val backgroundBaseColor = lineColor.copy(
                                alpha = if (readableMode) 1f else if (isActive) 0.3f else 0.55f
                            )
                            val backgroundStyle = (if (desktopText) {
                                ExpressiveTypography.bodyLarge
                            } else {
                                ExpressiveTypography.bodyMedium
                            }).copy(fontWeight = FontWeight.Medium)
                            if (readableMode) {
                                ReadableLyricsLine(
                                    text = backgroundText,
                                    words = line.background_words,
                                    positionMs = effectivePositionMs,
                                    isActive = isActive,
                                    isPast = bgIsPast,
                                    baseColor = backgroundBaseColor,
                                    activeColor = voiceAccent,
                                    style = backgroundStyle,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            } else {
                                VisualLyricsLine(
                                    text = backgroundText,
                                    words = if (animationOptions.wordByWord) line.background_words else emptyList(),
                                    positionMs = effectivePositionMs,
                                    isActive = isActive,
                                    isPast = bgIsPast,
                                    baseColor = backgroundBaseColor,
                                    activeColor = voiceAccent.copy(alpha = 0.85f),
                                    style = backgroundStyle,
                                    textAlign = TextAlign.Start,
                                    config = config.copy(enableSparkles = false),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }

                        if (showRomanization) {
                            line.romanization?.takeIf(String::isNotBlank)?.let { romanization ->
                                Text(
                                    text = romanization,
                                    style = if (desktopText) {
                                        ExpressiveTypography.bodyMedium
                                    } else {
                                        ExpressiveTypography.bodySmall
                                    },
                                    color = lineColor.copy(
                                        alpha = if (readableMode) 1f else if (isActive) 0.75f else 0.45f
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }

                        if (showTranslations) {
                            line.translations.forEach { translation ->
                                Text(
                                    text = translation.text,
                                    style = if (desktopText) {
                                        ExpressiveTypography.bodyMedium
                                    } else {
                                        ExpressiveTypography.bodySmall
                                    },
                                    color = lineColor.copy(
                                        alpha = if (readableMode) 1f else if (isActive) 0.75f else 0.45f
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showResumeButton,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Surface(
                    onClick = {
                        isFollowingPlayback = true
                        isUserInteracting = false
                        showResumeButton = false
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Play,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sync to playback",
                            style = ExpressiveTypography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            lyrics.attribution?.takeIf(String::isNotBlank)?.let { attribution ->
                Text(
                    text = attribution,
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
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
