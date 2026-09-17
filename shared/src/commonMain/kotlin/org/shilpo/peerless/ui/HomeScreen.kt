package org.shilpo.peerless.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.UncachedTrackDto
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.components.*

val SampleLosslessLibrary = listOf(
    TrackSummaryDto(
        id = 101,
        provider = "qobuz",
        track_id = "101",
        title = "Time",
        artist = "Pink Floyd",
        album = "The Dark Side of the Moon (50th Anniv.)",
        duration = 425,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true
    ),
    TrackSummaryDto(
        id = 102,
        provider = "apple_music",
        track_id = "102",
        title = "Get Lucky (feat. Pharrell Williams)",
        artist = "Daft Punk",
        album = "Random Access Memories",
        duration = 369,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 88200,
        is_cached = true
    ),
    TrackSummaryDto(
        id = 103,
        provider = "qobuz",
        track_id = "103",
        title = "So What",
        artist = "Miles Davis",
        album = "Kind of Blue",
        duration = 562,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 192000,
        is_cached = true
    ),
    TrackSummaryDto(
        id = 104,
        provider = "apple_music",
        track_id = "104",
        title = "Dreams",
        artist = "Fleetwood Mac",
        album = "Rumours (Super Deluxe)",
        duration = 257,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true
    ),
    TrackSummaryDto(
        id = 105,
        provider = "qobuz",
        track_id = "105",
        title = "Paranoid Android",
        artist = "Radiohead",
        album = "OK Computer",
        duration = 383,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = false
    ),
    TrackSummaryDto(
        id = 106,
        provider = "apple_music",
        track_id = "106",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        duration = 200,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 48000,
        is_cached = true
    ),
    TrackSummaryDto(
        id = 107,
        provider = "qobuz",
        track_id = "107",
        title = "Hotel California",
        artist = "Eagles",
        album = "Hotel California (2013 Remaster)",
        duration = 391,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 192000,
        is_cached = false
    ),
    TrackSummaryDto(
        id = 108,
        provider = "apple_music",
        track_id = "108",
        title = "Come Together",
        artist = "The Beatles",
        album = "Abbey Road (2019 Mix)",
        duration = 259,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true
    )
)

fun UncachedTrackDto.toTrackSummary(): TrackSummaryDto = TrackSummaryDto(
    id = track_id.hashCode().let { if (it == 0) 1 else kotlin.math.abs(it) },
    provider = provider,
    track_id = track_id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    codec = "FLAC",
    bit_depth = 24,
    sample_rate = 96000,
    is_cached = false
)

@Composable
fun HomeScreen(
    coordinator: PlaybackCoordinator = remember { PlaybackCoordinator() },
    modifier: Modifier = Modifier
) {
    val playerState by coordinator.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var serverConnected by remember { mutableStateOf(false) }
    var serverTracks by remember { mutableStateOf<List<TrackSummaryDto>>(emptyList()) }

    // Query server on start or query/filter change
    LaunchedEffect(searchQuery, selectedFilter, playerState.serverUrl) {
        isSearching = true
        delay(300) // Debounce

        val providerParam = when (selectedFilter) {
            "Apple Music" -> "apple_music"
            "Qobuz" -> "qobuz"
            else -> null
        }

        val result = coordinator.apiClient.search(
            query = searchQuery.trim(),
            provider = providerParam
        )

        result.onSuccess { response ->
            serverConnected = true
            val combined = response.cached + response.live.map { it.toTrackSummary() }
            serverTracks = combined
            isSearching = false
        }.onFailure {
            serverConnected = false
            isSearching = false
        }
    }

    // Determine displayed tracks
    val activeLibrary = if (serverTracks.isNotEmpty()) serverTracks else SampleLosslessLibrary
    val displayedTracks = remember(activeLibrary, selectedFilter, searchQuery) {
        activeLibrary.filter { track ->
            val matchesFilter = when (selectedFilter) {
                "Cached" -> track.is_cached
                "Apple Music" -> track.provider.contains("apple", ignoreCase = true)
                "Qobuz" -> track.provider.contains("qobuz", ignoreCase = true)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true) ||
                    track.album.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceContainerLowestDark,
                        BackgroundDark,
                        SurfaceDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Brand & Status Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(SquircleShapeSmall)
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryDark, TertiaryDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PEERLESS",
                            style = ExpressiveTypography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "BIT-PERFECT STREAMING",
                            style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                            color = SecondaryDark,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Daemon status pill
                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceContainerDark)
                        .border(1.dp, OutlineVariantDark, PillShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (serverConnected) SecondaryDark else LosslessGold)
                    )

                    Text(
                        text = if (serverConnected) "ONLINE" else "LOCAL CACHE",
                        style = SpecBadgeTypography.copy(fontSize = 9.sp),
                        color = if (serverConnected) SecondaryDark else LosslessGold
                    )
                }
            }

            // Expressive Search Bar & Filter Chips
            ExpressiveSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearQuery = { searchQuery = "" },
                selectedFilter = selectedFilter,
                onFilterSelect = { selectedFilter = it },
                onOpenSettings = { isSettingsOpen = true },
                isDevMode = playerState.isDevMode,
                serverUrl = playerState.serverUrl
            )

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Lossless Library" else "Search Results",
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Text(
                    text = "${displayedTracks.size} tracks",
                    style = SpecBadgeTypography,
                    color = OnSurfaceVariantDark
                )
            }

            // Track List
            if (displayedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No matching lossless tracks found",
                            style = ExpressiveTypography.bodyLarge,
                            color = OnSurfaceVariantDark
                        )
                        Text(
                            text = "Try searching by song title, artist, or clearing filter chips",
                            style = ExpressiveTypography.bodySmall,
                            color = OnSurfaceVariantDark.copy(alpha = 0.7f)
                        )
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
                        bottom = if (playerState.currentTrack != null) 96.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = displayedTracks,
                        key = { it.id }
                    ) { track ->
                        val isPlaying = playerState.currentTrack?.id == track.id &&
                                playerState.status == PlaybackStatus.PLAYING

                        TrackRow(
                            track = track,
                            artworkUrl = coordinator.apiClient.getArtworkUrl(track.id, 200),
                            isPlaying = isPlaying,
                            onTrackClick = { clicked ->
                                coordinator.playTrack(clicked, displayedTracks)
                            }
                        )
                    }
                }
            }
        }

        // Floating MiniPlayer Bar
        AnimatedVisibility(
            visible = playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            playerState.currentTrack?.let { track ->
                MiniPlayerBar(
                    track = track,
                    playbackInfo = playerState.playbackInfo,
                    status = playerState.status,
                    positionMs = playerState.positionMs,
                    durationMs = playerState.durationMs,
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track.id, 200),
                    onTogglePlayPause = { coordinator.togglePlayPause() },
                    onPlayNext = { coordinator.playNext() },
                    onOpenNowPlaying = { isNowPlayingOpen = true }
                )
            }
        }

        // Animated Fullscreen Now-Playing Modal
        AnimatedVisibility(
            visible = isNowPlayingOpen && playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            playerState.currentTrack?.let { track ->
                NowPlayingSheet(
                    track = track,
                    playbackInfo = playerState.playbackInfo,
                    status = playerState.status,
                    positionMs = playerState.positionMs,
                    durationMs = playerState.durationMs,
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track.id, 600),
                    serverUrl = playerState.serverUrl,
                    isDevMode = playerState.isDevMode,
                    onTogglePlayPause = { coordinator.togglePlayPause() },
                    onSeekTo = { pos -> coordinator.seekTo(pos) },
                    onPlayNext = { coordinator.playNext() },
                    onPlayPrevious = { coordinator.playPrevious() },
                    onClose = { isNowPlayingOpen = false },
                    onOpenSettings = { isSettingsOpen = true }
                )
            }
        }

        // Server Settings Dialog
        if (isSettingsOpen) {
            ServerSettingsDialog(
                currentServerUrl = playerState.serverUrl,
                currentDevMode = playerState.isDevMode,
                onSave = { newUrl, newDevMode ->
                    coordinator.setServerUrl(newUrl)
                    coordinator.setDevMode(newDevMode)
                },
                onDismiss = { isSettingsOpen = false }
            )
        }
    }
}
