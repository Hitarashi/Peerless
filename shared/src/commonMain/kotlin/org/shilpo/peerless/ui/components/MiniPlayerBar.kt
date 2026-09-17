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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import org.shilpo.peerless.theme.*

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
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.90f else 1f, label = "PlayButtonScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(SquircleShapeMedium)
            .background(SurfaceContainerHighestDark)
            .border(1.dp, OutlineVariantDark.copy(alpha = 0.8f), SquircleShapeMedium)
            .clickable { onOpenNowPlaying() }
    ) {
        // Main MiniPlayer content
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top thin progress line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0x1AFFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryDark, SecondaryDark)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature artwork
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PeerlessIcons.MusicNote,
                        contentDescription = null,
                        tint = OnSurfaceVariantDark.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )

                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "${track.title} artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
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
                        color = OnSurfaceDark,
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
                            color = OnSurfaceVariantDark,
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

                // Action Controls: Play/Pause Squircle and Skip Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Play/Pause Squircle button with bouncy animation
                    Box(
                        modifier = Modifier
                            .scale(buttonScale)
                            .size(42.dp)
                            .clip(SquircleShapeSmall)
                            .background(PrimaryDark)
                            .clickable(
                                interactionSource = playButtonInteractionSource,
                                indication = ripple(),
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
                                tint = OnPrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Next track button
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.SkipNext,
                            contentDescription = "Next Track",
                            tint = OnSurfaceDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
