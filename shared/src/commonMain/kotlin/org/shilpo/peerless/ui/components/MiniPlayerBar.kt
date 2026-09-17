package org.shilpo.peerless.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape

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
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isPlaying = status == PlaybackStatus.PLAYING
    val isBuffering = status == PlaybackStatus.BUFFERING

    val playButtonInteractionSource = remember { MutableInteractionSource() }
    val isPressed by playButtonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1f, label = "PlayButtonScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                    )
                ),
                PillShape
            )
            .clickable { onOpenNowPlaying() }
    ) {
        // Official Material 3 Expressive LinearWavyProgressIndicator
        if (isBuffering) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = Color(0x14FFFFFF)
            )
        } else {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0x14FFFFFF)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Artwork with glowing border
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        1.5.dp,
                        if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
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
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizer(
                            isPlaying = true,
                            color = MaterialTheme.colorScheme.primary,
                            barWidth = 2.5.dp,
                            maxHeight = 13.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, Micro Lossless Badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = track.title,
                    style = ExpressiveTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.artist,
                        style = ExpressiveTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    LosslessBadge(
                        bitDepth = playbackInfo?.bit_depth ?: track.bit_depth,
                        sampleRate = playbackInfo?.sample_rate ?: track.sample_rate,
                        codec = playbackInfo?.codec ?: track.codec,
                        compact = true
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Controls: Circular Play/Pause & Skip Next
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // High-contrast circular Play/Pause button
                Box(
                    modifier = Modifier
                        .scale(buttonScale)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = playButtonInteractionSource,
                            indication = ripple(bounded = false, radius = 21.dp),
                            onClick = onTogglePlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "PlayPauseAnim"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) PeerlessIcons.Pause else PeerlessIcons.Play,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Skip Next button
                IconButton(
                    onClick = onPlayNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
