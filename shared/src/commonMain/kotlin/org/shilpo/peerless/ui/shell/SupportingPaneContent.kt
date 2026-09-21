package org.shilpo.peerless.ui.shell


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shilpo.peerless.lyrics.LyricsLoader
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeSmall
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.theme.rememberMiniPlayerGlowPalette
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.SyncedLyricsContent
import org.shilpo.peerless.ui.components.TrackRow
import org.shilpo.peerless.ui.components.lyrics.VisualLyricsConfig

@Composable
internal fun QueuePaneContent(
    playerConnection: PlayerConnection,
    queueDtos: List<TrackSummaryDto>,
    status: PlaybackStatus
) {
    val apiClient = LocalPeerlessApiClient.current
    val currentIndex by playerConnection.currentIndex.collectAsState()

    if (queueDtos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.Queue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Queue is empty",
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Queue",
                    style = ExpressiveTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = playerConnection::clearQueue) {
                    Text("Clear queue")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = queueDtos,
                    key = { index, track -> "queue_${track.id}_$index" }
                ) { index, trackDto ->
                    val isCurrent = currentIndex == index
                    val isPlaying = isCurrent && status == PlaybackStatus.PLAYING

                    TrackRow(
                        track = trackDto,
                        artworkUrl = apiClient.getArtworkUrl(trackDto, 120),
                        isPlaying = isPlaying,
                        isCurrent = isCurrent,
                        onTrackClick = { playerConnection.playQueueItem(index) },
                        onMoveQueueItemUp = if (index > 0) {
                            { playerConnection.moveInQueue(index, index - 1) }
                        } else null,
                        onMoveQueueItemDown = if (index < queueDtos.lastIndex) {
                            { playerConnection.moveInQueue(index, index + 1) }
                        } else null,
                        onRemoveFromQueue = { playerConnection.removeAt(index) },
                        showArtworkOverlay = false
                    )
                }
            }
        }
    }
}

@Composable
internal fun LyricsPaneContent(
    currentTrack: TrackSummaryDto?,
    lyricsLoader: LyricsLoader,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    isPlaying: Boolean,
    outputLatencyMs: Long = 0L
) {
    val apiClient = LocalPeerlessApiClient.current
    val serverUrl by apiClient.baseUrlState.collectAsState()
    val lyricsState by lyricsLoader.state.collectAsState()

    LaunchedEffect(currentTrack?.id, serverUrl) {
        currentTrack?.let { lyricsLoader.loadIfNeeded(it, serverUrl) }
    }

    if (currentTrack == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Play a track to view synced lyrics",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val artworkUrl = apiClient.getArtworkUrl(currentTrack, 600)
        val palette = rememberMiniPlayerGlowPalette(
            rememberArtworkSeedColor(artworkUrl, MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = currentTrack.artist,
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val effectiveOffset = if (outputLatencyMs != 0L) outputLatencyMs else -750L

                SyncedLyricsContent(
                    state = lyricsState,
                    positionMs = positionMs,
                    onSeekTo = onSeekTo,
                    onRetry = { lyricsLoader.retry(currentTrack, serverUrl) },
                    config = VisualLyricsConfig(syncOffsetMs = effectiveOffset),
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
    }
}

@Composable
internal fun SignalPathPaneContent(
    track: TrackSummaryDto?,
    playbackInfo: PlaybackInfo?,
    serverUrl: String
) {
    if (track == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active audio stream to inspect",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SignalPathStageCard(
                stageNumber = "1",
                stageName = "SOURCE ORIGIN",
                primaryInfo = if (track.is_cached) "Telegram Dump Channel (Instant)" else "${track.provider.uppercase()} Mirror",
                secondaryInfo = "Endpoint: $serverUrl • HMAC Signed Ticket",
                accentColor = MaterialTheme.colorScheme.secondary
            )

            val codec = playbackInfo?.codec ?: track.codec
            val bitDepth = playbackInfo?.bit_depth ?: track.bit_depth
            val sampleRate = playbackInfo?.sample_rate ?: track.sample_rate
            SignalPathStageCard(
                stageNumber = "2",
                stageName = "CONTAINER & CODEC",
                primaryInfo = "${codec.uppercase()} Lossless",
                secondaryInfo = "${bitDepth ?: 24}-Bit • ${((sampleRate ?: 96000) / 1000.0)} kHz • 2.0 Stereo",
                accentColor = MaterialTheme.colorScheme.tertiary
            )

            SignalPathStageCard(
                stageNumber = "3",
                stageName = "PLATFORM DECODER",
                primaryInfo = "Native Multiplatform Audio Engine",
                secondaryInfo = "Bit-perfect PCM Uncompressed Buffer",
                accentColor = MaterialTheme.colorScheme.primary
            )

            SignalPathStageCard(
                stageNumber = "4",
                stageName = "STREAM PIPE / CACHE",
                primaryInfo = "HTTP Chunk Buffer (Chunked Transfer)",
                secondaryInfo = "Latency: <120ms • Gapless Engine Active",
                accentColor = MaterialTheme.colorScheme.secondary
            )

            SignalPathStageCard(
                stageNumber = "5",
                stageName = "OUTPUT & SINK",
                primaryInfo = "Hardware Audio Sink",
                secondaryInfo = "High-Res Direct Path • No Resampling",
                accentColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun SignalPathStageCard(
    stageNumber: String,
    stageName: String,
    primaryInfo: String,
    secondaryInfo: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShapeSmall)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                SquircleShapeSmall
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stageNumber,
                    style = SpecBadgeTypography.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stageName,
                    style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = primaryInfo,
                    style = ExpressiveTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = secondaryInfo,
                    style = ExpressiveTypography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
