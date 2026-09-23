@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.LocalLiquidGlassState
import org.shilpo.peerless.theme.createSoftwareArtworkRequest
import org.shilpo.peerless.theme.extractArtworkSeedColor
import org.shilpo.peerless.theme.liquidGlass
import org.shilpo.peerless.theme.rememberLiquidGlassTint
import org.shilpo.peerless.theme.rememberMiniPlayerGlowPalette
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayerBar(
    track: TrackSummaryDto,
    playbackInfo: PlaybackInfo?,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    artworkUrl: String,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit = {},
    onOpenNowPlaying: () -> Unit,
    onDismiss: () -> Unit = {},
    canSkipNext: Boolean = true,
    canSkipPrevious: Boolean = true,
    modifier: Modifier = Modifier,
    isPairedWithNavigation: Boolean = true,
    pureBlack: Boolean = false
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isPlaying = status == PlaybackStatus.PLAYING
    val isBuffering = status == PlaybackStatus.BUFFERING

    val fallbackArtworkColor = MaterialTheme.colorScheme.primary
    var extractedArtworkColor by remember(
        artworkUrl,
        fallbackArtworkColor
    ) { mutableStateOf<Color?>(null) }
    val seedColor = extractedArtworkColor ?: fallbackArtworkColor
    val glowPalette = rememberMiniPlayerGlowPalette(seedColor)
    val imageContext = LocalPlatformContext.current
    val artworkRequest = remember(artworkUrl, imageContext) {
        createSoftwareArtworkRequest(imageContext, artworkUrl, size = 200)
    }
    val motionScheme = MaterialTheme.motionScheme

    val shouldShrink = isPlaying || isBuffering
    val artworkSizeDp = remember { Animatable(if (shouldShrink) 36f else 42f) }

    LaunchedEffect(shouldShrink) {
        if (shouldShrink) {
            artworkSizeDp.animateTo(
                targetValue = 36f,
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        } else {
            delay(335.milliseconds)
            artworkSizeDp.animateTo(
                targetValue = 42f,
                animationSpec = motionScheme.defaultSpatialSpec()
            )
        }
    }
    val artworkSize = artworkSizeDp.value.dp

    val miniPlayerShape = remember(isPairedWithNavigation) {
        if (isPairedWithNavigation) {
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            )
        } else {
            RoundedCornerShape(32.dp)
        }
    }

    // Glass replaces the painted glow only when enabled and not in pure-black
    // mode; otherwise the current drawWithCache fallback stays untouched.
    val liquidGlassEnabled = LocalLiquidGlassState.current?.isEnabled == true
    val useGlass = liquidGlassEnabled && !pureBlack
    val glassTint = rememberLiquidGlassTint(artworkColor = glowPalette.first)

    val coroutineScope = rememberCoroutineScope()
    val offsetXAnimatable = remember { Animatable(0f) }
    val offsetYAnimatable = remember { Animatable(0f) }

    val currentCanSkipPrevious by rememberUpdatedState(canSkipPrevious)
    val currentCanSkipNext by rememberUpdatedState(canSkipNext)
    val currentOnPlayPrevious by rememberUpdatedState(onPlayPrevious)
    val currentOnPlayNext by rememberUpdatedState(onPlayNext)
    val currentOnOpenNowPlaying by rememberUpdatedState(onOpenNowPlaying)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val density = LocalDensity.current
    val horizontalCueThresholdPx = with(density) { 60.dp.toPx() }
    val horizontalSkipThresholdPx = with(density) { 50.dp.toPx() }
    val flingDistance20Px = with(density) { 20.dp.toPx() }
    val flingDistance15Px = with(density) { 15.dp.toPx() }
    val verticalOpenThresholdPx = with(density) { -40.dp.toPx() }
    val verticalDismissThresholdPx = with(density) { 50.dp.toPx() }
    val verticalFadeThresholdPx = with(density) { 80.dp.toPx() }

    val swipeSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    var isHorizontalDragging by remember { mutableStateOf(false) }
    var isVerticalDragging by remember { mutableStateOf(false) }
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }

    val horizontalDraggableState = rememberDraggableState { delta ->
        val currentX = offsetXAnimatable.value
        val effectiveDeltaX = if ((currentX > 0 || delta > 0) && !currentCanSkipNext && delta > 0) {
            delta * 0.15f
        } else if ((currentX < 0 || delta < 0) && !currentCanSkipPrevious && delta < 0) {
            delta * 0.15f
        } else {
            delta
        }
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            offsetXAnimatable.snapTo(currentX + effectiveDeltaX)
        }
    }

    val verticalDraggableState = rememberDraggableState { delta ->
        cumulativeDragY += delta
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (cumulativeDragY < 0f) {
                offsetYAnimatable.snapTo(cumulativeDragY * 0.3f)
            } else {
                offsetYAnimatable.snapTo(cumulativeDragY)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight),
        contentAlignment = Alignment.Center
    ) {
        val currentOffsetX = offsetXAnimatable.value
        val cueColor = glowPalette.first

        if (currentOffsetX > 0f) {
            val nextAlpha = (currentOffsetX / horizontalCueThresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .graphicsLayer {
                        alpha = nextAlpha
                        val scale = 0.8f + (0.2f * nextAlpha)
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f * nextAlpha)
                    )
                    .border(
                        width = 1.dp,
                        color = cueColor.copy(alpha = 0.45f * nextAlpha),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.SkipNext,
                    contentDescription = "Next Track",
                    tint = cueColor.copy(alpha = nextAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (currentOffsetX < 0f) {
            val prevAlpha = (-currentOffsetX / horizontalCueThresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(44.dp)
                    .graphicsLayer {
                        alpha = prevAlpha
                        val scale = 0.8f + (0.2f * prevAlpha)
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f * prevAlpha)
                    )
                    .border(
                        width = 1.dp,
                        color = cueColor.copy(alpha = 0.45f * prevAlpha),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = cueColor.copy(alpha = prevAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        val currentOffsetY = offsetYAnimatable.value
        val cardAlpha = (1f - (currentOffsetY / verticalFadeThresholdPx)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        offsetXAnimatable.value.roundToInt(),
                        offsetYAnimatable.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    alpha = cardAlpha
                }
                .shadow(
                    elevation = if (useGlass) 10.dp else 4.dp,
                    shape = miniPlayerShape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(miniPlayerShape)
                .draggable(
                    state = horizontalDraggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !isVerticalDragging,
                    onDragStarted = {
                        isHorizontalDragging = true
                        offsetXAnimatable.stop()
                    },
                    onDragStopped = { velocity ->
                        try {
                            val finalX = offsetXAnimatable.value
                            if (finalX > horizontalSkipThresholdPx || (finalX > flingDistance20Px && velocity > 500f)) {
                                if (currentCanSkipNext) {
                                    currentOnPlayNext()
                                }
                            } else if (finalX < -horizontalSkipThresholdPx || (finalX < -flingDistance20Px && velocity < -500f)) {
                                if (currentCanSkipPrevious) {
                                    currentOnPlayPrevious()
                                }
                            }
                            offsetXAnimatable.animateTo(0f, animationSpec = swipeSpring)
                        } finally {
                            isHorizontalDragging = false
                        }
                    }
                )
                .draggable(
                    state = verticalDraggableState,
                    orientation = Orientation.Vertical,
                    enabled = !isHorizontalDragging,
                    onDragStarted = {
                        isVerticalDragging = true
                        cumulativeDragY = 0f
                        offsetYAnimatable.stop()
                    },
                    onDragStopped = { velocity ->
                        try {
                            val currentY = offsetYAnimatable.value
                            if (cumulativeDragY < verticalOpenThresholdPx || (cumulativeDragY < -flingDistance15Px && velocity < -500f)) {
                                currentOnOpenNowPlaying()
                            } else if (currentY > verticalDismissThresholdPx || (currentY > flingDistance20Px && velocity > 500f)) {
                                currentOnDismiss()
                            }
                            offsetYAnimatable.animateTo(0f, animationSpec = swipeSpring)
                        } finally {
                            isVerticalDragging = false
                            cumulativeDragY = 0f
                        }
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenNowPlaying
                )
                .then(
                    if (useGlass) {
                        Modifier.liquidGlass(shape = miniPlayerShape, tint = glassTint)
                    } else {
                        Modifier.drawWithCache {
                            val width = size.width
                            val height = size.height

                            if (pureBlack) {
                                onDrawBehind {
                                    drawRect(Color.Black)
                                }
                            } else {
                                val verticalGradient = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to glowPalette.first.copy(alpha = 0.95f),
                                        0.52f to glowPalette.second.copy(alpha = 0.82f),
                                        1f to glowPalette.third.copy(alpha = 0.72f)
                                    )
                                )
                                val startGlow = Brush.radialGradient(
                                    colors = listOf(
                                        glowPalette.first.copy(alpha = 0.50f),
                                        glowPalette.first.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.12f, height * 0.42f),
                                    radius = width * 0.72f,
                                )
                                val endGlow = Brush.radialGradient(
                                    colors = listOf(
                                        glowPalette.second.copy(alpha = 0.45f),
                                        glowPalette.second.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.88f, height * 0.58f),
                                    radius = width * 0.72f,
                                )
                                val topGlow = Brush.radialGradient(
                                    colors = listOf(
                                        glowPalette.third.copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.52f, height * 0.05f),
                                    radius = width * 0.54f,
                                )
                                val bottomGlow = Brush.radialGradient(
                                    colors = listOf(
                                        glowPalette.fourth.copy(alpha = 0.30f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.46f, height * 1.05f),
                                    radius = width * 0.54f,
                                )

                                onDrawBehind {
                                    drawRect(Color.Black)
                                    drawRect(verticalGradient)
                                    drawRect(startGlow)
                                    drawRect(endGlow)
                                    drawRect(topGlow)
                                    drawRect(bottomGlow)
                                    drawRect(Color.Black.copy(alpha = 0.32f))
                                }
                            }
                        }
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp)
                ) {
                    if (isBuffering) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.24f)
                        )
                    } else {
                        CircularWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.24f),
                            amplitude = { p ->
                                if (isPlaying) WavyProgressIndicatorDefaults.indicatorAmplitude(p) else 0f
                            }
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(artworkSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = CircleShape
                            )
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(artworkSize * 0.5f)
                        )
                        AsyncImage(
                            model = artworkRequest,
                            contentDescription = "${track.title} artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { state ->
                                extractedArtworkColor = extractArtworkSeedColor(
                                    painter = state.painter,
                                    fallback = fallbackArtworkColor
                                )
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 2000,
                            repeatDelayMillis = 2000
                        )
                    )

                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 2000,
                            repeatDelayMillis = 2000
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkipTrackMorphButton(
                        isNext = false,
                        onClick = onPlayPrevious,
                        modifier = Modifier.size(48.dp),
                        iconSize = 20.dp,
                        tint = Color.White,
                        contentDescription = "Previous Track",
                        containerColor = Color.Black.copy(alpha = 0.22f),
                    )

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.92f),
                            contentColor = Color.Black
                        )
                    ) {
                        PlayPauseMorphIcon(
                            isPlaying = isPlaying,
                            tint = Color.Black,
                            size = 24.dp
                        )
                    }

                    SkipTrackMorphButton(
                        isNext = true,
                        onClick = onPlayNext,
                        modifier = Modifier.size(48.dp),
                        iconSize = 20.dp,
                        tint = Color.White,
                        contentDescription = "Next Track",
                        containerColor = Color.Black.copy(alpha = 0.22f),
                    )
                }
            }

        }
    }
}
