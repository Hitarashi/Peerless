package org.shilpo.peerless.ui.shell


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.lyrics.LyricsLoader
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.preferences.LocalAppPreferences
import org.shilpo.peerless.preferences.LyricsPresentation
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.theme.rememberMiniPlayerGlowPalette
import org.shilpo.peerless.ui.components.LyricsPresentationControl
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.SyncedLyricsContent
import org.shilpo.peerless.ui.components.TrackRow
import org.shilpo.peerless.ui.components.animationOptions
import org.shilpo.peerless.ui.components.visualConfig

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
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
                        index = index,
                        count = queueDtos.size,
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
    val appPreferences = LocalAppPreferences.current
    val lyricsPresentation by appPreferences.lyricsPresentation.collectAsState()
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

                LyricsPresentationControl(
                    presentation = lyricsPresentation,
                    onToggle = {
                        appPreferences.setLyricsPresentation(
                            if (lyricsPresentation == LyricsPresentation.VISUAL) {
                                LyricsPresentation.READABLE
                            } else {
                                LyricsPresentation.VISUAL
                            }
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                val effectiveOffset = if (outputLatencyMs != 0L) outputLatencyMs else -750L

                SyncedLyricsContent(
                    state = lyricsState,
                    positionMs = positionMs,
                    onSeekTo = onSeekTo,
                    onRetry = { lyricsLoader.retry(currentTrack, serverUrl) },
                    animationOptions = lyricsPresentation.animationOptions(),
                    readableMode = lyricsPresentation == LyricsPresentation.READABLE,
                    config = lyricsPresentation.visualConfig(effectiveOffset),
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
    }
}

@Composable
internal fun RipPaneContent(
    modifier: Modifier = Modifier
) {
    org.shilpo.peerless.ui.components.RipActivityPane(modifier = modifier)
}
