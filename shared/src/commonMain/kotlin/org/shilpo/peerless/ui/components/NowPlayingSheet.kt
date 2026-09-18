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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.PlaybackInfo
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
    artworkUrl: String,
    serverUrl: String,
    isDevMode: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = status == PlaybackStatus.PLAYING
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayFraction = if (isDraggingSlider) sliderDragPosition else currentFraction
    val displayPositionMs = (displayFraction * durationMs).toLong()

    val elapsedText = formatDuration((displayPositionMs / 1000).toInt())
    val remainingMs = (durationMs - displayPositionMs).coerceAtLeast(0L)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceContainerLowestDark,
                        BackgroundDark,
                        SurfaceDark
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
                        .background(SurfaceContainerDark)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.ExpandMore,
                        contentDescription = "Collapse player",
                        tint = OnSurfaceDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        style = ExpressiveTypography.labelSmall,
                        color = OnSurfaceVariantDark.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = track.provider.replace("_", " ").uppercase(),
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerDark)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Settings,
                        contentDescription = "Playback settings",
                        tint = if (isDevMode) LosslessGold else OnSurfaceDark,
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
                        spotColor = PrimaryDark.copy(alpha = 0.35f),
                        ambientColor = SecondaryDark.copy(alpha = 0.25f)
                    )
                    .clip(HeroArtworkShape)
                    .background(SurfaceContainerHighDark)
                    .border(1.dp, OutlineVariantDark, HeroArtworkShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.MusicNote,
                    contentDescription = null,
                    tint = OnSurfaceVariantDark.copy(alpha = 0.3f),
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
                    color = OnSurfaceDark,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.artist,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.album,
                    style = ExpressiveTypography.bodySmall,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LosslessBadge(
                bitDepth = playbackInfo?.bit_depth ?: track.bit_depth,
                sampleRate = playbackInfo?.sample_rate ?: track.sample_rate,
                codec = playbackInfo?.codec ?: track.codec,
                compact = false,
                showTierTag = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = displayFraction,
                    onValueChange = { frac ->
                        isDraggingSlider = true
                        sliderDragPosition = frac
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo((sliderDragPosition * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryDark,
                        activeTrackColor = PrimaryDark,
                        inactiveTrackColor = SurfaceContainerHighestDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = elapsedText,
                        style = SpecBadgeTypography,
                        color = OnSurfaceVariantDark
                    )
                    Text(
                        text = remainingText,
                        style = SpecBadgeTypography,
                        color = OnSurfaceVariantDark
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
                    onClick = { isShuffle = !isShuffle },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) SecondaryDark else OnSurfaceVariantDark.copy(alpha = 0.6f),
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
                        tint = OnSurfaceDark,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .scale(playButtonScale)
                        .size(68.dp)
                        .clip(SquircleShapeMedium)
                        .background(PrimaryDark)
                        .clickable(
                            interactionSource = playButtonInteractionSource,
                            indication = ripple(),
                            onClick = onTogglePlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlayPauseMorphIcon(
                        isPlaying = isPlaying,
                        tint = OnPrimaryDark,
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
                        tint = OnSurfaceDark,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { isRepeat = !isRepeat },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) SecondaryDark else OnSurfaceVariantDark.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(SurfaceContainerDark)
                    .border(1.dp, OutlineVariantDark, PillShape)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isDevMode) LosslessGold else SecondaryDark)
                )

                Text(
                    text = if (isDevMode) "Dev Mode Direct Stream" else "Authenticated Stream",
                    style = ExpressiveTypography.labelSmall,
                    color = OnSurfaceDark
                )

                Text(
                    text = "•  $serverUrl",
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = OnSurfaceVariantDark
                )
            }
        }
    }
}
