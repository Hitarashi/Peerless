package org.shilpo.peerless.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.lyrics.LyricsLoadState
import org.shilpo.peerless.lyrics.LyricsLoader
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.AudioSpectrumFrame
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.preferences.LocalAppPreferences
import org.shilpo.peerless.preferences.LyricsPresentation
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.HeroArtworkShape
import org.shilpo.peerless.theme.MiniPlayerGlowPalette
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeMedium
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.theme.rememberMiniPlayerGlowPalette

internal enum class LyricsSpectrumStyle {
    Curve,
    Bar
}

internal enum class LyricsSpectrumPlacement {
    Top,
    Bottom
}

@Composable
fun NowPlayingSheet(
    track: TrackSummaryDto,
    lyricsLoader: LyricsLoader,
    playbackInfo: PlaybackInfo?,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = 0L,
    outputLatencyMs: Long = 0L,
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
    queueTracks: List<TrackSummaryDto> = emptyList(),
    currentQueueIndex: Int = -1,
    onPlayQueueItem: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    spectrumFrame: AudioSpectrumFrame? = null
) {
    val favoritesManager = org.shilpo.peerless.library.LocalFavoritesManager.current
    val appPreferences = LocalAppPreferences.current
    val lyricsPresentation by appPreferences.lyricsPresentation.collectAsState()
    val favoriteIds by (favoritesManager?.favoriteIds
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet()) }).collectAsState()
    val isFav = isFavorite ?: favoriteIds.contains(track.id)

    val isPlaying = status == PlaybackStatus.PLAYING
    val lyricsState by lyricsLoader.state.collectAsState()
    var isLyricsView by remember { mutableStateOf(false) }
    var isQueueView by remember { mutableStateOf(false) }
    var showSpectrum by remember { mutableStateOf(true) }
    var spectrumStyle by remember { mutableStateOf(LyricsSpectrumStyle.Curve) }
    var spectrumPlacement by remember { mutableStateOf(LyricsSpectrumPlacement.Bottom) }
    var spectrumGlowEnabled by remember { mutableStateOf(true) }
    var showLyricsTranslations by remember(track.id) { mutableStateOf(true) }
    var showLyricsRomanization by remember(track.id) { mutableStateOf(true) }

    LaunchedEffect(track.id, isLyricsView, serverUrl) {
        if (isLyricsView) lyricsLoader.loadIfNeeded(track, serverUrl)
    }

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
    val artworkPalette = rememberMiniPlayerGlowPalette(
        rememberArtworkSeedColor(artworkUrl, colorScheme.primary)
    )

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Box(
            modifier = Modifier
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
        )

        if (isLyricsView) {
            LyricsPlayerLayout(
                track = track,
                artworkUrl = artworkUrl,
                lyricsState = lyricsState,
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                outputLatencyMs = outputLatencyMs,
                progressFraction = { smoothProgressFraction.value },
                elapsedText = elapsedText,
                remainingText = remainingText,
                isPlaying = isPlaying,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                onProgressChange = { fraction ->
                    scrubPositionMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
                },
                onProgressCommit = { fraction ->
                    val targetMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
                    onSeekTo(targetMs)
                    scrubPositionMs = null
                },
                onSeekTo = onSeekTo,
                onTogglePlayPause = onTogglePlayPause,
                onPlayPrevious = onPlayPrevious,
                onPlayNext = onPlayNext,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onClose = onClose,
                onOpenSettings = onOpenSettings,
                onReturnToPlayer = { isLyricsView = false },
                onRetry = { lyricsLoader.retry(track, serverUrl) },
                artworkPalette = artworkPalette,
                spectrumFrame = spectrumFrame,
                showSpectrum = showSpectrum,
                onToggleSpectrum = { showSpectrum = !showSpectrum },
                spectrumStyle = spectrumStyle,
                onSelectSpectrumStyle = { spectrumStyle = it },
                spectrumPlacement = spectrumPlacement,
                onSelectSpectrumPlacement = { spectrumPlacement = it },
                spectrumGlowEnabled = spectrumGlowEnabled,
                onToggleSpectrumGlow = { spectrumGlowEnabled = !spectrumGlowEnabled },
                showTranslations = showLyricsTranslations,
                showRomanization = showLyricsRomanization,
                lyricsPresentation = lyricsPresentation,
                onToggleLyricsPresentation = {
                    appPreferences.setLyricsPresentation(
                        if (lyricsPresentation == LyricsPresentation.VISUAL) {
                            LyricsPresentation.READABLE
                        } else {
                            LyricsPresentation.VISUAL
                        }
                    )
                },
                onToggleTranslations = { showLyricsTranslations = !showLyricsTranslations },
                onToggleRomanization = { showLyricsRomanization = !showLyricsRomanization },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        if (isQueueView) {
            FullPlayerQueueView(
                queueTracks = queueTracks,
                currentQueueIndex = currentQueueIndex,
                status = status,
                onPlayQueueItem = { index ->
                    onPlayQueueItem(index)
                    isQueueView = false
                },
                onBack = { isQueueView = false }
            )
        }

        AnimatedVisibility(
            visible = !isLyricsView && !isQueueView,
            modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.ExpandMore,
                                contentDescription = "Collapse player",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                        onClick = { isLyricsView = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Lyrics,
                                contentDescription = "Show synced lyrics",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isQueueView = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Queue,
                                contentDescription = "Show playback queue",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Settings,
                                contentDescription = "Playback settings",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                    PeerlessIcon(
                        icon = PeerlessIcons.MusicNote,
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
                        modifier = Modifier.heightIn(min = 48.dp).wrapContentWidth()
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
                            .size(48.dp)
                            .padding(7.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainer)
                    ) {
                        PeerlessIcon(
                            icon = if (isFav) PeerlessIcons.Heart else PeerlessIcons.HeartBorder,
                            contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                            tint = if (isFav) Color(0xFFFF5252) else colorScheme.onSurfaceVariant.copy(
                                alpha = 0.75f
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    val effectiveBufferedMs =
                        if (track.is_cached && durationMs > 0L) durationMs else maxOf(
                            bufferedPositionMs,
                            positionMs
                        )
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
                            .height(48.dp)
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    SkipTrackMorphButton(
                        isNext = false,
                        onClick = onPlayPrevious,
                        modifier = Modifier.size(52.dp),
                        iconSize = 30.dp,
                        tint = colorScheme.onSurface,
                        contentDescription = "Previous Track",
                    )

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

                    SkipTrackMorphButton(
                        isNext = true,
                        onClick = onPlayNext,
                        modifier = Modifier.size(52.dp),
                        iconSize = 30.dp,
                        tint = colorScheme.onSurface,
                        contentDescription = "Next Track",
                    )

                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        val repeatIcon =
                            if (repeatMode == RepeatMode.ONE) PeerlessIcons.RepeatOne else PeerlessIcons.Repeat
                        val repeatTint =
                            if (repeatMode != RepeatMode.OFF) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            )
                        PeerlessIcon(
                            icon = repeatIcon,
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
                        .heightIn(min = 48.dp)
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
}

@Composable
private fun FullPlayerQueueView(
    queueTracks: List<TrackSummaryDto>,
    currentQueueIndex: Int,
    status: PlaybackStatus,
    onPlayQueueItem: (Int) -> Unit,
    onBack: () -> Unit
) {
    val apiClient = org.shilpo.peerless.network.LocalPeerlessApiClient.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                PeerlessIcon(
                    icon = PeerlessIcons.ExpandMore,
                    contentDescription = "Return to player",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Queue",
                style = ExpressiveTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (queueTracks.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    style = ExpressiveTypography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = queueTracks,
                    key = { index, track -> "full_player_queue_${track.id}_$index" }
                ) { index, track ->
                    TrackRow(
                        track = track,
                        artworkUrl = apiClient.getArtworkUrl(track, 120),
                        isPlaying = currentQueueIndex == index && status == PlaybackStatus.PLAYING,
                        isCurrent = currentQueueIndex == index,
                        onTrackClick = { onPlayQueueItem(index) },
                        showArtworkOverlay = false
                    )
                }
            }
        }
    }
}

@Composable
internal fun LyricsArtworkGlowBackground(
    artworkUrl: String,
    palette: MiniPlayerGlowPalette,
    spectrumFrame: AudioSpectrumFrame?,
    showSpectrum: Boolean,
    spectrumStyle: LyricsSpectrumStyle = LyricsSpectrumStyle.Curve,
    spectrumPlacement: LyricsSpectrumPlacement = LyricsSpectrumPlacement.Bottom,
    spectrumGlowEnabled: Boolean = true,
    showPaletteTint: Boolean = true,
    isPlaying: Boolean
) {
    val spectrumBassEnergy = if (showSpectrum && isPlaying) spectrumFrame?.bassEnergy ?: 0f else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                }
                .blur(48.dp)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showPaletteTint) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.first.copy(alpha = 0.28f),
                            palette.second.copy(alpha = 0.22f),
                            palette.third.copy(alpha = 0.20f),
                            palette.fourth.copy(alpha = 0.18f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.57f),
                        Color.Black.copy(alpha = 0.68f),
                        Color.Black.copy(alpha = 0.80f)
                    )
                )
            )
            if (showSpectrum) {
                val bands = spectrumFrame?.bands.orEmpty()
                if (bands.size > 1) {
                    val isBottom = spectrumPlacement == LyricsSpectrumPlacement.Bottom
                    val baseline = if (isBottom) size.height else 0f
                    val amplitude = size.height * 0.34f
                    val displayAlpha = if (isPlaying) 1f else 0.56f
                    if (spectrumStyle == LyricsSpectrumStyle.Bar) {
                        val bandWidth = size.width / bands.size
                        val barWidth = (bandWidth * 0.72f).coerceAtLeast(1f)
                        bands.forEachIndexed { index, value ->
                            val height = value.coerceIn(0f, 1f) * amplitude
                            if (height > 0.5f) {
                                val left = index * bandWidth + (bandWidth - barWidth) / 2f
                                val top = if (isBottom) baseline - height else baseline
                                if (spectrumGlowEnabled) {
                                    drawRect(
                                        color = palette.first.copy(alpha = (0.14f + spectrumBassEnergy * 0.18f) * displayAlpha),
                                        topLeft = Offset(left - 3.dp.toPx(), top),
                                        size = androidx.compose.ui.geometry.Size(
                                            barWidth + 6.dp.toPx(),
                                            height + 3.dp.toPx()
                                        )
                                    )
                                }
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            palette.first,
                                            palette.second,
                                            palette.third
                                        )
                                    ),
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                                    alpha = displayAlpha
                                )
                            }
                        }
                    } else {
                        val line = Path()
                        val filled = Path()
                        val step = size.width / (bands.size - 1)
                        val points = bands.mapIndexed { index, value ->
                            val offset = value.coerceIn(0f, 1f) * amplitude
                            Offset(
                                index * step,
                                if (isBottom) baseline - offset else baseline + offset
                            )
                        }
                        line.moveTo(points.first().x, points.first().y)
                        for (index in 0 until points.lastIndex) {
                            val previous = points.getOrElse(index - 1) { points[index] }
                            val current = points[index]
                            val next = points[index + 1]
                            val following = points.getOrElse(index + 2) { next }
                            val firstControl = Offset(
                                current.x + (next.x - previous.x) * 0.1667f,
                                current.y + (next.y - previous.y) * 0.1667f
                            )
                            val secondControl = Offset(
                                next.x - (following.x - current.x) * 0.1667f,
                                next.y - (following.y - current.y) * 0.1667f
                            )
                            line.cubicTo(
                                firstControl.x,
                                firstControl.y,
                                secondControl.x,
                                secondControl.y,
                                next.x,
                                next.y
                            )
                        }
                        filled.addPath(line)
                        filled.lineTo(size.width, baseline)
                        filled.lineTo(0f, baseline)
                        filled.close()

                        val fillColors = if (isBottom) {
                            listOf(
                                palette.first.copy(alpha = 0.02f),
                                palette.second.copy(alpha = 0.16f),
                                palette.third.copy(alpha = 0.32f)
                            )
                        } else {
                            listOf(
                                palette.third.copy(alpha = 0.32f),
                                palette.second.copy(alpha = 0.16f),
                                palette.first.copy(alpha = 0.02f)
                            )
                        }
                        drawPath(
                            path = filled,
                            brush = Brush.verticalGradient(
                                colors = fillColors,
                                startY = if (isBottom) baseline - amplitude else baseline,
                                endY = if (isBottom) baseline else baseline + amplitude
                            ),
                            alpha = displayAlpha
                        )
                        if (spectrumGlowEnabled) {
                            drawPath(
                                path = line,
                                color = palette.first.copy(alpha = (0.30f + spectrumBassEnergy * 0.18f) * displayAlpha),
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        drawPath(
                            path = line,
                            color = Color.White.copy(alpha = 0.54f * displayAlpha),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPlayerLayout(
    track: TrackSummaryDto,
    artworkUrl: String,
    lyricsState: LyricsLoadState,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    outputLatencyMs: Long = 0L,
    progressFraction: () -> Float,
    elapsedText: String,
    remainingText: String,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onProgressChange: (Float) -> Unit,
    onProgressCommit: (Float) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onReturnToPlayer: () -> Unit,
    onRetry: () -> Unit,
    artworkPalette: MiniPlayerGlowPalette,
    spectrumFrame: AudioSpectrumFrame?,
    showSpectrum: Boolean,
    onToggleSpectrum: () -> Unit,
    spectrumStyle: LyricsSpectrumStyle,
    onSelectSpectrumStyle: (LyricsSpectrumStyle) -> Unit,
    spectrumPlacement: LyricsSpectrumPlacement,
    onSelectSpectrumPlacement: (LyricsSpectrumPlacement) -> Unit,
    spectrumGlowEnabled: Boolean,
    onToggleSpectrumGlow: () -> Unit,
    showTranslations: Boolean,
    showRomanization: Boolean,
    lyricsPresentation: LyricsPresentation,
    onToggleLyricsPresentation: () -> Unit,
    onToggleTranslations: () -> Unit,
    onToggleRomanization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val lyricAccentColors = remember(artworkPalette) {
        listOf(
            artworkPalette.first,
            artworkPalette.second,
            artworkPalette.third,
            artworkPalette.fourth
        )
    }
    val playButtonInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playButtonInteractionSource.collectIsPressedAsState()
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        label = "LyricsPlayScale"
    )
    var spectrumOptionsExpanded by remember { mutableStateOf(false) }
    var manualNudgeMs by remember(track.id) { mutableStateOf(0L) }
    var showSyncTuner by remember(track.id) { mutableStateOf(false) }
    val effectiveOffset = (if (outputLatencyMs != 0L) outputLatencyMs else -750L) + manualNudgeMs
    val effectiveBufferedMs =
        if (track.is_cached && durationMs > 0L) durationMs else maxOf(
            bufferedPositionMs,
            positionMs
        )
    val bufferedFraction = if (durationMs > 0L) {
        (effectiveBufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.24f))
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.ExpandMore,
                    contentDescription = "Collapse player",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SYNCED LYRICS",
                    style = ExpressiveTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = track.provider.replace("_", " ").uppercase(),
                    style = ExpressiveTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (spectrumFrame != null) {
                    Box {
                        IconButton(
                            onClick = { spectrumOptionsExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.24f))
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.LosslessWave,
                                contentDescription = "Spectrum options",
                                tint = if (showSpectrum) colorScheme.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = spectrumOptionsExpanded,
                            onDismissRequest = { spectrumOptionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (showSpectrum) "Hide live spectrum" else "Show live spectrum") },
                                onClick = {
                                    onToggleSpectrum()
                                    spectrumOptionsExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Style: ${spectrumStyle.name.lowercase()}") },
                                onClick = {
                                    onSelectSpectrumStyle(
                                        if (spectrumStyle == LyricsSpectrumStyle.Curve) {
                                            LyricsSpectrumStyle.Bar
                                        } else {
                                            LyricsSpectrumStyle.Curve
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Position: ${spectrumPlacement.name.lowercase()}") },
                                onClick = {
                                    onSelectSpectrumPlacement(
                                        if (spectrumPlacement == LyricsSpectrumPlacement.Bottom) {
                                            LyricsSpectrumPlacement.Top
                                        } else {
                                            LyricsSpectrumPlacement.Bottom
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Glow: ${if (spectrumGlowEnabled) "on" else "off"}") },
                                onClick = onToggleSpectrumGlow
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onReturnToPlayer,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.24f))
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.MusicNote,
                        contentDescription = "Return to player",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.24f))
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.Settings,
                        contentDescription = "Playback settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = "${track.title} artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(HeroArtworkShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = ExpressiveTypography.bodyMedium,
                    color = Color.White.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            LyricsPresentationControl(
                presentation = lyricsPresentation,
                onToggle = onToggleLyricsPresentation
            )
        }

        val availableLyrics = (lyricsState as? LyricsLoadState.Available)?.lyrics
        val hasTranslations = availableLyrics?.lines?.any { line ->
            line.translations.any { it.text.isNotBlank() }
        } == true
        val hasRomanization =
            availableLyrics?.lines?.any { !it.romanization.isNullOrBlank() } == true
        val isSynced = availableLyrics != null &&
                !availableLyrics.format.equals("plain", ignoreCase = true) &&
                availableLyrics.lines.any { it.end_ms > it.start_ms || it.is_instrumental }

        if (hasTranslations || hasRomanization || isSynced) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasTranslations) {
                    FilterChip(
                        selected = showTranslations,
                        onClick = onToggleTranslations,
                        label = { Text("Translation") }
                    )
                }
                if (hasRomanization) {
                    FilterChip(
                        selected = showRomanization,
                        onClick = onToggleRomanization,
                        label = { Text("Romanization") }
                    )
                }
                if (isSynced) {
                    FilterChip(
                        selected = showSyncTuner,
                        onClick = { showSyncTuner = !showSyncTuner },
                        label = {
                            Text(
                                if (manualNudgeMs == 0L) "Sync ${effectiveOffset}ms"
                                else "Sync ${effectiveOffset}ms (${if (manualNudgeMs > 0) "+$manualNudgeMs" else "$manualNudgeMs"})"
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(visible = showSyncTuner && isSynced) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { manualNudgeMs -= 50L },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            "-50",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Offset: ${effectiveOffset}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { manualNudgeMs += 50L },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            "+50",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (manualNudgeMs != 0L) {
                        TextButton(
                            onClick = { manualNudgeMs = 0L },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 2.dp
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        SyncedLyricsContent(
            state = lyricsState,
            positionMs = positionMs,
            onSeekTo = onSeekTo,
            onRetry = onRetry,
            showTranslations = showTranslations,
            showRomanization = showRomanization,
            accentColors = lyricAccentColors,
            animationOptions = lyricsPresentation.animationOptions(),
            readableMode = lyricsPresentation == LyricsPresentation.READABLE,
            config = lyricsPresentation.visualConfig(effectiveOffset),
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            WavySliderExpressive(
                value = progressFraction,
                bufferedValue = { bufferedFraction },
                onValueChange = onProgressChange,
                onValueCommit = onProgressCommit,
                enabled = durationMs > 0L,
                activeTrackColor = colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                bufferedTrackColor = Color.White.copy(alpha = 0.46f),
                thumbColor = colorScheme.primary,
                isPlaying = isPlaying,
                isVisible = true,
                strokeWidth = 4.dp,
                thumbRadius = 6.dp,
                idleGap = 3.5.dp,
                thumbLineHeightWhenInteracting = 22.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = elapsedText,
                    style = SpecBadgeTypography,
                    color = Color.White.copy(alpha = 0.76f)
                )
                Text(
                    text = remainingText,
                    style = SpecBadgeTypography,
                    color = Color.White.copy(alpha = 0.76f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
                PeerlessIcon(
                    icon = PeerlessIcons.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) colorScheme.secondary else Color.White.copy(alpha = 0.64f),
                    modifier = Modifier.size(22.dp)
                )
            }
            SkipTrackMorphButton(
                isNext = false,
                onClick = onPlayPrevious,
                modifier = Modifier.size(52.dp),
                iconSize = 30.dp,
                tint = Color.White,
                contentDescription = "Previous track",
            )
            Box(
                modifier = Modifier
                    .scale(playButtonScale)
                    .size(64.dp)
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
                    size = 34.dp
                )
            }
            SkipTrackMorphButton(
                isNext = true,
                onClick = onPlayNext,
                modifier = Modifier.size(52.dp),
                iconSize = 30.dp,
                tint = Color.White,
                contentDescription = "Next track",
            )
            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(48.dp)) {
                val repeatIcon =
                    if (repeatMode == RepeatMode.ONE) PeerlessIcons.RepeatOne else PeerlessIcons.Repeat
                PeerlessIcon(
                    icon = repeatIcon,
                    contentDescription = if (repeatMode == RepeatMode.OFF) "Repeat off" else "Repeat",
                    tint = if (repeatMode == RepeatMode.OFF) Color.White.copy(alpha = 0.64f) else colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
