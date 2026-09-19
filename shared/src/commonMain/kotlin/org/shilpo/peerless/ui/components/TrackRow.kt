package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.CanonicalTrack
import org.shilpo.peerless.model.Codec
import org.shilpo.peerless.model.TrackSource
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.theme.ArtworkShape
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography

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
    val activeSource = selectedSource ?: canonicalTrack?.immediatePlaySource() ?: canonicalTrack?.bestSource

    val displayCodec = activeSource?.codec?.raw ?: track.codec
    val displayProvider = activeSource?.provider?.displayName ?: track.provider
    val isTrackCached = activeSource?.isCached ?: track.is_cached

    val hasApple = effectiveSources.any { it.provider.displayName.contains("apple", ignoreCase = true) } ||
            displayProvider.contains("apple", ignoreCase = true)
    val hasQobuz = effectiveSources.any { it.provider.displayName.contains("qobuz", ignoreCase = true) } ||
            displayProvider.contains("qobuz", ignoreCase = true)
    val hasDolby = effectiveSources.any { it.codec == Codec.Ec3 } ||
            activeSource?.codec == Codec.Ec3 ||
            displayCodec.contains("ec-3", ignoreCase = true) ||
            displayCodec.contains("ec3", ignoreCase = true) ||
            displayCodec.contains("atmos", ignoreCase = true)
    val hasHiRes = effectiveSources.any { (it.bitDepth ?: 16) >= 24 || (it.sampleRate ?: 44100) >= 88200 } ||
            (activeSource?.bitDepth ?: track.bit_depth ?: 16) >= 24 ||
            (activeSource?.sampleRate ?: track.sample_rate ?: 44100) >= 88200

    var showAudioDetails by remember { mutableStateOf(false) }

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
                    onTrackClick(clickSummary)
                },
                onLongClick = { showAudioDetails = true }
            )
            .padding(
                horizontal = if (embedded) 8.dp else 10.dp,
                vertical = if (embedded) 6.dp else 8.dp
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
                contentDescription = "${track.title} artwork",
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
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                style = ExpressiveTypography.bodySmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
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
                                        if (source.provider.displayName.contains("apple", ignoreCase = true)) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.AppleLogo,
                                                contentDescription = "Apple Music",
                                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else if (source.provider.displayName.contains("qobuz", ignoreCase = true)) {
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
                                                (source.bitDepth ?: 16) >= 24 || (source.sampleRate ?: 44100) >= 88200
                                            if (isSourceHiRes) {
                                                PeerlessIcon(
                                                    icon = PeerlessIcons.HiRes,
                                                    contentDescription = "Hi-Res Audio",
                                                    tint = colorScheme.tertiary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            val specLabel = buildString {
                                                if (source.bitDepth != null && source.bitDepth >= 24) append("${source.bitDepth}-bit ")
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
                                        val sourceSummary = canonicalTrack?.toSummaryDto(source) ?: track
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
                modifier = Modifier.size(28.dp)
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
                    color = if (isPlaying) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false
                )

                if (!isTrackCached && onRipClick != null) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colorScheme.tertiary, colorScheme.primary)
                                )
                            )
                            .clickable {
                                val ripTarget = canonicalTrack?.toSummaryDto(activeSource) ?: track
                                onRipClick(ripTarget)
                            }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RIP",
                            style = SpecBadgeTypography.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            ),
                            color = colorScheme.onTertiary
                        )
                    }
                }
            }

            IconButton(
                onClick = { showAudioDetails = true },
                modifier = Modifier.size(28.dp)
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.MoreVert,
                    contentDescription = "Track options",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
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
    isFavorite: Boolean? = null,
    onToggleFavorite: ((TrackSummaryDto) -> Unit)? = null,
    isCurrent: Boolean = isPlaying,
    embedded: Boolean = false,
    showArtworkOverlay: Boolean = !embedded
) {
    val activeSource = canonicalTrack.immediatePlaySource() ?: canonicalTrack.bestSource
    val summary = remember(canonicalTrack, activeSource) { canonicalTrack.toSummaryDto(activeSource) }
    TrackRow(
        track = summary,
        artworkUrl = artworkUrl,
        isPlaying = isPlaying,
        onTrackClick = { onTrackClick(canonicalTrack) },
        modifier = modifier,
        canonicalTrack = canonicalTrack,
        onSelectSource = onSelectSource,
        onRipClick = onRipClick,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        isCurrent = isCurrent,
        embedded = embedded,
        showArtworkOverlay = showArtworkOverlay
    )
}
