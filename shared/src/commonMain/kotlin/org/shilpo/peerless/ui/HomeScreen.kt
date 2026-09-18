package org.shilpo.peerless.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
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
import kotlinx.coroutines.delay
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.UncachedTrackDto
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerState
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.components.*

val SampleLosslessLibrary = listOf(
    TrackSummaryDto(
        id = -101,
        provider = "qobuz",
        track_id = "sample_101",
        title = "Time",
        artist = "Pink Floyd",
        album = "The Dark Side of the Moon (50th Anniv.)",
        duration = 425,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/3e/76/b0/3e76b0e3-762b-2286-a019-8afb19cee541/886445635829.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -102,
        provider = "apple_music",
        track_id = "sample_102",
        title = "Get Lucky (feat. Pharrell Williams)",
        artist = "Daft Punk",
        album = "Random Access Memories",
        duration = 369,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 88200,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/e8/43/5f/e8435ffa-b6b9-b171-40ab-4ff3959ab661/886443919266.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -103,
        provider = "qobuz",
        track_id = "sample_103",
        title = "So What",
        artist = "Miles Davis",
        album = "Kind of Blue",
        duration = 562,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 192000,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music/7f/9f/d6/mzi.vtnaewef.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -104,
        provider = "apple_music",
        track_id = "sample_104",
        title = "Dreams",
        artist = "Fleetwood Mac",
        album = "Rumours (Super Deluxe)",
        duration = 257,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/d2/48/f4/d248f4ae-a7e4-a48e-1588-6617de3e8d76/mzi.izeorbmm.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -105,
        provider = "qobuz",
        track_id = "sample_105",
        title = "Paranoid Android",
        artist = "Radiohead",
        album = "OK Computer",
        duration = 383,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = false,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/07/60/ba/0760ba0f-148c-b18f-d0ff-169ee96f3af5/634904078164.png/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -106,
        provider = "apple_music",
        track_id = "sample_106",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        duration = 200,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 48000,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/61/e7/3f/61e73f94-018d-5f50-50ec-8521952bc72e/20UM1IM11629.rgb.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -107,
        provider = "qobuz",
        track_id = "sample_107",
        title = "Hotel California",
        artist = "Eagles",
        album = "Hotel California (2013 Remaster)",
        duration = 391,
        codec = "FLAC",
        bit_depth = 24,
        sample_rate = 192000,
        is_cached = false,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/88/16/2c/88162c3d-46db-8321-61f3-3a47404cfe76/075596050920.jpg/600x600bb.jpg"
    ),
    TrackSummaryDto(
        id = -108,
        provider = "apple_music",
        track_id = "sample_108",
        title = "Come Together",
        artist = "The Beatles",
        album = "Abbey Road (2019 Mix)",
        duration = 259,
        codec = "ALAC",
        bit_depth = 24,
        sample_rate = 96000,
        is_cached = true,
        artwork_url = "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/df/db/61/dfdb615d-47f8-06e9-9533-b96daccc029f/18UMGIM31076.rgb.jpg/600x600bb.jpg"
    )
)

fun UncachedTrackDto.toTrackSummary(): TrackSummaryDto = TrackSummaryDto(
    id = -(kotlin.math.abs(track_id.hashCode()).let { if (it == 0) 1 else it }),
    provider = provider,
    track_id = track_id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    codec = "FLAC",
    bit_depth = 24,
    sample_rate = 96000,
    is_cached = false,
    artwork_url = artwork_url
)

@Composable
fun HomeTopHeader(
    serverConnected: Boolean,
    onNavigateToSearch: () -> Unit,
    onToggleStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                    contentDescription = "Peerless",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "PEERLESS",
                style = ExpressiveTypography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = OnSurfaceDark
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerDark)
                    .border(1.dp, OutlineVariantDark.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onToggleStats),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.Compass,
                    contentDescription = "Explore & Stats",
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerDark)
                    .border(1.dp, OutlineVariantDark.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onNavigateToSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.Search,
                    contentDescription = "Search",
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerDark)
                    .border(1.dp, OutlineVariantDark.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PeerlessIcons.Settings,
                    contentDescription = "Settings",
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPicksCarousel(
    tracks: List<TrackSummaryDto>,
    currentTrackId: Int?,
    isPlaying: Boolean,
    onTrackClick: (TrackSummaryDto) -> Unit,
    getArtworkUrl: (TrackSummaryDto) -> String,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quick picks",
            style = ExpressiveTypography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceDark,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val heroHeight = when {
                maxWidth >= 840.dp -> 380.dp
                maxWidth >= 600.dp -> 356.dp
                else -> 332.dp
            }
            val heroMaxWidth = (maxWidth - 48.dp)
                .coerceAtLeast(232.dp)
                .coerceAtMost(440.dp)

            val carouselState = rememberCarouselState { tracks.size }

            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = heroMaxWidth,
                itemSpacing = 10.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
            ) { index ->
                val track = tracks[index]
                val isCurrentPlaying = currentTrackId == track.id && isPlaying

                QuickPickCard(
                    track = track,
                    artworkUrl = getArtworkUrl(track),
                    isPlaying = isCurrentPlaying,
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                )
            }
        }
    }
}

@Composable
fun QuickPickCard(
    track: TrackSummaryDto,
    artworkUrl: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceContainerHighDark)
            .border(
                1.dp,
                if (isPlaying) PrimaryDark.copy(alpha = 0.6f) else OutlineVariantDark.copy(alpha = 0.5f),
                MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = "${track.title} cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.84f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = track.title,
                style = ExpressiveTypography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = track.artist,
                style = ExpressiveTypography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PowerampLosslessBadge(
                    bitDepth = track.bit_depth,
                    sampleRate = track.sample_rate,
                    codec = track.codec,
                    compact = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LosslessLibrarySectionHeader(
    trackCount: Int,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lossless Library",
                style = ExpressiveTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark
            )

            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(SurfaceContainerDark)
                    .border(1.dp, OutlineVariantDark.copy(alpha = 0.5f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$trackCount tracks",
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = OnSurfaceVariantDark
                )
            }
        }

        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SurfaceContainerHighestDark,
                        contentColor = OnSurfaceDark
                    )
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Play,
                        contentDescription = "Play all",
                        tint = PrimaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play all",
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    onClick = onShuffle,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SurfaceContainerDark,
                        contentColor = OnSurfaceDark
                    )
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = OnSurfaceDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun HomeExpressiveContent(
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    onOpenSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onToggleStats: () -> Unit,
    contentBottomPadding: Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "top_header") {
            HomeTopHeader(
                serverConnected = serverConnected,
                onNavigateToSearch = onNavigateToSearch,
                onToggleStats = onToggleStats,
                onOpenSettings = onOpenSettings
            )
        }

        if (searchQuery.isBlank()) {

            if (allTracks.isNotEmpty()) {
                item(key = "quick_picks") {
                    QuickPicksCarousel(
                        tracks = allTracks.take(6),
                        currentTrackId = playerState.currentTrack?.id,
                        isPlaying = playerState.status == PlaybackStatus.PLAYING,
                        onTrackClick = { clicked ->
                            coordinator.playTrack(clicked, allTracks)
                        },
                        getArtworkUrl = { track ->
                            coordinator.apiClient.getArtworkUrl(track, 300)
                        }
                    )
                }
            }
        }

        item(key = "library_header") {
            LosslessLibrarySectionHeader(
                trackCount = displayedTracks.size,
                onShuffle = {
                    if (displayedTracks.isNotEmpty()) {
                        val shuffled = displayedTracks.shuffled()
                        coordinator.playTrack(shuffled.first(), shuffled)
                    }
                },
                onPlayAll = {
                    if (displayedTracks.isNotEmpty()) {
                        coordinator.playTrack(displayedTracks.first(), displayedTracks)
                    }
                }
            )
        }

        if (displayedTracks.isEmpty()) {
            item(key = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariantDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No matching lossless tracks found",
                            style = ExpressiveTypography.bodyMedium,
                            color = OnSurfaceVariantDark
                        )
                    }
                }
            }
        } else {
            items(
                items = displayedTracks,
                key = { it.id }
            ) { track ->
                val isPlaying = playerState.currentTrack?.id == track.id &&
                        playerState.status == PlaybackStatus.PLAYING

                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    TrackRow(
                        track = track,
                        artworkUrl = coordinator.apiClient.getArtworkUrl(track, 200),
                        isPlaying = isPlaying,
                        onTrackClick = { clicked ->
                            coordinator.playTrack(clicked, displayedTracks)
                        },
                        onRipClick = onRipClick
                    )
                }
            }
        }
    }
}

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

    LaunchedEffect(searchQuery, selectedFilter, playerState.serverUrl) {
        isSearching = true
        delay(300)

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HomeExpressiveContent(
                coordinator = coordinator,
                playerState = playerState,
                serverConnected = serverConnected,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onSelectFilter = { selectedFilter = it },
                displayedTracks = displayedTracks,
                allTracks = activeLibrary,
                onOpenSettings = { isSettingsOpen = true },
                onNavigateToSearch = { searchQuery = " " },
                onToggleStats = { isNowPlayingOpen = true },
                contentBottomPadding = if (playerState.currentTrack != null) 96.dp else 24.dp
            )
        }

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
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track, 200),
                    onTogglePlayPause = { coordinator.togglePlayPause() },
                    onPlayNext = { coordinator.playNext() },
                    onPlayPrevious = { coordinator.playPrevious() },
                    onOpenNowPlaying = { isNowPlayingOpen = true },
                    onDismiss = { coordinator.stopAndDismiss() },
                    canSkipNext = coordinator.canSkipNext,
                    canSkipPrevious = coordinator.canSkipPrevious
                )
            }
        }

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
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track, 600),
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
