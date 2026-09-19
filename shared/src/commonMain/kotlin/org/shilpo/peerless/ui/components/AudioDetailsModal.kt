package org.shilpo.peerless.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.AudioSpecs
import org.shilpo.peerless.model.Codec
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.theme.*

@Composable
fun AudioDetailsModal(
    track: TrackSummaryDto,
    artworkUrl: String? = null,
    onDismiss: () -> Unit,
    onPlayTrack: (() -> Unit)? = null
) {
    val codecEnum = Codec.fromString(track.codec)
    val specs = AudioSpecs(
        codec = codecEnum,
        bitDepth = track.bit_depth,
        sampleRate = track.sample_rate
    )
    val colorScheme = MaterialTheme.colorScheme
    val isHiRes = specs.isHiRes
    val tierColor = if (isHiRes) colorScheme.tertiary else colorScheme.secondary
    val tierBorder = tierColor.copy(alpha = 0.35f)

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            shape = SquircleShapeLarge,
            containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            borderBrush = Brush.verticalGradient(
                listOf(tierColor.copy(alpha = 0.5f), colorScheme.outlineVariant)
            ),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.SignalPath,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AUDIO SIGNAL PATH",
                            style = SpecBadgeTypography.copy(
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = tierColor
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(tierColor.copy(alpha = 0.16f))
                            .border(1.dp, tierBorder, PillShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isHiRes) {
                            Icon(
                                imageVector = PeerlessIcons.HiRes,
                                contentDescription = "Hi-Res Audio",
                                tint = tierColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = if (isHiRes) "HI-RES AUDIO" else "LOSSLESS AUDIO",
                            style = SpecBadgeTypography.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = tierColor
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleShapeMedium)
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(ArtworkShape)
                            .background(colorScheme.surfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        if (!artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = "${track.title} artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = ExpressiveTypography.bodyMedium,
                            color = colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.album,
                            style = ExpressiveTypography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SquircleShapeMedium)
                        .background(colorScheme.surfaceContainer)
                        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f), SquircleShapeMedium)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpecRow("Specs Badge", specs.badgeText)
                    SpecRow("Codec", codecEnum.displayName.uppercase())
                    SpecRow("Resolution", "${track.bit_depth ?: 16} BIT PCM")
                    val sampleRateKhz = track.sample_rate?.let {
                        if (it % 1000 == 0) "${it / 1000} kHz" else "${it / 1000.0} kHz"
                    } ?: "44.1 kHz"
                    SpecRow("Sample Rate", "$sampleRateKhz (${track.sample_rate ?: 44100} Hz)")
                    specs.effectiveBitrateKbps?.let { kbps ->
                        SpecRow("Bitrate", "$kbps kbps")
                    }
                    SpecRow("Provider Source", track.provider.uppercase())
                    SpecRow(
                        "Storage & Cache",
                        if (track.is_cached) "Telegram Dump Channel (<200ms)" else "Uncached (On-Demand Rip)"
                    )
                    SpecRow("Track ID", track.track_id)
                    SpecRow("Duration", formatDuration(track.duration))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dismiss",
                        style = ExpressiveTypography.labelLarge,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (onPlayTrack != null) {
                        Button(
                            onClick = {
                                onPlayTrack()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            shape = PillShape
                        ) {
                            Icon(
                                imageVector = PeerlessIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = SpecBadgeTypography.copy(
                fontSize = 9.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
        Text(
            text = value,
            style = SpecBadgeTypography.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
        )
    }
}
