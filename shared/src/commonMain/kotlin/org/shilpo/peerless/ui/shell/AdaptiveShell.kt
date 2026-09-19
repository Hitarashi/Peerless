package org.shilpo.peerless.ui.shell

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.SessionState
import org.shilpo.peerless.library.LocalFavoritesManager
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.playTrack
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.HomeExpressiveContent
import org.shilpo.peerless.ui.SampleLosslessLibrary
import org.shilpo.peerless.ui.components.*
import org.shilpo.peerless.ui.navigation.NavigationDestination
import org.shilpo.peerless.ui.screens.AuthOnboardingScreen
import org.shilpo.peerless.ui.screens.LastFmLoginScreen
import org.shilpo.peerless.ui.screens.ProfileScreen
import org.shilpo.peerless.ui.screens.SearchScreen
import kotlin.ranges.coerceIn

@Composable
fun AdaptiveShell(
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current
    val apiClient = LocalPeerlessApiClient.current
    val sessionManager = LocalSessionManager.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val hasCredentials = sessionManager.hasSavedCredentials()

    var isProfileOpen by remember { mutableStateOf(false) }

    val isLastFmConnected by sessionManager.isLastFmConnected.collectAsState()

    if (sessionState is SessionState.Unauthenticated || (sessionState is SessionState.Loading && !hasCredentials)) {
        AuthOnboardingScreen(modifier = modifier.fillMaxSize())
        return
    }

    if (!isLastFmConnected) {
        LastFmLoginScreen(
            modifier = modifier.fillMaxSize(),
            onLogin = sessionManager::loginLastFm
        )
        return
    }

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
    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()
    val bufferedPositionMs by playerConnection.bufferedPositionMs.collectAsState()

    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var activeSupportingPane by remember { mutableStateOf<SupportingPaneType?>(SupportingPaneType.QUEUE) }
    var supportingPaneWidth by remember { mutableStateOf(340.dp) }
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
            val canonicalList = CanonicalDeduplicator.deduplicate(
                cachedTracks = response.cached,
                liveTracks = response.live,
                baseUrl = apiClient.baseUrl
            )
            serverTracks = canonicalList.map { it.toSummaryDto() }
            isSearching = false
        }.onFailure {
            serverConnected = false
            isSearching = false
        }
    }

    val activeLibrary = remember(serverTracks, apiClient.baseUrl) {
        if (serverTracks.isNotEmpty()) {
            serverTracks
        } else {
            CanonicalDeduplicator.deduplicateTracks(SampleLosslessLibrary, apiClient.baseUrl).map { it.toSummaryDto() }
        }
    }
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
                            onOpenProfile = { isProfileOpen = true },
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
                            onOpenProfile = { isProfileOpen = true },
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
                            onSelectSupportingPane = { pane ->
                                activeSupportingPane = pane
                            },
                            supportingPaneWidth = supportingPaneWidth,
                            onSupportingPaneWidthChange = { supportingPaneWidth = it.coerceIn(280.dp, 560.dp) },
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
                            onOpenProfile = { isProfileOpen = true },
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
                            positionMs = positionMs,
                            durationMs = durationMs,
                            bufferedPositionMs = bufferedPositionMs,
                            artworkUrl = apiClient.getArtworkUrl(trackDto, 600),
                            serverUrl = apiClient.baseUrl,
                            onTogglePlayPause = { playerConnection.togglePlayPause() },
                            onSeekTo = { pos -> playerConnection.seekTo(pos) },
                            onPlayNext = { playerConnection.playNext() },
                            onPlayPrevious = { playerConnection.playPrevious() },
                            onClose = { isNowPlayingOpen = false },
                            onOpenSettings = { isSettingsOpen = true },
                            isShuffle = shuffleMode,
                            onToggleShuffle = { playerConnection.toggleShuffle() },
                            repeatMode = repeatMode,
                            onToggleRepeat = { playerConnection.cycleRepeatMode() }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isProfileOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = ExpressiveMotion.EmphasizedEasing)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                    ) + fadeOut()
                ) {
                    ProfileScreen(
                        onClose = { isProfileOpen = false }
                    )
                }

                if (isSettingsOpen) {
                    ServerSettingsDialog(
                        currentServerUrl = apiClient.baseUrl,
                        onSave = { newUrl ->
                            apiClient.baseUrl = newUrl
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
    onOpenProfile: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = LocalPeerlessApiClient.current
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()

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
                onOpenProfile = onOpenProfile,
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
                        positionMs = positionMs,
                        durationMs = durationMs,
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
    onOpenProfile: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = LocalPeerlessApiClient.current
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ExpressiveWideNavigationRail(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                initialExpanded = false,
                modifier = Modifier.fillMaxHeight()
            )

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
                    onOpenProfile = onOpenProfile,
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
                    positionMs = positionMs,
                    durationMs = durationMs,
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
    onSelectSupportingPane: (SupportingPaneType) -> Unit = onToggleSupportingPane,
    supportingPaneWidth: Dp = 340.dp,
    onSupportingPaneWidthChange: (Dp) -> Unit = {},
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
    onOpenProfile: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    val apiClient = LocalPeerlessApiClient.current
    val isRepeat = repeatMode != org.shilpo.peerless.model.RepeatMode.OFF

    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()
    val bufferedPositionMs by playerConnection.bufferedPositionMs.collectAsState()

    var displayedSupportingPane by remember { mutableStateOf(activeSupportingPane) }
    if (activeSupportingPane != null) {
        displayedSupportingPane = activeSupportingPane
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ExpressiveWideNavigationRail(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                initialExpanded = true,
                modifier = Modifier.fillMaxHeight().padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
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
                    onOpenProfile = onOpenProfile,
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
                    animationSpec = spring(
                        dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
                        stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness,
                        visibilityThreshold = IntOffset(1, 1)
                    )
                ) + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
                        stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness,
                        visibilityThreshold = IntSize(1, 1)
                    ),
                    expandFrom = Alignment.End,
                    clip = false
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = spring(
                        dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
                        stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness,
                        visibilityThreshold = IntOffset(1, 1)
                    )
                ) + shrinkHorizontally(
                    animationSpec = spring(
                        dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
                        stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness,
                        visibilityThreshold = IntSize(1, 1)
                    ),
                    shrinkTowards = Alignment.End,
                    clip = false
                )
            ) {
                val paneType = activeSupportingPane ?: displayedSupportingPane
                if (paneType != null) {
                    SupportingPaneContainer(
                        paneType = paneType,
                        playerConnection = playerConnection,
                        currentTrackDto = currentTrackDto,
                        status = status,
                        onClose = { onToggleSupportingPane(paneType) },
                        onSelectPane = onSelectSupportingPane,
                        currentWidth = supportingPaneWidth,
                        onWidthChange = onSupportingPaneWidthChange,
                        onResetWidth = {
                            onSupportingPaneWidthChange(340.dp)
                        },
                        modifier = Modifier
                            .width(supportingPaneWidth)
                            .fillMaxHeight()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = currentTrackDto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            PersistentBottomPlayer(
                track = currentTrackDto,
                playbackInfo = playerConnection.playbackInfo.collectAsState().value,
                status = status,
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
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
                onOpenNowPlaying = onOpenNowPlaying,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveWideNavigationRail(
    selectedDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    initialExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val railState = rememberWideNavigationRailState(
        initialValue = if (initialExpanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )
    val isExpanded = railState.targetValue == WideNavigationRailValue.Expanded
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val headerWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 96.dp
    )

    val railShape = remember {
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
    }

    WideNavigationRail(
        state = railState,
        shape = railShape,
        colors = WideNavigationRailDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        header = {
            Box(
                modifier = Modifier
                    .width(headerWidth)
                    .padding(top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    modifier = Modifier.padding(start = 24.dp),
                    onClick = {
                        coroutineScope.launch {
                            if (isExpanded) {
                                railState.collapse()
                            } else {
                                railState.expand()
                            }
                        }
                    }
                ) {
                    NavToggleMorphIcon(
                        isExpanded = isExpanded,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier
            .clip(railShape)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDestination.PrimaryDestinations.forEach { dest ->
            val isSelected = dest == selectedDestination
            WideNavigationRailItem(
                selected = isSelected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    when (dest) {
                        NavigationDestination.HOME -> {
                            HomeMorphIcon(
                                selected = isSelected,
                                size = 24.dp,
                                contentDescription = dest.title,
                            )
                        }

                        NavigationDestination.SEARCH -> {
                            SearchMorphIcon(
                                selected = isSelected,
                                size = 24.dp,
                                contentDescription = dest.title,
                            )
                        }

                        NavigationDestination.LIBRARY -> {
                            LibraryMorphIcon(
                                selected = isSelected,
                                size = 24.dp,
                                contentDescription = dest.title,
                            )
                        }

                        NavigationDestination.SETTINGS -> {
                            SettingsMorphIcon(
                                selected = isSelected,
                                size = 24.dp,
                                contentDescription = dest.title,
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = dest.title,
                        style = ExpressiveTypography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                railExpanded = isExpanded,
                colors = WideNavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = if (isExpanded) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary,
                    selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SupportingPaneSplitter(
    currentWidth: Dp,
    onWidthChange: (Dp) -> Unit,
    onResetWidth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }

    val isActive = isHovered || isDragging
    val handleColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(150),
        label = "SplitterHandleColor"
    )
    val handleWidth by animateDpAsState(
        targetValue = if (isActive) 6.dp else 4.dp,
        animationSpec = tween(150),
        label = "SplitterHandleWidth"
    )

    var splitterCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lastClickUptime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerHoverIcon(PointerIcon.Crosshair)
            .hoverable(interactionSource)
            .onGloballyPositioned { splitterCoordinates = it }
            .pointerInput(density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val coords = splitterCoordinates
                    val startRootX = coords?.localToWindow(down.position)?.x
                        ?: coords?.localToRoot(down.position)?.x
                        ?: down.position.x
                    val startWidth = currentWidth
                    isDragging = true

                    var dragged = false
                    var finalUptime = down.uptimeMillis

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            if (change != null) {
                                finalUptime = change.uptimeMillis
                            }
                            isDragging = false
                            break
                        }
                        finalUptime = change.uptimeMillis
                        val currentCoords = splitterCoordinates
                        val currentRootX = currentCoords?.localToWindow(change.position)?.x
                            ?: currentCoords?.localToRoot(change.position)?.x
                            ?: change.position.x
                        val deltaPx = currentRootX - startRootX
                        if (kotlin.math.abs(deltaPx) > 0.5f) {
                            dragged = true
                            val deltaDp = with(density) { deltaPx.toDp() }
                            onWidthChange((startWidth - deltaDp).coerceIn(280.dp, 560.dp))
                            change.consume()
                        }
                    }

                    if (!dragged) {
                        if (finalUptime - lastClickUptime < 350L) {
                            onResetWidth()
                            lastClickUptime = 0L
                        } else {
                            lastClickUptime = finalUptime
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(handleWidth)
                .height(48.dp)
                .clip(CircleShape)
                .background(handleColor)
        )
    }
}

@Composable
private fun SupportingPaneContainer(
    paneType: SupportingPaneType,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    onClose: () -> Unit,
    onSelectPane: (SupportingPaneType) -> Unit,
    currentWidth: Dp,
    onWidthChange: (Dp) -> Unit,
    onResetWidth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apiClient = LocalPeerlessApiClient.current
    val playbackInfo by playerConnection.playbackInfo.collectAsState()
    val queue by playerConnection.queue.collectAsState()
    val queueDtos = remember(queue) { queue.map { it.toSummaryDto() } }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SupportingPaneSplitter(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
            onResetWidth = onResetWidth
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 2.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Floating Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val paneIcon = when (paneType) {
                            SupportingPaneType.QUEUE -> PeerlessIcons.Queue
                            SupportingPaneType.LYRICS -> PeerlessIcons.Lyrics
                            SupportingPaneType.SIGNAL_PATH -> PeerlessIcons.SignalPath
                        }

                        Icon(
                            imageVector = paneIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = paneType.title,
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (paneType == SupportingPaneType.QUEUE && queueDtos.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${queueDtos.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    SupportingPaneQuickSwitcherPill(
                        activeSupportingPane = paneType,
                        onToggleSupportingPane = { type ->
                            if (type == paneType) {
                                onClose()
                            } else {
                                onSelectPane(type)
                            }
                        }
                    )
                }
            }

            // Floating Content Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                            serverUrl = apiClient.baseUrl
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
    val apiClient = LocalPeerlessApiClient.current

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items = queueDtos, key = { it.id }) { trackDto ->
                val isCurrent = currentTrackDto?.id == trackDto.id
                val isPlaying = isCurrent && status == PlaybackStatus.PLAYING

                TrackRow(
                    track = trackDto,
                    artworkUrl = apiClient.getArtworkUrl(trackDto, 120),
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    onTrackClick = {
                        if (isCurrent) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection.play(it.toTrack(), queueDtos.map { t -> t.toTrack() })
                        }
                    },
                    showArtworkOverlay = false
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = currentTrack.artist,
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
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
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.5f
                    ),
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), SquircleShapeSmall)
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
    onOpenProfile: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onToggleStats: (() -> Unit)? = null,
    contentBottomPadding: Dp,
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
                onOpenProfile = onOpenProfile,
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
    contentBottomPadding: Dp,
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
    contentBottomPadding: Dp,
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
        CanonicalDeduplicator.deduplicateTracks(rawTracks, apiClient.baseUrl).map { it.toSummaryDto() }
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
                            Icon(
                                imageVector = PeerlessIcons.Heart,
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

@Composable
private fun SettingsDestinationView(
    playerConnection: PlayerConnection,
    serverConnected: Boolean,
    onOpenSettingsDialog: () -> Unit,
    onOpenProfile: () -> Unit,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    val serverUrl = apiClient.baseUrl

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
            color = MaterialTheme.colorScheme.onSurface
        )

        // Telegram Account & Telemetry Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), SquircleShapeMedium)
                .clickable { onOpenProfile() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Telegram Account & Telemetry",
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View active device sessions, daemon metrics, and logout",
                            style = ExpressiveTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = PeerlessIcons.OpenInNew,
                    contentDescription = "Open profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SquircleShapeMedium)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (serverConnected) "ONLINE" else "DISCONNECTED",
                        style = SpecBadgeTypography,
                        color = if (serverConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    )
                }

                Text(
                    text = if (serverUrl.isNotBlank()) "Endpoint: $serverUrl" else "Endpoint: Unconfigured",
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Verified Session & HMAC Ticket Auth Active",
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SquircleShapeMedium)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Audio Engine Capabilities",
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Hi-Res PCM 24-bit / 192kHz output\n• FLAC & Apple Lossless (ALAC) gapless playback\n• Poweramp Signal Path Real-Time Inspector",
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
