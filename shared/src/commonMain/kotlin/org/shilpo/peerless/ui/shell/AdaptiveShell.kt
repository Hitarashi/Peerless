package org.shilpo.peerless.ui.shell


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.ExpressiveMotion
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.LocalWindowWidthSizeClass
import org.shilpo.peerless.theme.WindowWidthSizeClass
import org.shilpo.peerless.ui.components.FloatingNavigationToolbar
import org.shilpo.peerless.ui.components.HomeMorphIcon
import org.shilpo.peerless.ui.components.LibraryMorphIcon
import org.shilpo.peerless.ui.components.MiniPlayerBar
import org.shilpo.peerless.ui.components.MiniPlayerBottomSpacing
import org.shilpo.peerless.ui.components.NavToggleMorphIcon
import org.shilpo.peerless.ui.components.NavigationBarBottomPadding
import org.shilpo.peerless.ui.components.NavigationBarHorizontalPadding
import org.shilpo.peerless.ui.components.NavigationBarMaxWidth
import org.shilpo.peerless.ui.components.NowPlayingSheet
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.PersistentBottomPlayer
import org.shilpo.peerless.ui.components.SearchMorphIcon
import org.shilpo.peerless.ui.components.ServerSettingsDialog
import org.shilpo.peerless.ui.components.SettingsMorphIcon
import org.shilpo.peerless.ui.components.SupportingPaneQuickSwitcherPill
import org.shilpo.peerless.ui.navigation.NavigationDestination
import org.shilpo.peerless.ui.screens.AuthOnboardingScreen
import org.shilpo.peerless.ui.screens.HomeScreenContent
import org.shilpo.peerless.ui.screens.LastFmLoginScreen
import org.shilpo.peerless.ui.screens.LibraryScreen
import org.shilpo.peerless.ui.screens.ProfileScreen
import org.shilpo.peerless.ui.screens.SampleLosslessLibrary
import org.shilpo.peerless.ui.screens.SearchScreen
import org.shilpo.peerless.ui.screens.SettingsScreen

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
            CanonicalDeduplicator.deduplicateTracks(SampleLosslessLibrary, apiClient.baseUrl)
                .map { it.toSummaryDto() }
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
                                activeSupportingPane =
                                    if (activeSupportingPane == pane) null else pane
                            },
                            onSelectSupportingPane = { pane ->
                                activeSupportingPane = pane
                            },
                            supportingPaneWidth = supportingPaneWidth,
                            onSupportingPaneWidthChange = {
                                supportingPaneWidth = it.coerceIn(280.dp, 560.dp)
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
                        animationSpec = tween(
                            300,
                            easing = ExpressiveMotion.EmphasizedAccelerateEasing
                        )
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
                        animationSpec = tween(
                            300,
                            easing = ExpressiveMotion.EmphasizedAccelerateEasing
                        )
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

                        PeerlessIcon(
                            icon = paneIcon,
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
            HomeScreenContent(
                playerConnection = playerConnection,
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
            SearchScreen(
                playerConnection = playerConnection,
                onOpenSettings = onOpenSettings,
                contentBottomPadding = contentBottomPadding,
                onRipClick = onRipClick
            )
        }

        NavigationDestination.LIBRARY -> {
            LibraryScreen(
                playerConnection = playerConnection,
                currentTrackDto = currentTrackDto,
                status = status,
                allTracks = displayedTracks,
                contentBottomPadding = contentBottomPadding
            )
        }

        NavigationDestination.SETTINGS -> {
            SettingsScreen(
                serverConnected = serverConnected,
                onOpenSettingsDialog = onOpenSettings,
                onOpenProfile = onOpenProfile,
                contentBottomPadding = contentBottomPadding
            )
        }
    }
}
