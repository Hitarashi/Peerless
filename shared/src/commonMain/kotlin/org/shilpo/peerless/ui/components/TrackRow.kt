package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.AudioSpecs
import org.shilpo.peerless.model.Codec
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
fun PowerampLosslessBadge(
    bitDepth: Int?,
    sampleRate: Int?,
    codec: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val codecEnum = Codec.fromString(codec ?: "flac")
    val specs = AudioSpecs(
        codec = codecEnum,
        bitDepth = bitDepth,
        sampleRate = sampleRate
    )
    val colorScheme = MaterialTheme.colorScheme
    val isHiRes = specs.isHiRes
    val badgeAccent = if (isHiRes) colorScheme.tertiary else colorScheme.secondary
    val badgeBg = badgeAccent.copy(alpha = 0.12f)
    val badgeBorder = badgeAccent.copy(alpha = 0.35f)

    val formattedSpecs = if (compact) {
        buildString {
            if (bitDepth != null && bitDepth >= 24) append("24B ")
            append((codec ?: "FLAC").uppercase())
        }
    } else {
        buildString {
            if (bitDepth != null && bitDepth >= 24) append("24-BIT ")
            if (sampleRate != null && sampleRate > 48000) {
                val khz = if (sampleRate % 1000 == 0) "${sampleRate / 1000}" else "${sampleRate / 1000.0}"
                append("${khz}k ")
            }
            append((codec ?: "FLAC").uppercase())
        }.trim()
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .border(1.dp, badgeBorder, RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = PeerlessIcons.LosslessWave,
            contentDescription = null,
            tint = badgeAccent,
            modifier = Modifier.size(9.dp)
        )
        Text(
            text = formattedSpecs,
            style = SpecBadgeTypography.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = badgeAccent,
            maxLines = 1,
            softWrap = false
        )
    }
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
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: ((TrackSummaryDto) -> Unit)? = null
) {
    val favoritesManager = org.shilpo.peerless.library.LocalFavoritesManager.current
    val favoriteIds by (favoritesManager?.favoriteIds
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptySet()) }).collectAsState()
    val isFav = isFavorite ?: favoriteIds.contains(track.id)

    var showAudioDetails by remember { mutableStateOf(false) }

    if (showAudioDetails) {
        AudioDetailsModal(
            track = track,
            artworkUrl = artworkUrl,
            onDismiss = { showAudioDetails = false },
            onPlayTrack = { onTrackClick(track) }
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    val rowBg by animateColorAsState(
        targetValue = if (isPlaying) colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200)
    )

    val rowBorderColor by animateColorAsState(
        targetValue = if (isPlaying) colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(200)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(1.dp, rowBorderColor, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { onTrackClick(track) },
                onLongClick = { showAudioDetails = true }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(ArtworkShape)
                .background(colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PeerlessIcons.MusicNote,
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

            if (isPlaying) {
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
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPlaying) colorScheme.primary else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.is_cached) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colorScheme.secondary.copy(alpha = 0.14f))
                            .border(1.dp, colorScheme.secondary.copy(alpha = 0.45f), PillShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.CloudDone,
                            contentDescription = "Cached",
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "CACHED",
                            style = SpecBadgeTypography.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.tertiary.copy(alpha = 0.20f),
                                        colorScheme.primary.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.tertiary.copy(alpha = 0.60f),
                                        colorScheme.primary.copy(alpha = 0.60f)
                                    )
                                ),
                                PillShape
                            )
                            .then(
                                if (onRipClick != null) Modifier.clickable { onRipClick(track) } else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RIP & STREAM",
                            style = SpecBadgeTypography.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = colorScheme.tertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = "${track.artist} • ${track.album}",
                style = ExpressiveTypography.bodySmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PowerampLosslessBadge(
                    bitDepth = track.bit_depth,
                    sampleRate = track.sample_rate,
                    codec = track.codec,
                    compact = false,
                    onClick = { showAudioDetails = true }
                )

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(colorScheme.surfaceContainer)
                        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f), PillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatProviderLabel(track.provider),
                        style = SpecBadgeTypography.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = {
                    if (onToggleFavorite != null) {
                        onToggleFavorite(track)
                    } else {
                        favoritesManager?.toggleFavorite(track)
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFav) PeerlessIcons.Heart else PeerlessIcons.HeartBorder,
                    contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                    tint = if (isFav) Color(0xFFFF5252) else colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = formatDuration(track.duration),
                    style = ExpressiveTypography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isPlaying) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false
                )

                if (!track.is_cached && onRipClick != null) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colorScheme.tertiary, colorScheme.primary)
                                )
                            )
                            .clickable { onRipClick(track) }
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = PeerlessIcons.MoreVert,
                    contentDescription = "Track options",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
