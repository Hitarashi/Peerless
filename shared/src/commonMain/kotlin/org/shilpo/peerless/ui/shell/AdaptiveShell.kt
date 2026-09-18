package org.shilpo.peerless.ui.shell

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.HomeExpressiveContent
import org.shilpo.peerless.ui.SampleLosslessLibrary
import org.shilpo.peerless.ui.components.*
import org.shilpo.peerless.ui.navigation.NavigationDestination
import org.shilpo.peerless.ui.screens.SearchScreen
import org.shilpo.peerless.ui.toTrackSummary

@Composable
fun AdaptiveShell(
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current
    val realConnection = playerConnection as RealPlayerConnection
    val apiClient = realConnection.apiClient

    val currentTrack by playerConnection.currentTrack.collectAsState()
    val currentTrackDto = currentTrack?.toSummaryDto()
    val status by playerConnection.status.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val queue by playerConnection.queue.collectAsState()
    val queueDtos = remember(queue) { queue.map { it.toSummaryDto() } }
    val shuffleMode by playerConnection.shuffleMode.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val volume by playerConnection.volume.collectAsState()

    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var activeSupportingPane by remember { mutableStateOf<SupportingPaneType?>(SupportingPaneType.QUEUE) }
    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
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
            val matchesQuery = searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true) ||
                    track.album.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowWidthSizeClass.fromWidth(maxWidth)

        CompositionLocalProvider(LocalWindowWidthSizeClass provides windowSizeClass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                val coroutineScope = rememberCoroutineScope()
                val onRipClick: (TrackSummaryDto) -> Unit = { track ->
                    coroutineScope.launch {
                        println("[Peerless] Initiating rip task for ${track.title} (${track.provider}:${track.track_id})")
                        apiClient.createRipTask(
                            provider = track.provider,
                            trackId = track.track_id,
                            codec = track.codec
                        ).onSuccess { resp ->
                            println("[Peerless] Rip task started: ${resp.task_id} (status: ${resp.status})")
                        }.onFailure { err ->
                            println("[Peerless] Rip task error: ${err.message}")
                        }
                    }
                }

                when (windowSizeClass) {
                    WindowWidthSizeClass.COMPACT -> {
                        CompactLayout(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it },
                            playerConnection = playerConnection,
                            currentTrackDto = currentTrackDto,
                            status = status,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            allTracks = activeLibrary,
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onRipClick = onRipClick
                        )
                    }

                    WindowWidthSizeClass.MEDIUM -> {
                        MediumLayout(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it },
                            playerConnection = playerConnection,
                            currentTrackDto = currentTrackDto,
                            status = status,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            allTracks = activeLibrary,
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onRipClick = onRipClick
                        )
                    }

                    WindowWidthSizeClass.EXPANDED -> {
                        ExpandedLayout(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it },
                            activeSupportingPane = activeSupportingPane,
                            onToggleSupportingPane = { pane ->
                                activeSupportingPane = if (activeSupportingPane == pane) null else pane
                            },
                            playerConnection = playerConnection,
                            currentTrackDto = currentTrackDto,
                            status = status,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            allTracks = activeLibrary,
                            volume = volume,
                            onVolumeChange = { playerConnection.setVolume(it) },
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
                            },
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onRipClick = onRipClick
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isNowPlayingOpen && currentTrackDto != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = ExpressiveMotion.EmphasizedEasing)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                    ) + fadeOut()
                ) {
                    currentTrackDto?.let { trackDto ->
                        NowPlayingSheet(
                            track = trackDto,
                            playbackInfo = playerConnection.playbackInfo.collectAsState().value,
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
    }
}

@Composable
private fun CompactLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = (playerConnection as RealPlayerConnection).apiClient
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            DestinationContent(
                destination = currentDestination,
                onSelectDestination = onSelectDestination,
                playerConnection = playerConnection,
                currentTrackDto = currentTrackDto,
                status = status,
                serverConnected = serverConnected,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                displayedTracks = displayedTracks,
                allTracks = allTracks,
                onOpenSettings = onOpenSettings,
                onOpenNowPlaying = onOpenNowPlaying,
                contentBottomPadding = if (currentTrackDto != null) 175.dp else 98.dp,
                onRipClick = onRipClick
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = NavigationBarBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = currentTrackDto != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.padding(bottom = MiniPlayerBottomSpacing)
            ) {
                currentTrackDto?.let { trackDto ->
                    MiniPlayerBar(
                        track = trackDto,
                        playbackInfo = playerConnection.playbackInfo.collectAsState().value,
                        status = status,
                        positionMs = playerConnection.currentPositionMs,
                        durationMs = playerConnection.durationMs,
                        artworkUrl = apiClient.getArtworkUrl(trackDto, 200),
                        onTogglePlayPause = { playerConnection.togglePlayPause() },
                        onPlayNext = { playerConnection.playNext() },
                        onPlayPrevious = { playerConnection.playPrevious() },
                        onOpenNowPlaying = onOpenNowPlaying,
                        onDismiss = { playerConnection.stopAndDismiss() },
                        canSkipNext = canSkipNext,
                        canSkipPrevious = canSkipPrevious,
                        isPairedWithNavigation = true,
                        modifier = Modifier
                            .widthIn(max = NavigationBarMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = NavigationBarHorizontalPadding)
                    )
                }
            }

            FloatingNavigationToolbar(
                items = NavigationDestination.MainDestinations,
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                isPairedWithMiniPlayer = currentTrackDto != null,
                modifier = Modifier
                    .widthIn(max = NavigationBarMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = NavigationBarHorizontalPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavDock(
    selectedDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            toolbarContentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = PillShape,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .height(58.dp)
    ) {
        NavigationDestination.PrimaryDestinations.forEach { dest ->
            val isSelected = dest == selectedDestination
            val pillColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent
            val iconTint =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.65f
                )
            val borderColor =
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f) else Color.Transparent

            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(pillColor)
                    .border(1.dp, borderColor, PillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectDestination(dest) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )

                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Text(
                            text = dest.title,
                            style = ExpressiveTypography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediumLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = (playerConnection as RealPlayerConnection).apiClient
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavigationRail(
                header = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .size(40.dp)
                            .clip(SquircleShapeSmall)
                            .background(
                                Brush.linearGradient(listOf(PrimaryDark, TertiaryDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.MusicNote,
                            contentDescription = "Peerless Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                containerColor = SurfaceContainerLowestDark,
                contentColor = OnSurfaceDark,
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDestination.PrimaryDestinations.forEach { dest ->
                    val isSelected = dest == currentDestination
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onSelectDestination(dest) },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.title
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                style = ExpressiveTypography.labelSmall
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = PrimaryDark,
                            selectedTextColor = PrimaryDark,
                            indicatorColor = PrimaryDark.copy(alpha = 0.20f),
                            unselectedIconColor = OnSurfaceVariantDark.copy(alpha = 0.65f),
                            unselectedTextColor = OnSurfaceVariantDark.copy(alpha = 0.65f)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding()
            ) {
                DestinationContent(
                    destination = currentDestination,
                    onSelectDestination = onSelectDestination,
                    playerConnection = playerConnection,
                    currentTrackDto = currentTrackDto,
                    status = status,
                    serverConnected = serverConnected,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    displayedTracks = displayedTracks,
                    allTracks = allTracks,
                    onOpenSettings = onOpenSettings,
                    onOpenNowPlaying = onOpenNowPlaying,
                    contentBottomPadding = if (currentTrackDto != null) 90.dp else 16.dp,
                    onRipClick = onRipClick
                )

            }
        }

        AnimatedVisibility(
            visible = currentTrackDto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentTrackDto?.let { trackDto ->
                MiniPlayerBar(
                    track = trackDto,
                    playbackInfo = playerConnection.playbackInfo.collectAsState().value,
                    status = status,
                    positionMs = playerConnection.currentPositionMs,
                    durationMs = playerConnection.durationMs,
                    artworkUrl = apiClient.getArtworkUrl(trackDto, 200),
                    onTogglePlayPause = { playerConnection.togglePlayPause() },
                    onPlayNext = { playerConnection.playNext() },
                    onPlayPrevious = { playerConnection.playPrevious() },
                    onOpenNowPlaying = onOpenNowPlaying,
                    onDismiss = { playerConnection.stopAndDismiss() },
                    canSkipNext = canSkipNext,
                    canSkipPrevious = canSkipPrevious,
                    isPairedWithNavigation = false,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    activeSupportingPane: SupportingPaneType?,
    onToggleSupportingPane: (SupportingPaneType) -> Unit,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    isShuffle: Boolean,
    onToggleShuffle: () -> Unit,
    repeatMode: org.shilpo.peerless.model.RepeatMode,
    onToggleRepeat: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = (playerConnection as RealPlayerConnection).apiClient
    val isRepeat = repeatMode != org.shilpo.peerless.model.RepeatMode.OFF

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PersistentNavigationDrawer(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                serverConnected = serverConnected,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                DestinationContent(
                    destination = currentDestination,
                    onSelectDestination = onSelectDestination,
                    playerConnection = playerConnection,
                    currentTrackDto = currentTrackDto,
                    status = status,
                    serverConnected = serverConnected,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    displayedTracks = displayedTracks,
                    allTracks = allTracks,
                    onOpenSettings = onOpenSettings,
                    onOpenNowPlaying = onOpenNowPlaying,
                    onToggleStats = { onToggleSupportingPane(SupportingPaneType.SIGNAL_PATH) },
                    contentBottomPadding = 16.dp,
                    onRipClick = onRipClick
                )
            }

            AnimatedVisibility(
                visible = activeSupportingPane != null,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(350, easing = ExpressiveMotion.EmphasizedEasing)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(280, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                ) + fadeOut()
            ) {
                activeSupportingPane?.let { paneType ->
                    SupportingPaneContainer(
                        paneType = paneType,
                        playerConnection = playerConnection,
                        currentTrackDto = currentTrackDto,
                        status = status,
                        onClose = { onToggleSupportingPane(paneType) },
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }

        PersistentBottomPlayer(
            track = currentTrackDto,
            playbackInfo = playerConnection.playbackInfo.collectAsState().value,
            status = status,
            positionMs = playerConnection.currentPositionMs,
            durationMs = playerConnection.durationMs,
            artworkUrl = currentTrackDto?.let { apiClient.getArtworkUrl(it, 200) } ?: "",
            onTogglePlayPause = { playerConnection.togglePlayPause() },
            onSeekTo = { pos -> playerConnection.seekTo(pos) },
            onPlayNext = { playerConnection.playNext() },
            onPlayPrevious = { playerConnection.playPrevious() },
            activeSupportingPane = activeSupportingPane,
            onToggleSupportingPane = onToggleSupportingPane,
            volume = volume,
            onVolumeChange = onVolumeChange,
            isShuffle = isShuffle,
            onToggleShuffle = onToggleShuffle,
            isRepeat = isRepeat,
            onToggleRepeat = onToggleRepeat,
            onOpenNowPlaying = onOpenNowPlaying
        )
    }
}

@Composable
private fun PersistentNavigationDrawer(
    selectedDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    serverConnected: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceContainerLowestDark)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, OutlineVariantDark.copy(alpha = 0.5f))
                ),
                shape = RectangleShape
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(SquircleShapeSmall)
                            .background(
                                Brush.linearGradient(listOf(PrimaryDark, TertiaryDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PEERLESS",
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "BIT-PERFECT STREAMING",
                            style = SpecBadgeTypography.copy(fontSize = 8.sp),
                            color = SecondaryDark,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceContainerDark)
                        .border(1.dp, OutlineVariantDark, PillShape)
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (serverConnected) SecondaryDark else LosslessGold)
                    )

                    Text(
                        text = if (serverConnected) "DAEMON ONLINE" else "LOCAL DUMP CACHE",
                        style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                        color = if (serverConnected) SecondaryDark else LosslessGold
                    )
                }

                HorizontalDivider(
                    color = OutlineVariantDark.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                NavigationDestination.PrimaryDestinations.forEach { dest ->
                    val isSelected = dest == selectedDestination
                    val containerColor = if (isSelected) PrimaryDark.copy(alpha = 0.16f) else Color.Transparent
                    val contentColor = if (isSelected) PrimaryDark else OnSurfaceVariantDark
                    val borderColor = if (isSelected) PrimaryDark.copy(alpha = 0.35f) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleShapeMedium)
                            .background(containerColor)
                            .border(1.dp, borderColor, SquircleShapeMedium)
                            .clickable { onSelectDestination(dest) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )

                            Column {
                                Text(
                                    text = dest.title,
                                    style = ExpressiveTypography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OnSurfaceDark else contentColor
                                )
                                Text(
                                    text = dest.subtitle,
                                    style = ExpressiveTypography.bodySmall.copy(fontSize = 10.sp),
                                    color = OnSurfaceVariantDark.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerDark.copy(alpha = 0.6f))
                    .border(1.dp, OutlineVariantDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AUDIO ENGINE",
                        style = SpecBadgeTypography.copy(fontSize = 8.sp),
                        color = SecondaryDark
                    )
                    Text(
                        text = "24-BIT / 192KHZ DIRECT",
                        style = SpecBadgeLargeTypography.copy(fontSize = 10.sp),
                        color = OnSurfaceDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportingPaneContainer(
    paneType: SupportingPaneType,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val realConnection = playerConnection as RealPlayerConnection
    val apiClient = realConnection.apiClient
    val playbackInfo by playerConnection.playbackInfo.collectAsState()
    val queue by playerConnection.queue.collectAsState()
    val queueDtos = remember(queue) { queue.map { it.toSummaryDto() } }

    Box(
        modifier = modifier
            .background(SurfaceContainerLowDark)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(OutlineVariantDark.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = RectangleShape
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val paneIcon = when (paneType) {
                        SupportingPaneType.QUEUE -> PeerlessIcons.Queue
                        SupportingPaneType.LYRICS -> PeerlessIcons.Lyrics
                        SupportingPaneType.SIGNAL_PATH -> PeerlessIcons.SignalPath
                    }

                    Icon(
                        imageVector = paneIcon,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = paneType.title,
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Close,
                        contentDescription = "Close supporting pane",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = OutlineVariantDark.copy(alpha = 0.4f))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                when (paneType) {
                    SupportingPaneType.QUEUE -> {
                        QueuePaneContent(
                            playerConnection = playerConnection,
                            queueDtos = queueDtos,
                            currentTrackDto = currentTrackDto,
                            status = status
                        )
                    }

                    SupportingPaneType.LYRICS -> {
                        LyricsPaneContent(
                            currentTrack = currentTrackDto
                        )
                    }

                    SupportingPaneType.SIGNAL_PATH -> {
                        SignalPathPaneContent(
                            track = currentTrackDto,
                            playbackInfo = playbackInfo,
                            serverUrl = apiClient.baseUrl,
                            isDevMode = realConnection.isDevMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueuePaneContent(
    playerConnection: PlayerConnection,
    queueDtos: List<TrackSummaryDto>,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus
) {
    val apiClient = (playerConnection as RealPlayerConnection).apiClient

    if (queueDtos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = PeerlessIcons.Queue,
                    contentDescription = null,
                    tint = OnSurfaceVariantDark.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Queue is empty",
                    style = ExpressiveTypography.bodyMedium,
                    color = OnSurfaceVariantDark
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = queueDtos, key = { it.id }) { trackDto ->
                val isPlaying = currentTrackDto?.id == trackDto.id
                TrackRow(
                    track = trackDto,
                    artworkUrl = apiClient.getArtworkUrl(trackDto, 120),
                    isPlaying = isPlaying && status == PlaybackStatus.PLAYING,
                    onTrackClick = { playerConnection.play(it.toTrack(), queueDtos.map { t -> t.toTrack() }) }
                )
            }
        }
    }
}

@Composable
private fun LyricsPaneContent(currentTrack: TrackSummaryDto?) {
    if (currentTrack == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Play a track to view synced lyrics",
                style = ExpressiveTypography.bodyMedium,
                color = OnSurfaceVariantDark
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTrack.title,
                style = ExpressiveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark,
                textAlign = TextAlign.Center
            )

            Text(
                text = currentTrack.artist,
                style = ExpressiveTypography.bodyMedium,
                color = PrimaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            val sampleLyrics = listOf(
                "Ticking away the moments that make up a dull day",
                "Fritter and waste the hours in an offhand way",
                "Kicking around on a piece of ground in your hometown",
                "Waiting for someone or something to show you the way",
                "Tired of lying in the sunshine, staying home to watch the rain"
            )

            sampleLyrics.forEachIndexed { index, line ->
                val isActive = index == 1
                Text(
                    text = line,
                    style = if (isActive) ExpressiveTypography.titleMedium else ExpressiveTypography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) PrimaryDark else OnSurfaceVariantDark.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SignalPathPaneContent(
    track: TrackSummaryDto?,
    playbackInfo: org.shilpo.peerless.model.PlaybackInfo?,
    serverUrl: String,
    isDevMode: Boolean
) {
    if (track == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active audio stream to inspect",
                style = ExpressiveTypography.bodyMedium,
                color = OnSurfaceVariantDark
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
                secondaryInfo = "Endpoint: $serverUrl • ${if (isDevMode) "Dev Mode Direct" else "Signed Ticket"}",
                accentColor = SecondaryDark
            )

            val codec = playbackInfo?.codec ?: track.codec
            val bitDepth = playbackInfo?.bit_depth ?: track.bit_depth
            val sampleRate = playbackInfo?.sample_rate ?: track.sample_rate
            SignalPathStageCard(
                stageNumber = "2",
                stageName = "CONTAINER & CODEC",
                primaryInfo = "${codec.uppercase()} Lossless",
                secondaryInfo = "${bitDepth ?: 24}-Bit • ${((sampleRate ?: 96000) / 1000.0)} kHz • 2.0 Stereo",
                accentColor = LosslessGold
            )

            SignalPathStageCard(
                stageNumber = "3",
                stageName = "PLATFORM DECODER",
                primaryInfo = "Native Multiplatform Audio Engine",
                secondaryInfo = "Bit-perfect PCM Uncompressed Buffer",
                accentColor = PrimaryDark
            )

            SignalPathStageCard(
                stageNumber = "4",
                stageName = "STREAM PIPE / CACHE",
                primaryInfo = "HTTP Chunk Buffer (Chunked Transfer)",
                secondaryInfo = "Latency: <120ms • Gapless Engine Active",
                accentColor = SecondaryDark
            )

            SignalPathStageCard(
                stageNumber = "5",
                stageName = "OUTPUT & SINK",
                primaryInfo = "Hardware Audio Sink",
                secondaryInfo = "High-Res Direct Path • No Resampling",
                accentColor = TertiaryDark
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
            .background(SurfaceContainerDark)
            .border(1.dp, OutlineVariantDark.copy(alpha = 0.5f), SquircleShapeSmall)
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
                    color = OnSurfaceDark
                )
                Text(
                    text = secondaryInfo,
                    style = ExpressiveTypography.bodySmall.copy(fontSize = 10.5.sp),
                    color = OnSurfaceVariantDark
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    onOpenSettings: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onToggleStats: (() -> Unit)? = null,
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    when (destination) {
        NavigationDestination.HOME -> {
            HomeDestinationView(
                playerConnection = playerConnection,
                currentTrackDto = currentTrackDto,
                status = status,
                serverConnected = serverConnected,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                displayedTracks = displayedTracks,
                allTracks = allTracks,
                onOpenSettings = onOpenSettings,
                onNavigateToSearch = { onSelectDestination(NavigationDestination.SEARCH) },
                onToggleStats = onToggleStats ?: onOpenNowPlaying,
                contentBottomPadding = contentBottomPadding,
                onRipClick = onRipClick
            )
        }

        NavigationDestination.SEARCH -> {
            SearchDestinationView(
                playerConnection = playerConnection,
                onOpenSettings = onOpenSettings,
                contentBottomPadding = contentBottomPadding,
                onRipClick = onRipClick
            )
        }

        NavigationDestination.LIBRARY -> {
            LibraryDestinationView(
                playerConnection = playerConnection,
                currentTrackDto = currentTrackDto,
                status = status,
                allTracks = displayedTracks,
                contentBottomPadding = contentBottomPadding
            )
        }

        NavigationDestination.SETTINGS -> {
            SettingsDestinationView(
                playerConnection = playerConnection,
                serverConnected = serverConnected,
                onOpenSettingsDialog = onOpenSettings,
                contentBottomPadding = contentBottomPadding
            )
        }
    }
}

@Composable
private fun HomeDestinationView(
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
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
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    HomeExpressiveContent(
        playerConnection = playerConnection,
        serverConnected = serverConnected,
        searchQuery = searchQuery,
        onQueryChange = onQueryChange,
        selectedFilter = selectedFilter,
        onSelectFilter = onSelectFilter,
        displayedTracks = displayedTracks,
        allTracks = allTracks,
        onOpenSettings = onOpenSettings,
        onNavigateToSearch = onNavigateToSearch,
        onToggleStats = onToggleStats,
        contentBottomPadding = contentBottomPadding,
        onRipClick = onRipClick
    )
}

@Composable
private fun SearchDestinationView(
    playerConnection: PlayerConnection,
    onOpenSettings: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    SearchScreen(
        playerConnection = playerConnection,
        onOpenSettings = onOpenSettings,
        contentBottomPadding = contentBottomPadding,
        onRipClick = onRipClick
    )
}

@Composable
private fun LibraryDestinationView(
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    allTracks: List<TrackSummaryDto>,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    val apiClient = (playerConnection as RealPlayerConnection).apiClient

    var selectedTab by remember { mutableStateOf("All Saved") }
    val tabs = listOf("All Saved", "Cached Downloads", "Playlists")

    val cachedOnly = remember(allTracks) { allTracks.filter { it.is_cached } }
    val tracksToShow = if (selectedTab == "Cached Downloads") cachedOnly else allTracks

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
                        .background(if (isSelected) PrimaryDark else SurfaceContainerDark)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) OnPrimaryDark else OnSurfaceVariantDark
                    )
                }
            }
        }

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
                val isPlaying = currentTrackDto?.id == track.id &&
                        status == PlaybackStatus.PLAYING

                TrackRow(
                    track = track,
                    artworkUrl = apiClient.getArtworkUrl(track, 200),
                    isPlaying = isPlaying,
                    onTrackClick = { clicked ->
                        playerConnection.play(clicked.toTrack(), tracksToShow.map { it.toTrack() })
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsDestinationView(
    playerConnection: PlayerConnection,
    serverConnected: Boolean,
    onOpenSettingsDialog: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    val realConnection = playerConnection as RealPlayerConnection
    val serverUrl = realConnection.apiClient.baseUrl
    val isDevMode = realConnection.isDevMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Configuration",
            style = ExpressiveTypography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceDark
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(SurfaceContainerDark)
                .border(1.dp, OutlineVariantDark, SquircleShapeMedium)
                .clickable { onOpenSettingsDialog() }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Streaming Daemon",
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = if (serverConnected) "ONLINE" else "DISCONNECTED",
                        style = SpecBadgeTypography,
                        color = if (serverConnected) SecondaryDark else LosslessGold
                    )
                }

                Text(
                    text = "Endpoint: $serverUrl",
                    style = ExpressiveTypography.bodyMedium,
                    color = OnSurfaceVariantDark
                )

                Text(
                    text = if (isDevMode) "Dev Mode Direct Stream Active (Bypasses Telegram OTP)" else "Production Ticket Auth Active",
                    style = ExpressiveTypography.bodySmall,
                    color = if (isDevMode) LosslessGold else PrimaryDark
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(SurfaceContainerDark)
                .border(1.dp, OutlineVariantDark, SquircleShapeMedium)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Audio Engine Capabilities",
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Text(
                    text = "• Hi-Res PCM 24-bit / 192kHz output\n• FLAC & Apple Lossless (ALAC) gapless playback\n• Poweramp Signal Path Real-Time Inspector",
                    style = ExpressiveTypography.bodyMedium,
                    color = OnSurfaceVariantDark
                )
            }
        }
    }
}
