package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.ActiveRipTask
import org.shilpo.peerless.model.CanonicalTrack
import org.shilpo.peerless.model.Codec
import org.shilpo.peerless.model.RipStage
import org.shilpo.peerless.model.TrackSource
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.tasks.LocalRipCoordinator
import org.shilpo.peerless.theme.ArtworkShape
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography
import kotlin.math.roundToInt

fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds <= 0) return "00:00"
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

fun formatProviderLabel(provider: String): String = when (provider.lowercase().trim()) {
    "apple_music", "applemusic", "apple" -> "Apple"
    "qobuz" -> "Qobuz"
    "telegram" -> "Telegram"
    "tidal" -> "Tidal"
    else -> provider.replace("_", " ").trim().lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerTransition")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar1"
    )

    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar2"
    )

    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar3"
    )

    val heights = listOf(
        if (isPlaying) anim1 else 0.4f,
        if (isPlaying) anim2 else 0.6f,
        if (isPlaying) anim3 else 0.3f
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.take(barCount).forEach { ratio ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction = ratio.coerceIn(0.2f, 1f))
                    .clip(PillShape)
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: TrackSummaryDto,
    artworkUrl: String,
    isPlaying: Boolean,
    onTrackClick: (TrackSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
    canonicalTrack: CanonicalTrack? = null,
    onSelectSource: ((TrackSource) -> Unit)? = null,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    onPlayNext: ((TrackSummaryDto) -> Unit)? = null,
    onAddToQueue: ((TrackSummaryDto) -> Unit)? = null,
    onStartRadio: ((TrackSummaryDto) -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onMoveQueueItemUp: (() -> Unit)? = null,
    onMoveQueueItemDown: (() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: ((TrackSummaryDto) -> Unit)? = null,
    isCurrent: Boolean = isPlaying,
    embedded: Boolean = false,
    showArtworkOverlay: Boolean = !embedded
) {
    val favoritesManager = org.shilpo.peerless.library.LocalFavoritesManager.current
    val favoriteIds by (favoritesManager?.favoriteIds
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet()) }).collectAsState()
    val isFav = isFavorite ?: favoriteIds.contains(track.id)

    val effectiveSources = canonicalTrack?.sources ?: emptyList()
    var selectedSource by remember(canonicalTrack) {
        mutableStateOf(canonicalTrack?.immediatePlaySource() ?: canonicalTrack?.bestSource)
    }
    val activeSource =
        selectedSource ?: canonicalTrack?.immediatePlaySource() ?: canonicalTrack?.bestSource

    val displayCodec = activeSource?.codec?.raw ?: track.codec
    val displayProvider = activeSource?.provider?.displayName ?: track.provider
    val isTrackCached = activeSource?.isCached ?: track.is_cached

    val hasApple =
        effectiveSources.any { it.provider.displayName.contains("apple", ignoreCase = true) } ||
                displayProvider.contains("apple", ignoreCase = true)
    val hasQobuz =
        effectiveSources.any { it.provider.displayName.contains("qobuz", ignoreCase = true) } ||
                displayProvider.contains("qobuz", ignoreCase = true)
    val hasDolby = effectiveSources.any { it.codec == Codec.Ec3 } ||
            activeSource?.codec == Codec.Ec3 ||
            displayCodec.contains("ec-3", ignoreCase = true) ||
            displayCodec.contains("ec3", ignoreCase = true) ||
            displayCodec.contains("atmos", ignoreCase = true)
    val hasHiRes =
        effectiveSources.any { (it.bitDepth ?: 16) >= 24 || (it.sampleRate ?: 44100) >= 88200 } ||
                (activeSource?.bitDepth ?: track.bit_depth ?: 16) >= 24 ||
                (activeSource?.sampleRate ?: track.sample_rate ?: 44100) >= 88200

    var showAudioDetails by remember { mutableStateOf(false) }
    var showTrackMenu by remember { mutableStateOf(false) }

    val ripCoordinator = LocalRipCoordinator.current
    val playerConnection = LocalPlayerConnection.current
    val toastNotifier = LocalToastNotifier.current
    val coroutineScope = rememberCoroutineScope()

    val activeTasks by (ripCoordinator?.activeTasks
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap()) }).collectAsState()
    val activeTask = remember(activeTasks, track.provider, track.track_id) {
        activeTasks.values.firstOrNull {
            it.provider.equals(track.provider, ignoreCase = true) && it.trackId == track.track_id
        }
    }
    var showRipDetailSheet by remember { mutableStateOf(false) }

    if (showRipDetailSheet && activeTask != null) {
        RipTaskDetailSheet(
            task = activeTask,
            artworkUrl = artworkUrl,
            onDismiss = { showRipDetailSheet = false },
            onCancelRip = {
                coroutineScope.launch { ripCoordinator?.cancelRip(activeTask.taskId) }
            }
        )
    }

    if (showAudioDetails) {
        val detailTrack = canonicalTrack?.toSummaryDto(activeSource) ?: track
        AudioDetailsModal(
            track = detailTrack,
            artworkUrl = artworkUrl,
            onDismiss = { showAudioDetails = false },
            onPlayTrack = { onTrackClick(detailTrack) }
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowHovered by rowInteractionSource.collectIsHoveredAsState()

    val rowBg by animateColorAsState(
        targetValue = when {
            embedded -> Color.Transparent
            isCurrent && isRowHovered -> {
                colorScheme.onSecondaryContainer.copy(alpha = 0.08f)
                    .compositeOver(colorScheme.secondaryContainer)
            }

            isCurrent -> colorScheme.secondaryContainer
            isRowHovered -> colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        animationSpec = tween(150)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .combinedClickable(
                interactionSource = rowInteractionSource,
                indication = ripple(),
                onClick = {
                    val clickSummary = canonicalTrack?.toSummaryDto(activeSource) ?: track
                    if (!isTrackCached) {
                        if (ripCoordinator != null) {
                            coroutineScope.launch {
                                ripCoordinator.ripAndPlay(clickSummary, playerConnection)
                            }
                            toastNotifier?.invoke("Ripping ${clickSummary.title}... Playback will begin as soon as upload finishes.")
                        } else {
                            onTrackClick(clickSummary)
                        }
                    } else {
                        onTrackClick(clickSummary)
                    }
                },
                onLongClick = { showAudioDetails = true }
            )
            .semantics(mergeDescendants = true) { }
            .padding(
                horizontal = if (embedded) 8.dp else 10.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val artworkInteractionSource = remember { MutableInteractionSource() }
        val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(ArtworkShape)
                .background(colorScheme.surfaceContainerHighest)
                .hoverable(artworkInteractionSource)
                .pointerHoverIcon(if (isCurrent) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = artworkInteractionSource,
                    indication = null,
                    enabled = isCurrent
                ) {
                    val clickSummary = canonicalTrack?.toSummaryDto(activeSource) ?: track
                    onTrackClick(clickSummary)
                },
            contentAlignment = Alignment.Center
        ) {
            PeerlessIcon(
                icon = PeerlessIcons.MusicNote,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying && showArtworkOverlay && !isArtworkHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizer(
                        isPlaying = true,
                        color = colorScheme.primary,
                        barWidth = 3.dp,
                        maxHeight = 16.dp
                    )
                }
            } else if (isCurrent && isArtworkHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    PlayPauseMorphIcon(
                        isPlaying = isPlaying,
                        size = 20.dp,
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPlaying) colorScheme.primary else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isPlaying) {
                                Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            Text(
                text = "${track.artist} • ${track.album}",
                style = ExpressiveTypography.bodySmall.copy(fontSize = 13.sp),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            var showVersionMenu by remember { mutableStateOf(false) }

            Box {
                Row(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (effectiveSources.size > 1) {
                                    showVersionMenu = true
                                } else {
                                    showAudioDetails = true
                                }
                            },
                            onLongClick = {
                                showAudioDetails = true
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasApple) {
                        PeerlessIcon(
                            icon = PeerlessIcons.AppleLogo,
                            contentDescription = "Apple Music",
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (hasQobuz) {
                        PeerlessIcon(
                            icon = PeerlessIcons.QobuzLogo,
                            contentDescription = "Qobuz",
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.height(11.dp).width(28.dp)
                        )
                    }
                    if (!hasApple && !hasQobuz) {
                        Text(
                            text = formatProviderLabel(displayProvider),
                            style = SpecBadgeTypography.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hasDolby) {
                        PeerlessIcon(
                            icon = PeerlessIcons.DolbyAtmos,
                            contentDescription = "Dolby Atmos",
                            tint = colorScheme.tertiary,
                            modifier = Modifier.height(10.dp).width(15.dp)
                        )
                    }
                    if (hasHiRes) {
                        PeerlessIcon(
                            icon = PeerlessIcons.HiRes,
                            contentDescription = "Hi-Res Audio",
                            tint = colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (effectiveSources.size > 1) {
                    DropdownMenu(
                        expanded = showVersionMenu,
                        onDismissRequest = { showVersionMenu = false },
                        modifier = Modifier
                            .background(colorScheme.surfaceContainerHigh)
                            .border(
                                1.dp,
                                colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = "AVAILABLE VERSIONS",
                            style = SpecBadgeTypography.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                        effectiveSources.forEach { source ->
                            val isSelected = activeSource?.let {
                                it.provider == source.provider &&
                                        it.providerTrackId == source.providerTrackId &&
                                        it.codec == source.codec
                            } == true
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (source.provider.displayName.contains(
                                                "apple",
                                                ignoreCase = true
                                            )
                                        ) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.AppleLogo,
                                                contentDescription = "Apple Music",
                                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else if (source.provider.displayName.contains(
                                                "qobuz",
                                                ignoreCase = true
                                            )
                                        ) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.QobuzLogo,
                                                contentDescription = "Qobuz",
                                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.height(11.dp).width(28.dp)
                                            )
                                        }
                                        Text(
                                            text = formatProviderLabel(source.provider.displayName),
                                            style = ExpressiveTypography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        if (source.codec == Codec.Ec3) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.DolbyAtmos,
                                                contentDescription = "Dolby Atmos",
                                                tint = colorScheme.tertiary,
                                                modifier = Modifier.height(10.dp).width(15.dp)
                                            )
                                            Text(
                                                text = "Dolby Atmos",
                                                style = ExpressiveTypography.bodySmall,
                                                color = colorScheme.tertiary
                                            )
                                        } else {
                                            val isSourceHiRes =
                                                (source.bitDepth ?: 16) >= 24 || (source.sampleRate
                                                    ?: 44100) >= 88200
                                            if (isSourceHiRes) {
                                                PeerlessIcon(
                                                    icon = PeerlessIcons.HiRes,
                                                    contentDescription = "Hi-Res Audio",
                                                    tint = colorScheme.tertiary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            val specLabel = buildString {
                                                if (source.bitDepth != null && source.bitDepth >= 24) append(
                                                    "${source.bitDepth}-bit "
                                                )
                                                if (source.sampleRate != null) {
                                                    val khz =
                                                        if (source.sampleRate % 1000 == 0) "${source.sampleRate / 1000}" else "${source.sampleRate / 1000.0}"
                                                    append("${khz}kHz ")
                                                }
                                                append(source.codec.displayName)
                                            }.trim()
                                            Text(
                                                text = "• $specLabel",
                                                style = ExpressiveTypography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (source.isCached) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.CloudDone,
                                                contentDescription = "Cached",
                                                tint = colorScheme.secondary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showVersionMenu = false
                                    selectedSource = source
                                    if (onSelectSource != null) {
                                        onSelectSource(source)
                                    } else {
                                        val sourceSummary =
                                            canonicalTrack?.toSummaryDto(source) ?: track
                                        onTrackClick(sourceSummary)
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.CheckCircle,
                                            contentDescription = "Active",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            IconButton(
                onClick = {
                    val favTrack = canonicalTrack?.toSummaryDto(activeSource) ?: track
                    if (onToggleFavorite != null) {
                        onToggleFavorite(favTrack)
                    } else {
                        favoritesManager?.toggleFavorite(favTrack)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                PeerlessIcon(
                    icon = if (isFav) PeerlessIcons.Heart else PeerlessIcons.HeartBorder,
                    contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                    tint = if (isFav) Color(0xFFFF5252) else colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatDuration(track.duration),
                    style = ExpressiveTypography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isPlaying) colorScheme.primary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )

                RipMorphBadge(
                    activeTask = activeTask,
                    isCached = isTrackCached,
                    onRipClick = {
                        val ripTarget = canonicalTrack?.toSummaryDto(activeSource) ?: track
                        if (onRipClick != null) {
                            onRipClick(ripTarget)
                        } else {
                            coroutineScope.launch {
                                ripCoordinator?.ripTrack(ripTarget)
                            }
                        }
                    },
                    onOpenDetails = { showRipDetailSheet = true }
                )
            }

            IconButton(
                onClick = {
                    if (onPlayNext != null || onAddToQueue != null || onStartRadio != null ||
                        onRemoveFromQueue != null || onMoveQueueItemUp != null || onMoveQueueItemDown != null
                    ) {
                        showTrackMenu = true
                    } else {
                        showAudioDetails = true
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.MoreVert,
                    contentDescription = "Track options",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showTrackMenu,
                onDismissRequest = { showTrackMenu = false }
            ) {
                val actionTrack = canonicalTrack?.toSummaryDto(activeSource) ?: track
                onPlayNext?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = { showTrackMenu = false; action(actionTrack) }
                    )
                }
                onAddToQueue?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        onClick = { showTrackMenu = false; action(actionTrack) }
                    )
                }
                onStartRadio?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Start radio") },
                        onClick = { showTrackMenu = false; action(actionTrack) }
                    )
                }
                onMoveQueueItemUp?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = { showTrackMenu = false; action() }
                    )
                }
                onMoveQueueItemDown?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = { showTrackMenu = false; action() }
                    )
                }
                onRemoveFromQueue?.let { action ->
                    DropdownMenuItem(
                        text = { Text("Remove from queue") },
                        onClick = { showTrackMenu = false; action() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Audio details") },
                    onClick = { showTrackMenu = false; showAudioDetails = true }
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    canonicalTrack: CanonicalTrack,
    artworkUrl: String,
    isPlaying: Boolean,
    onTrackClick: (CanonicalTrack) -> Unit,
    modifier: Modifier = Modifier,
    onSelectSource: ((TrackSource) -> Unit)? = null,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    onPlayNext: ((TrackSummaryDto) -> Unit)? = null,
    onAddToQueue: ((TrackSummaryDto) -> Unit)? = null,
    onStartRadio: ((TrackSummaryDto) -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onMoveQueueItemUp: (() -> Unit)? = null,
    onMoveQueueItemDown: (() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: ((TrackSummaryDto) -> Unit)? = null,
    isCurrent: Boolean = isPlaying,
    embedded: Boolean = false,
    showArtworkOverlay: Boolean = !embedded
) {
    val activeSource = canonicalTrack.immediatePlaySource() ?: canonicalTrack.bestSource
    val summary =
        remember(canonicalTrack, activeSource) { canonicalTrack.toSummaryDto(activeSource) }
    TrackRow(
        track = summary,
        artworkUrl = artworkUrl,
        isPlaying = isPlaying,
        onTrackClick = { onTrackClick(canonicalTrack) },
        modifier = modifier,
        canonicalTrack = canonicalTrack,
        onSelectSource = onSelectSource,
        onRipClick = onRipClick,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onStartRadio = onStartRadio,
        onRemoveFromQueue = onRemoveFromQueue,
        onMoveQueueItemUp = onMoveQueueItemUp,
        onMoveQueueItemDown = onMoveQueueItemDown,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        isCurrent = isCurrent,
        embedded = embedded,
        showArtworkOverlay = showArtworkOverlay
    )
}

@Composable
fun RipMorphBadge(
    activeTask: ActiveRipTask?,
    isCached: Boolean,
    onRipClick: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val touchTarget = 48.dp

    when {
        activeTask != null && !activeTask.isFinished -> {
            val infiniteTransition = rememberInfiniteTransition(label = "RipSpin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RipSpinAngle"
            )

            Box(
                modifier = modifier
                    .size(touchTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        onClick = onOpenDetails
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (activeTask.percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(28.dp),
                        color = colorScheme.primary,
                        trackColor = colorScheme.primary.copy(alpha = 0.2f),
                        strokeWidth = 2.5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    PeerlessIcon(
                        icon = PeerlessIcons.RipCloudSync,
                        contentDescription = "Ripping ${activeTask.percent.roundToInt()}%",
                        tint = colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(rotationZ = rotation)
                    )
                }
            }
        }

        activeTask != null && (activeTask.stage == RipStage.COMPLETED || activeTask.completed) -> {
            Box(
                modifier = modifier
                    .size(touchTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        onClick = onOpenDetails
                    ),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.RipCloudDone,
                    contentDescription = "Rip Completed",
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        !isCached -> {
            Box(
                modifier = modifier
                    .size(touchTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        onClick = onRipClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(colorScheme.tertiary, colorScheme.primary)
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.RipCloudDownload,
                            contentDescription = "Rip Track",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RIP",
                            style = SpecBadgeTypography.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            ),
                            color = colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        else -> {
            // Track is cached and no active rip
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RipTaskDetailSheet(
    task: ActiveRipTask,
    artworkUrl: String,
    onDismiss: () -> Unit,
    onCancelRip: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isFinished = task.isFinished
    val isCompleted = task.stage == RipStage.COMPLETED

    val targetProgress = when {
        isCompleted -> 1f
        else -> (task.percent / 100f).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300),
        label = "DetailProgress"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(ArtworkShape)
                        .background(colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.track.title,
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${task.track.artist} • ${task.track.album}",
                        style = ExpressiveTypography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RipStageBadge(stage = task.stage)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    task.speed?.let { speedText ->
                        if (!isFinished && speedText.isNotBlank()) {
                            Text(
                                text = speedText,
                                style = SpecBadgeTypography,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val percentLabel = when {
                        isCompleted -> "100%"
                        task.percent > 0f -> "${task.percent.roundToInt().coerceIn(0, 100)}%"
                        task.stage == RipStage.QUEUED -> "Queued"
                        else -> "0%"
                    }
                    Text(
                        text = percentLabel,
                        style = SpecBadgeTypography,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(PillShape),
                color = when {
                    task.stage == RipStage.ERROR -> colorScheme.error
                    isCompleted -> Color(0xFF4CAF50)
                    else -> colorScheme.primary
                },
                trackColor = colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )

            if (!task.isFinished && task.isOwner) {
                OutlinedButton(
                    onClick = {
                        onCancelRip()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
                ) {
                    Text("Cancel Rip", style = ExpressiveTypography.labelLarge)
                }
            }
        }
    }
}
