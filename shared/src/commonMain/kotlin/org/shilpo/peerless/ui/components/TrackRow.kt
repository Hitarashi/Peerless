package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.theme.*

fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds <= 0) return "00:00"
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    color: Color = PrimaryDark,
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

@Composable
fun TrackRow(
    track: TrackSummaryDto,
    artworkUrl: String,
    isPlaying: Boolean,
    onTrackClick: (TrackSummaryDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBg by animateColorAsState(
        targetValue = if (isPlaying) PrimaryDark.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(200)
    )

    val rowBorderColor by animateColorAsState(
        targetValue = if (isPlaying) PrimaryDark.copy(alpha = 0.30f) else Color.Transparent,
        animationSpec = tween(200)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(1.dp, rowBorderColor, RoundedCornerShape(14.dp))
            .clickable { onTrackClick(track) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-res artwork thumbnail with fallback placeholder
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(ArtworkShape)
                .background(SurfaceContainerHighestDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PeerlessIcons.MusicNote,
                contentDescription = null,
                tint = OnSurfaceVariantDark.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            AsyncImage(
                model = artworkUrl,
                contentDescription = "${track.title} artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying) {
                // Subtle scrim overlay with animated equalizer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizer(
                        isPlaying = true,
                        color = PrimaryDark,
                        barWidth = 3.dp,
                        maxHeight = 16.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title, Artist, Album, Specs
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isPlaying) PrimaryDark else OnSurfaceDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.is_cached) {
                    Icon(
                        imageVector = PeerlessIcons.CloudDone,
                        contentDescription = "Cached",
                        tint = SecondaryDark,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = "${track.artist} • ${track.album}",
                style = ExpressiveTypography.bodySmall,
                color = OnSurfaceVariantDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LosslessBadge(
                    bitDepth = track.bit_depth,
                    sampleRate = track.sample_rate,
                    codec = track.codec,
                    compact = true
                )

                Text(
                    text = track.provider.uppercase(),
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = OnSurfaceVariantDark.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration / Playing indicator
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDuration(track.duration),
                style = ExpressiveTypography.labelSmall,
                color = if (isPlaying) PrimaryDark else OnSurfaceVariantDark.copy(alpha = 0.8f)
            )
        }
    }
}
