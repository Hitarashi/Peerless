package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.*

@Composable
fun NowPlayingSheet(
    track: TrackSummaryDto,
    playbackInfo: PlaybackInfo?,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = 0L,
    artworkUrl: String,
    serverUrl: String,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    isShuffle: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleRepeat: () -> Unit = {},
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val favoritesManager = org.shilpo.peerless.library.LocalFavoritesManager.current
    val favoriteIds by (favoritesManager?.favoriteIds
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet()) }).collectAsState()
    val isFav = isFavorite ?: favoriteIds.contains(track.id)

    val isPlaying = status == PlaybackStatus.PLAYING

    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }

    val (smoothProgressFraction, displayedPosition) = rememberSmoothProgress(
        isPlayingProvider = { isPlaying },
        currentPositionProvider = { scrubPositionMs ?: positionMs },
        totalDuration = durationMs.coerceAtLeast(0L),
        isVisible = true
    )

    val currentPosition = scrubPositionMs ?: displayedPosition.value
    val elapsedText = formatDuration((currentPosition / 1000).toInt())
    val remainingMs = (durationMs - currentPosition).coerceAtLeast(0L)
    val remainingText = "-${formatDuration((remainingMs / 1000).toInt())}"

    val playButtonInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playButtonInteractionSource.collectIsPressedAsState()
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        label = "HeroPlayScale"
    )

    val currentOnClose by rememberUpdatedState(onClose)
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 60.dp.toPx() }
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }
    var hasTriggeredClose by remember { mutableStateOf(false) }
    var showAudioDetails by remember { mutableStateOf(false) }

    if (showAudioDetails) {
        val detailTrack = track.copy(
            codec = playbackInfo?.codec ?: track.codec,
            bit_depth = playbackInfo?.bit_depth ?: track.bit_depth,
            sample_rate = playbackInfo?.sample_rate ?: track.sample_rate
        )
        AudioDetailsModal(
            track = detailTrack,
            artworkUrl = artworkUrl,
            onDismiss = { showAudioDetails = false }
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surfaceContainerLowest,
                        colorScheme.surface,
                        colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            cumulativeDragY += delta
                            if (!hasTriggeredClose && cumulativeDragY > dismissThresholdPx) {
                                hasTriggeredClose = true
                                currentOnClose()
                            }
                        },
                        onDragStarted = {
                            cumulativeDragY = 0f
                            hasTriggeredClose = false
                        },
                        onDragStopped = { velocity ->
                            if (!hasTriggeredClose && (cumulativeDragY > dismissThresholdPx || velocity > 600f)) {
                                hasTriggeredClose = true
                                currentOnClose()
                            }
                            cumulativeDragY = 0f
                            hasTriggeredClose = false
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.ExpandMore,
                        contentDescription = "Collapse player",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        style = ExpressiveTypography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = track.provider.replace("_", " ").uppercase(),
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Settings,
                        contentDescription = "Playback settings",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(270.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = HeroArtworkShape,
                        spotColor = colorScheme.primary.copy(alpha = 0.35f),
                        ambientColor = colorScheme.secondary.copy(alpha = 0.25f)
                    )
                    .clip(HeroArtworkShape)
                    .background(colorScheme.surfaceContainerHigh)
                    .border(1.dp, colorScheme.outlineVariant, HeroArtworkShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(72.dp)
                )

                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "${track.title} hero artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.artist,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.album,
                    style = ExpressiveTypography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                PlaybackDeviceIndicator(
                    showLabel = true,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LosslessBadge(
                    bitDepth = playbackInfo?.bit_depth ?: track.bit_depth,
                    sampleRate = playbackInfo?.sample_rate ?: track.sample_rate,
                    codec = playbackInfo?.codec ?: track.codec,
                    provider = track.provider,
                    compact = false,
                    showTierTag = true,
                    onClick = { showAudioDetails = true }
                )

                IconButton(
                    onClick = {
                        if (onToggleFavorite != null) {
                            onToggleFavorite()
                        } else {
                            favoritesManager?.toggleFavorite(track)
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = if (isFav) PeerlessIcons.Heart else PeerlessIcons.HeartBorder,
                        contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                        tint = if (isFav) Color(0xFFFF5252) else colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                val effectiveBufferedMs =
                    if (track.is_cached && durationMs > 0L) durationMs else maxOf(bufferedPositionMs, positionMs)
                val bufferedFraction = if (durationMs > 0L) {
                    (effectiveBufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                WavySliderExpressive(
                    value = { smoothProgressFraction.value },
                    bufferedValue = { bufferedFraction },
                    onValueChange = { fraction ->
                        scrubPositionMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
                    },
                    onValueCommit = { fraction ->
                        val targetMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
                        onSeekTo(targetMs)
                        scrubPositionMs = null
                    },
                    enabled = durationMs > 0L,
                    activeTrackColor = colorScheme.primary,
                    inactiveTrackColor = colorScheme.surfaceContainerHighest,
                    bufferedTrackColor = colorScheme.onSurface.copy(alpha = 0.42f),
                    thumbColor = colorScheme.primary,
                    isPlaying = isPlaying,
                    isVisible = true,
                    strokeWidth = 4.dp,
                    thumbRadius = 6.dp,
                    idleGap = 3.5.dp,
                    thumbLineHeightWhenInteracting = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = elapsedText,
                        style = SpecBadgeTypography,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = remainingText,
                        style = SpecBadgeTypography,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .scale(playButtonScale)
                        .size(68.dp)
                        .clip(SquircleShapeMedium)
                        .background(colorScheme.primary)
                        .clickable(
                            interactionSource = playButtonInteractionSource,
                            indication = ripple(),
                            onClick = onTogglePlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlayPauseMorphIcon(
                        isPlaying = isPlaying,
                        tint = colorScheme.onPrimary,
                        size = 36.dp
                    )
                }

                IconButton(
                    onClick = onPlayNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.SkipNext,
                        contentDescription = "Next Track",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(44.dp)
                ) {
                    val repeatIcon = if (repeatMode == RepeatMode.ONE) PeerlessIcons.RepeatOne else PeerlessIcons.Repeat
                    val repeatTint =
                        if (repeatMode != RepeatMode.OFF) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f
                        )
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colorScheme.surfaceContainer)
                    .border(1.dp, colorScheme.outlineVariant, PillShape)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colorScheme.secondary)
                )

                Text(
                    text = "Authenticated Lossless Stream",
                    style = ExpressiveTypography.labelSmall,
                    color = colorScheme.onSurface
                )

                Text(
                    text = "•  $serverUrl",
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
