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
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.components.*

val SampleLosslessLibrary = listOf(
    TrackSummaryDto(
        id = 29516,
        provider = "apple",
        track_id = "1651284061",
        title = "Tamaki (Dolby Atmos)",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 296,
        codec = "ec-3",
        bit_depth = 16,
        sample_rate = 48000,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29515,
        provider = "apple",
        track_id = "1651284061",
        title = "Tamaki",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 296,
        codec = "alac",
        bit_depth = 16,
        sample_rate = 44100,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29514,
        provider = "apple",
        track_id = "1651284059",
        title = "Suzume (Dolby Atmos)",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 238,
        codec = "ec-3",
        bit_depth = 16,
        sample_rate = 48000,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29513,
        provider = "apple",
        track_id = "1651284059",
        title = "Suzume (feat. Toaka)",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 238,
        codec = "alac",
        bit_depth = 16,
        sample_rate = 44100,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29512,
        provider = "apple",
        track_id = "1651284057",
        title = "Kanata Haluka (Dolby Atmos)",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 356,
        codec = "ec-3",
        bit_depth = 16,
        sample_rate = 48000,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29511,
        provider = "apple",
        track_id = "1651284057",
        title = "Kanata Haluka",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 356,
        codec = "alac",
        bit_depth = 16,
        sample_rate = 44100,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29518,
        provider = "apple",
        track_id = "1651284062",
        title = "Tears of Suzume (Dolby Atmos)",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 276,
        codec = "ec-3",
        bit_depth = 16,
        sample_rate = 48000,
        is_cached = true,
        artwork_url = ""
    ),
    TrackSummaryDto(
        id = 29517,
        provider = "apple",
        track_id = "1651284062",
        title = "Tears of Suzume",
        artist = "RADWIMPS",
        album = "Suzume (Motion Picture Soundtrack)",
        duration = 276,
        codec = "alac",
        bit_depth = 16,
        sample_rate = 44100,
        is_cached = true,
        artwork_url = ""
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
    playerConnection: PlayerConnection,
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
    val currentTrack by playerConnection.currentTrack.collectAsState()
    val currentTrackDto = currentTrack?.toSummaryDto()
    val status by playerConnection.status.collectAsState()
    val apiClient = (playerConnection as RealPlayerConnection).apiClient

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
                        currentTrackId = currentTrackDto?.id,
                        isPlaying = status == PlaybackStatus.PLAYING,
                        onTrackClick = { clicked ->
                            playerConnection.play(clicked.toTrack(), allTracks.map { it.toTrack() })
                        },
                        getArtworkUrl = { track ->
                            apiClient.getArtworkUrl(track, 300)
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
                        playerConnection.play(shuffled.first().toTrack(), shuffled.map { it.toTrack() })
                    }
                },
                onPlayAll = {
                    if (displayedTracks.isNotEmpty()) {
                        playerConnection.play(displayedTracks.first().toTrack(), displayedTracks.map { it.toTrack() })
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
                val isPlaying = currentTrackDto?.id == track.id &&
                        status == PlaybackStatus.PLAYING

                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    TrackRow(
                        track = track,
                        artworkUrl = apiClient.getArtworkUrl(track, 200),
                        isPlaying = isPlaying,
                        onTrackClick = { clicked ->
                            playerConnection.play(clicked.toTrack(), displayedTracks.map { it.toTrack() })
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
    playerConnection: PlayerConnection = LocalPlayerConnection.current,
    modifier: Modifier = Modifier
) {
    val realConnection = playerConnection as RealPlayerConnection
    val apiClient = realConnection.apiClient
    val currentTrack by playerConnection.currentTrack.collectAsState()
    val currentTrackDto = currentTrack?.toSummaryDto()
    val status by playerConnection.status.collectAsState()
    val playbackInfo by playerConnection.playbackInfo.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val shuffleMode by playerConnection.shuffleMode.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var serverConnected by remember { mutableStateOf(false) }
    var serverTracks by remember { mutableStateOf<List<TrackSummaryDto>>(emptyList()) }

    LaunchedEffect(searchQuery, selectedFilter, apiClient.baseUrl) {
        isSearching = true
        delay(300)

        val providerParam = when (selectedFilter) {
            "Apple Music" -> "apple_music"
            "Qobuz" -> "qobuz"
            else -> null
        }

        val result = apiClient.search(
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
                playerConnection = playerConnection,
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
                contentBottomPadding = if (currentTrackDto != null) 96.dp else 24.dp
            )
        }

        AnimatedVisibility(
            visible = currentTrackDto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            currentTrackDto?.let { trackDto ->
                MiniPlayerBar(
                    track = trackDto,
                    playbackInfo = playbackInfo,
                    status = status,
                    positionMs = playerConnection.currentPositionMs,
                    durationMs = playerConnection.durationMs,
                    artworkUrl = apiClient.getArtworkUrl(trackDto, 200),
                    onTogglePlayPause = { playerConnection.togglePlayPause() },
                    onPlayNext = { playerConnection.playNext() },
                    onPlayPrevious = { playerConnection.playPrevious() },
                    onOpenNowPlaying = { isNowPlayingOpen = true },
                    onDismiss = { playerConnection.stopAndDismiss() },
                    canSkipNext = canSkipNext,
                    canSkipPrevious = canSkipPrevious
                )
            }
        }

        AnimatedVisibility(
            visible = isNowPlayingOpen && currentTrackDto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentTrackDto?.let { trackDto ->
                NowPlayingSheet(
                    track = trackDto,
                    playbackInfo = playbackInfo,
                    status = status,
                    positionMs = playerConnection.currentPositionMs,
                    durationMs = playerConnection.durationMs,
                    artworkUrl = apiClient.getArtworkUrl(trackDto, 600),
                    serverUrl = apiClient.baseUrl,
                    isDevMode = realConnection.isDevMode,
                    onTogglePlayPause = { playerConnection.togglePlayPause() },
                    onSeekTo = { pos -> playerConnection.seekTo(pos) },
                    onPlayNext = { playerConnection.playNext() },
                    onPlayPrevious = { playerConnection.playPrevious() },
                    onClose = { isNowPlayingOpen = false },
                    onOpenSettings = { isSettingsOpen = true },
                    isShuffle = shuffleMode,
                    onToggleShuffle = { playerConnection.setShuffleMode(!shuffleMode) },
                    repeatMode = repeatMode,
                    onToggleRepeat = {
                        val next = when (repeatMode) {
                            org.shilpo.peerless.model.RepeatMode.OFF -> org.shilpo.peerless.model.RepeatMode.ALL
                            org.shilpo.peerless.model.RepeatMode.ALL -> org.shilpo.peerless.model.RepeatMode.ONE
                            org.shilpo.peerless.model.RepeatMode.ONE -> org.shilpo.peerless.model.RepeatMode.OFF
                        }
                        playerConnection.setRepeatMode(next)
                    }
                )
            }
        }

        if (isSettingsOpen) {
            ServerSettingsDialog(
                currentServerUrl = apiClient.baseUrl,
                currentDevMode = realConnection.isDevMode,
                onSave = { newUrl, newDevMode ->
                    apiClient.baseUrl = newUrl
                    realConnection.isDevMode = newDevMode
                },
                onDismiss = { isSettingsOpen = false }
            )
        }
    }
}
