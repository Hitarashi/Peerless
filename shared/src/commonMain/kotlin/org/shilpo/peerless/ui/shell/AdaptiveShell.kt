package org.shilpo.peerless.ui.shell


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
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
import org.shilpo.peerless.lyrics.LyricsLoader
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.sync.LocalPlaybackSyncManager
import org.shilpo.peerless.tasks.LocalRipCoordinator
import org.shilpo.peerless.tasks.RipCoordinator
import org.shilpo.peerless.theme.ExpressiveMotion
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.LocalLiquidGlassState
import org.shilpo.peerless.theme.LocalWindowWidthSizeClass
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.WindowWidthSizeClass
import org.shilpo.peerless.theme.liquidGlassSource
import org.shilpo.peerless.theme.rememberLiquidGlassState
import org.shilpo.peerless.ui.components.FloatingNavigationToolbar
import org.shilpo.peerless.ui.components.HomeMorphIcon
import org.shilpo.peerless.ui.components.LibraryMorphIcon
import org.shilpo.peerless.ui.components.LocalToastNotifier
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
import org.shilpo.peerless.ui.components.RipActivityPane
import org.shilpo.peerless.ui.components.SearchMorphIcon
import org.shilpo.peerless.ui.components.ServerSettingsDialog
import org.shilpo.peerless.ui.components.SettingsMorphIcon
import org.shilpo.peerless.ui.components.SupportingPaneQuickSwitcherPill
import org.shilpo.peerless.ui.navigation.NavigationDestination
import org.shilpo.peerless.ui.posture.LocalWindowPostureProvider
import org.shilpo.peerless.ui.posture.WindowPosture
import org.shilpo.peerless.ui.posture.WindowPostureKind
import org.shilpo.peerless.ui.screens.AuthOnboardingScreen
import org.shilpo.peerless.ui.screens.HomeScreenContent
import org.shilpo.peerless.ui.screens.LastFmLoginScreen
import org.shilpo.peerless.ui.screens.LibraryScreen
import org.shilpo.peerless.ui.screens.ProfileScreen
import org.shilpo.peerless.ui.screens.SampleLosslessLibrary
import org.shilpo.peerless.ui.screens.SearchScreen
import org.shilpo.peerless.ui.screens.SettingsScreen

private data class ShellDisplayState(
    val currentDestination: NavigationDestination,
    val onSelectDestination: (NavigationDestination) -> Unit,
    val playerConnection: PlayerConnection,
    val currentTrackDto: TrackSummaryDto?,
    val status: PlaybackStatus,
    val serverConnected: Boolean,
    val searchQuery: String,
    val onQueryChange: (String) -> Unit,
    val selectedFilter: String,
    val onSelectFilter: (String) -> Unit,
    val displayedTracks: List<TrackSummaryDto>,
    val allTracks: List<TrackSummaryDto>,
    val onOpenNowPlaying: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenProfile: () -> Unit,
    val onRipClick: (TrackSummaryDto) -> Unit
)

private data class FoldRegion(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp
)

@Composable
fun AdaptiveShell(
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current
    val apiClient = LocalPeerlessApiClient.current
    val currentServerUrl by apiClient.baseUrlState.collectAsState()
    val sessionManager = LocalSessionManager.current
    val windowPostureProvider = LocalWindowPostureProvider.current
    val windowPosture by windowPostureProvider.posture.collectAsState()
    val sessionState by sessionManager.sessionState.collectAsState()
    val authenticatedUserId = (sessionState as? SessionState.Authenticated)?.user?.telegram_id
    val playbackSyncManager = LocalPlaybackSyncManager.current
    val sessionCheckScope = rememberCoroutineScope()

    var isProfileOpen by remember { mutableStateOf(false) }

    val isLastFmConnected by sessionManager.isLastFmConnected.collectAsState()

    if (sessionState is SessionState.Loading) {
        AuthLoadingScreen(modifier = modifier.fillMaxSize())
        return
    }

    if (sessionState is SessionState.VerificationFailed) {
        SessionVerificationFailedScreen(
            modifier = modifier.fillMaxSize(),
            onRetry = { sessionCheckScope.launch { sessionManager.checkExistingSession() } }
        )
        return
    }

    if (sessionState is SessionState.Unauthenticated || sessionState is SessionState.Connecting) {
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
    val lyricsScope = rememberCoroutineScope()
    val lyricsLoader = remember(apiClient, lyricsScope) { LyricsLoader(apiClient, lyricsScope) }
    LaunchedEffect(currentTrackDto?.id, currentServerUrl) {
        lyricsLoader.selectTrack(currentTrackDto?.id, currentServerUrl)
    }
    val status by playerConnection.status.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val queue by playerConnection.queue.collectAsState()
    val currentQueueIndex by playerConnection.currentIndex.collectAsState()
    val queueDtos = remember(queue) { queue.map { it.toSummaryDto() } }
    val shuffleMode by playerConnection.shuffleMode.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val volume by playerConnection.volume.collectAsState()
    val positionMs by playerConnection.positionMs.collectAsState()
    val spectrumFrame by playerConnection.spectrum.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()
    val bufferedPositionMs by playerConnection.bufferedPositionMs.collectAsState()
    val outputLatencyMs by playerConnection.outputLatencyMs.collectAsState()

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
        val paneLayout = supportingPaneLayout(maxWidth, supportingPaneWidth)
        val supportingPaneAvailable =
            paneLayout.isAvailable && windowPosture.kind == WindowPostureKind.FLAT
        val fullPlayerBounds = fullPlayerRegion(maxWidth, maxHeight, windowPosture)

        val coroutineScope = rememberCoroutineScope()
        val ripCoordinator = remember(apiClient, coroutineScope, playbackSyncManager) {
            RipCoordinator.getInstance(
                apiClient,
                coroutineScope,
                playbackSyncManager.remoteRipTasks
            )
        }
        LaunchedEffect(ripCoordinator, authenticatedUserId, currentServerUrl) {
            ripCoordinator.clearServerProjection()
            if (authenticatedUserId != null) {
                ripCoordinator.refreshServerTasks()
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val toastNotifier: (String) -> Unit = { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }

        val liquidGlassState = rememberLiquidGlassState()
        CompositionLocalProvider(
            LocalWindowWidthSizeClass provides windowSizeClass,
            LocalLiquidGlassState provides liquidGlassState,
            LocalRipCoordinator provides ripCoordinator,
            LocalToastNotifier provides toastNotifier
        ) {
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
                val onRipClick: (TrackSummaryDto) -> Unit = { track ->
                    coroutineScope.launch {
                        ripCoordinator.ripTrack(track)
                    }
                }

                val displayState = ShellDisplayState(
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

                when (windowPosture.kind) {
                    WindowPostureKind.BOOK -> BookPostureLayout(
                        state = displayState,
                        posture = windowPosture
                    )

                    WindowPostureKind.TABLETOP -> TabletopPostureLayout(
                        state = displayState,
                        posture = windowPosture
                    )

                    WindowPostureKind.FLAT -> when (windowSizeClass) {
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
                                activeSupportingPane = activeSupportingPane.takeIf { supportingPaneAvailable },
                                isSupportingPaneAvailable = supportingPaneAvailable,
                                onToggleSupportingPane = { pane ->
                                    activeSupportingPane =
                                        if (activeSupportingPane == pane) null else pane
                                },
                                onSelectSupportingPane = { pane ->
                                    activeSupportingPane = pane
                                },
                                supportingPaneWidth = paneLayout.width,
                                onSupportingPaneWidthChange = {
                                    supportingPaneWidth = it.coerceIn(280.dp, 560.dp)
                                },
                                playerConnection = playerConnection,
                                currentTrackDto = currentTrackDto,
                                lyricsLoader = lyricsLoader,
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
                            lyricsLoader = lyricsLoader,
                            playbackInfo = playerConnection.playbackInfo.collectAsState().value,
                            status = status,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            bufferedPositionMs = bufferedPositionMs,
                            outputLatencyMs = outputLatencyMs,
                            artworkUrl = apiClient.getArtworkUrl(trackDto, 600),
                            serverUrl = currentServerUrl,
                            onTogglePlayPause = { playerConnection.togglePlayPause() },
                            onSeekTo = { pos -> playerConnection.seekTo(pos) },
                            onPlayNext = { playerConnection.playNext() },
                            onPlayPrevious = { playerConnection.playPrevious() },
                            onClose = { isNowPlayingOpen = false },
                            onOpenSettings = { isSettingsOpen = true },
                            isShuffle = shuffleMode,
                            onToggleShuffle = { playerConnection.toggleShuffle() },
                            repeatMode = repeatMode,
                            onToggleRepeat = { playerConnection.cycleRepeatMode() },
                            spectrumFrame = spectrumFrame,
                            queueTracks = queueDtos,
                            currentQueueIndex = currentQueueIndex,
                            onPlayQueueItem = playerConnection::playQueueItem,
                            modifier = Modifier
                                .offset(x = fullPlayerBounds.x, y = fullPlayerBounds.y)
                                .size(fullPlayerBounds.width, fullPlayerBounds.height)
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

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuthLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadingIndicator()
            Text(
                text = "Checking your sign-in…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionVerificationFailedScreen(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Couldn't verify your sign-in",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your saved Telegram sign-in is still on this device. Check your connection or server, then try again.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val apiClient = LocalPeerlessApiClient.current
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
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

        val ripCoordinator = LocalRipCoordinator.current
        val activeTasksCount by (ripCoordinator?.activeCount
            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()
        var showRipActivitySheet by remember { mutableStateOf(false) }

        if (showRipActivitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showRipActivitySheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                RipActivityPane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                )
            }
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
                visible = activeTasksCount > 0,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "CompactChipSpin")
                val spinAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "CompactChipRotation"
                )

                Surface(
                    onClick = { showRipActivitySheet = true },
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.RipCloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer(rotationZ = spinAngle)
                        )
                        Text(
                            text = "Ripping: $activeTasksCount active",
                            style = SpecBadgeTypography.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

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
    isSupportingPaneAvailable: Boolean,
    onToggleSupportingPane: (SupportingPaneType) -> Unit,
    onSelectSupportingPane: (SupportingPaneType) -> Unit = onToggleSupportingPane,
    supportingPaneWidth: Dp = 340.dp,
    onSupportingPaneWidthChange: (Dp) -> Unit = {},
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    lyricsLoader: LyricsLoader,
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
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()
    val bufferedPositionMs by playerConnection.bufferedPositionMs.collectAsState()

    var displayedSupportingPane by remember { mutableStateOf(activeSupportingPane) }
    if (activeSupportingPane != null) {
        displayedSupportingPane = activeSupportingPane
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val playerSlotHeight by animateDpAsState(
            targetValue = if (currentTrackDto != null) 122.dp else 0.dp,
            label = "BottomPlayerSlotHeight"
        )
        val sidebarHeight = maxHeight - playerSlotHeight

        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ExpressiveWideNavigationRail(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                initialExpanded = true,
                modifier = Modifier.height(sidebarHeight)
                    .padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
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
                    contentBottomPadding = 16.dp + playerSlotHeight,
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
                        lyricsLoader = lyricsLoader,
                        positionMs = positionMs,
                        status = status,
                        isPlaying = isPlaying,
                        onClose = { onToggleSupportingPane(paneType) },
                        onSelectPane = onSelectSupportingPane,
                        currentWidth = supportingPaneWidth,
                        onWidthChange = onSupportingPaneWidthChange,
                        onResetWidth = {
                            onSupportingPaneWidthChange(340.dp)
                        },
                        modifier = Modifier
                            .width(supportingPaneWidth + 48.dp)
                            .height(sidebarHeight)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = currentTrackDto != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
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
                onToggleSupportingPane = onToggleSupportingPane.takeIf { isSupportingPaneAvailable },
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

@Composable
private fun BookPostureLayout(
    state: ShellDisplayState,
    posture: WindowPosture
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val hinge = posture.hingeBounds
        if (hinge == null) {
            CompactFoldRegion(state, FoldRegion(0.dp, 0.dp, maxWidth, maxHeight))
            return@BoxWithConstraints
        }

        val leftWidth = maxWidth * hinge.left.coerceIn(0f, 1f)
        val hingeWidth = maxWidth * (hinge.right - hinge.left).coerceAtLeast(0f)
        val rightWidth = maxWidth * (1f - hinge.right).coerceIn(0f, 1f)
        if (leftWidth >= 96.dp && rightWidth >= 280.dp) {
            CompositionLocalProvider(
                LocalWindowWidthSizeClass provides WindowWidthSizeClass.fromWidth(rightWidth)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.width(leftWidth).fillMaxHeight()) {
                        ExpressiveWideNavigationRail(
                            selectedDestination = state.currentDestination,
                            onSelectDestination = state.onSelectDestination,
                            initialExpanded = false,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    }
                    Spacer(modifier = Modifier.width(hingeWidth))
                    Column(modifier = Modifier.width(rightWidth).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            state.RenderDestination(contentBottomPadding = 16.dp)
                        }
                        state.RenderMiniPlayer(
                            isPairedWithNavigation = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            val useLeftRegion = leftWidth >= rightWidth
            CompactFoldRegion(
                state = state,
                region = if (useLeftRegion) {
                    FoldRegion(0.dp, 0.dp, leftWidth, maxHeight)
                } else {
                    FoldRegion(maxWidth * hinge.right, 0.dp, rightWidth, maxHeight)
                }
            )
        }
    }
}

@Composable
private fun TabletopPostureLayout(
    state: ShellDisplayState,
    posture: WindowPosture
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val hinge = posture.hingeBounds
        if (hinge == null) {
            CompactFoldRegion(state, FoldRegion(0.dp, 0.dp, maxWidth, maxHeight))
            return@BoxWithConstraints
        }

        val topHeight = maxHeight * hinge.top.coerceIn(0f, 1f)
        val hingeHeight = maxHeight * (hinge.bottom - hinge.top).coerceAtLeast(0f)
        val bottomHeight = maxHeight * (1f - hinge.bottom).coerceIn(0f, 1f)
        val requiredBottomHeight = if (state.currentTrackDto == null) 128.dp else 208.dp
        if (topHeight >= 240.dp && bottomHeight >= requiredBottomHeight) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topHeight)
                        .statusBarsPadding()
                ) {
                    state.RenderDestination(contentBottomPadding = 16.dp)
                }
                Spacer(modifier = Modifier.height(hingeHeight))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomHeight)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    state.RenderMiniPlayer(
                        isPairedWithNavigation = true,
                        modifier = Modifier
                            .widthIn(max = NavigationBarMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = NavigationBarHorizontalPadding)
                            .padding(bottom = MiniPlayerBottomSpacing)
                    )
                    FloatingNavigationToolbar(
                        items = NavigationDestination.MainDestinations,
                        selectedDestination = state.currentDestination,
                        onSelectDestination = state.onSelectDestination,
                        isPairedWithMiniPlayer = state.currentTrackDto != null,
                        modifier = Modifier
                            .widthIn(max = NavigationBarMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = NavigationBarHorizontalPadding)
                    )
                }
            }
        } else {
            val useTopRegion = topHeight >= bottomHeight
            CompactFoldRegion(
                state = state,
                region = if (useTopRegion) {
                    FoldRegion(0.dp, 0.dp, maxWidth, topHeight)
                } else {
                    FoldRegion(0.dp, maxHeight * hinge.bottom, maxWidth, bottomHeight)
                }
            )
        }
    }
}

@Composable
private fun CompactFoldRegion(
    state: ShellDisplayState,
    region: FoldRegion
) {
    CompositionLocalProvider(
        LocalWindowWidthSizeClass provides WindowWidthSizeClass.fromWidth(region.width)
    ) {
        CompactLayout(
            currentDestination = state.currentDestination,
            onSelectDestination = state.onSelectDestination,
            playerConnection = state.playerConnection,
            currentTrackDto = state.currentTrackDto,
            status = state.status,
            serverConnected = state.serverConnected,
            searchQuery = state.searchQuery,
            onQueryChange = state.onQueryChange,
            selectedFilter = state.selectedFilter,
            onSelectFilter = state.onSelectFilter,
            displayedTracks = state.displayedTracks,
            allTracks = state.allTracks,
            onOpenNowPlaying = state.onOpenNowPlaying,
            onOpenSettings = state.onOpenSettings,
            onOpenProfile = state.onOpenProfile,
            onRipClick = state.onRipClick,
            modifier = Modifier
                .offset(x = region.x, y = region.y)
                .size(region.width, region.height)
        )
    }
}

@Composable
private fun ShellDisplayState.RenderDestination(contentBottomPadding: Dp) {
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
        contentBottomPadding = contentBottomPadding,
        onRipClick = onRipClick
    )
}

@Composable
private fun ShellDisplayState.RenderMiniPlayer(
    isPairedWithNavigation: Boolean,
    modifier: Modifier = Modifier
) {
    val track = currentTrackDto ?: return
    val apiClient = LocalPeerlessApiClient.current
    val positionMs by playerConnection.positionMs.collectAsState()
    val durationMs by playerConnection.durationMs.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val playbackInfo by playerConnection.playbackInfo.collectAsState()

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        MiniPlayerBar(
            track = track,
            playbackInfo = playbackInfo,
            status = status,
            positionMs = positionMs,
            durationMs = durationMs,
            artworkUrl = apiClient.getArtworkUrl(track, 200),
            onTogglePlayPause = { playerConnection.togglePlayPause() },
            onPlayNext = { playerConnection.playNext() },
            onPlayPrevious = { playerConnection.playPrevious() },
            onOpenNowPlaying = onOpenNowPlaying,
            onDismiss = { playerConnection.stopAndDismiss() },
            canSkipNext = canSkipNext,
            canSkipPrevious = canSkipPrevious,
            isPairedWithNavigation = isPairedWithNavigation,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun fullPlayerRegion(width: Dp, height: Dp, posture: WindowPosture): FoldRegion {
    val hinge = posture.hingeBounds ?: return FoldRegion(0.dp, 0.dp, width, height)
    return when (posture.kind) {
        WindowPostureKind.BOOK -> {
            val leftWidth = width * hinge.left.coerceIn(0f, 1f)
            val rightWidth = width * (1f - hinge.right).coerceIn(0f, 1f)
            if (rightWidth >= 280.dp || rightWidth >= leftWidth) {
                FoldRegion(width * hinge.right, 0.dp, rightWidth, height)
            } else {
                FoldRegion(0.dp, 0.dp, leftWidth, height)
            }
        }

        WindowPostureKind.TABLETOP -> {
            val topHeight = height * hinge.top.coerceIn(0f, 1f)
            val bottomHeight = height * (1f - hinge.bottom).coerceIn(0f, 1f)
            if (topHeight >= bottomHeight) {
                FoldRegion(0.dp, 0.dp, width, topHeight)
            } else {
                FoldRegion(0.dp, height * hinge.bottom, width, bottomHeight)
            }
        }

        WindowPostureKind.FLAT -> FoldRegion(0.dp, 0.dp, width, height)
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
            .width(48.dp)
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
    lyricsLoader: LyricsLoader,
    positionMs: Long,
    status: PlaybackStatus,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onSelectPane: (SupportingPaneType) -> Unit,
    currentWidth: Dp,
    onWidthChange: (Dp) -> Unit,
    onResetWidth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queue by playerConnection.queue.collectAsState()
    val queueDtos = remember(queue) { queue.map { it.toSummaryDto() } }
    val outputLatencyMs by playerConnection.outputLatencyMs.collectAsState()

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
                            SupportingPaneType.TRACK_CONTEXT -> PeerlessIcons.InfoFilled
                            SupportingPaneType.TASKS -> PeerlessIcons.RipCloudSync
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

                        val ripCoordinator = LocalRipCoordinator.current
                        val activeRipCount by (ripCoordinator?.activeCount
                            ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()
                        val headerBadgeCount = when (paneType) {
                            SupportingPaneType.QUEUE -> queueDtos.size
                            SupportingPaneType.TASKS -> activeRipCount
                            else -> 0
                        }

                        if (headerBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$headerBadgeCount",
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
                    .padding(if (paneType == SupportingPaneType.LYRICS) 0.dp else 12.dp)
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
                            currentTrack = currentTrackDto,
                            lyricsLoader = lyricsLoader,
                            positionMs = positionMs,
                            onSeekTo = playerConnection::seekTo,
                            isPlaying = isPlaying,
                            outputLatencyMs = outputLatencyMs
                        )
                    }

                    SupportingPaneType.TRACK_CONTEXT -> {
                        TrackContextPaneContent(
                            currentTrack = currentTrackDto,
                            playerConnection = playerConnection
                        )
                    }

                    SupportingPaneType.TASKS -> {
                        RipActivityPane()
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
    contentBottomPadding: Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null
) {
    // Single backdrop seam: everything behind the bottom players + nav toolbar
    // is captured here for the liquid glass system (no-op when glass is off).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassSource(LocalLiquidGlassState.current)
    ) {
        when (destination) {
            NavigationDestination.HOME -> {
                HomeScreenContent(
                    playerConnection = playerConnection,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    displayedTracks = displayedTracks,
                    allTracks = allTracks,
                    onNavigateToSearch = { onSelectDestination(NavigationDestination.SEARCH) },
                    onNavigateToSettings = { onSelectDestination(NavigationDestination.SETTINGS) },
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
}
