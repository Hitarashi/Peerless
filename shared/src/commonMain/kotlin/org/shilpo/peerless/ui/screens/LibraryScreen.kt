package org.shilpo.peerless.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.library.LocalFavoritesManager
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.playTrack
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.TrackRow

@Composable
fun LibraryScreen(
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    allTracks: List<TrackSummaryDto>,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    val favoritesManager = LocalFavoritesManager.current

    val favorites by (favoritesManager?.favorites
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }).collectAsState()
    val isFavoritesLoading by (favoritesManager?.isLoading
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()

    var selectedTab by remember { mutableStateOf("Favorites") }
    val tabs = listOf("Favorites", "Cached Downloads", "All Catalog")

    LaunchedEffect(Unit) {
        favoritesManager?.refreshFavorites()
    }

    val cachedOnly = remember(allTracks) { allTracks.filter { it.is_cached } }
    val rawTracks = when (selectedTab) {
        "Favorites" -> favorites
        "Cached Downloads" -> cachedOnly
        else -> allTracks
    }
    val tracksToShow = remember(rawTracks, apiClient.baseUrl) {
        CanonicalDeduplicator.deduplicateTracks(rawTracks, apiClient.baseUrl)
            .map { it.toSummaryDto() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (tab == "Favorites" && favorites.isNotEmpty()) "Favorites (${favorites.size})" else tab,
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (selectedTab == "Favorites" && favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = contentBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                if (isFavoritesLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Heart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "No Favorite Tracks Yet",
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Tap the heart icon on any track in Search or Home to bookmark it in your personal high-fidelity library.",
                            style = ExpressiveTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = contentBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tracksToShow, key = { it.id }) { track ->
                    val isCurrent = currentTrackDto?.id == track.id
                    val isPlaying = isCurrent &&
                            status == PlaybackStatus.PLAYING

                    TrackRow(
                        track = track,
                        artworkUrl = apiClient.getArtworkUrl(track, 200),
                        isPlaying = isPlaying,
                        isCurrent = isCurrent,
                        onTrackClick = { clicked ->
                            if (isCurrent) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playTrack(clicked, tracksToShow)
                            }
                        },
                        onPlayNext = if (track.is_cached) {
                            { clicked -> playerConnection.playNextInQueue(clicked.toTrack()) }
                        } else null,
                        onAddToQueue = if (track.is_cached) {
                            { clicked -> playerConnection.addToQueue(clicked.toTrack()) }
                        } else null,
                        onStartRadio = if (track.is_cached) {
                            { clicked -> playerConnection.startRadio(clicked.toTrack()) }
                        } else null,
                        isFavorite = favoritesManager?.isFavorite(track.id),
                        onToggleFavorite = { clicked ->
                            favoritesManager?.toggleFavorite(clicked)
                        }
                    )
                }
            }
        }
    }
}
