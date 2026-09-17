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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerState
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.SampleLosslessLibrary
import org.shilpo.peerless.ui.components.*
import org.shilpo.peerless.ui.navigation.NavigationDestination
import org.shilpo.peerless.ui.toTrackSummary

/**
 * Multiplatform Adaptive Shell implementing Google Large Screen Guidelines & ADR 0002.
 *
 * Viewport Form Factors:
 * - Compact (< 600dp, Mobile):
 *     Content area + Floating docked MiniPlayer + Floating pill navigation dock (FloatingNavDock) + NowPlayingSheet
 * - Medium (600dp .. 839dp, Foldable/Tablet):
 *     Vertical NavigationRail on start side + Center content + MiniPlayerBar docked at bottom
 * - Expanded (>= 840dp, Desktop/Landscape Tablet):
 *     Canonical Supporting Pane Layout (Persistent Left Drawer + Center Content + Right Supporting Pane + Persistent Bottom Player)
 */
@Composable
fun AdaptiveShell(
    coordinator: PlaybackCoordinator = remember { PlaybackCoordinator() },
    modifier: Modifier = Modifier
) {
    val playerState by coordinator.state.collectAsState()

    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var activeSupportingPane by remember { mutableStateOf<SupportingPaneType?>(SupportingPaneType.QUEUE) }
    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Volume state for desktop player
    var volume by remember { mutableFloatStateOf(0.85f) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    // Search and catalog state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }
    var serverConnected by remember { mutableStateOf(false) }
    var serverTracks by remember { mutableStateOf<List<TrackSummaryDto>>(emptyList()) }

    // Query server on filter or query change
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
                    .background(BackgroundDark)
            ) {
                when (windowSizeClass) {
                    WindowWidthSizeClass.COMPACT -> {
                        CompactLayout(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it },
                            coordinator = coordinator,
                            playerState = playerState,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    }

                    WindowWidthSizeClass.MEDIUM -> {
                        MediumLayout(
                            currentDestination = currentDestination,
                            onSelectDestination = { currentDestination = it },
                            coordinator = coordinator,
                            playerState = playerState,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true }
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
                            coordinator = coordinator,
                            playerState = playerState,
                            serverConnected = serverConnected,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            displayedTracks = displayedTracks,
                            volume = volume,
                            onVolumeChange = { volume = it },
                            isShuffle = isShuffle,
                            onToggleShuffle = { isShuffle = !isShuffle },
                            isRepeat = isRepeat,
                            onToggleRepeat = { isRepeat = !isRepeat },
                            onOpenNowPlaying = { isNowPlayingOpen = true },
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    }
                }

                // Full-screen expandable Now Playing Sheet modal
                AnimatedVisibility(
                    visible = isNowPlayingOpen && playerState.currentTrack != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = ExpressiveMotion.EmphasizedEasing)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                    ) + fadeOut()
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
    }
}

// =========================================================================
// 1. COMPACT VIEWPORT (< 600dp): Floating Dock + Clearance MiniPlayer
// =========================================================================
@Composable
private fun CompactLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Destination Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            DestinationContent(
                destination = currentDestination,
                coordinator = coordinator,
                playerState = playerState,
                serverConnected = serverConnected,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                displayedTracks = displayedTracks,
                onOpenSettings = onOpenSettings,
                // Extra bottom padding for MiniPlayer + Floating Dock clearance
                contentBottomPadding = if (playerState.currentTrack != null) 160.dp else 90.dp
            )
        }

        // Floating Dock & MiniPlayer Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating MiniPlayerBar with 8dp floating clearance above dock
            AnimatedVisibility(
                visible = playerState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.padding(bottom = 8.dp)
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
                        onOpenNowPlaying = onOpenNowPlaying,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Floating Pill Navigation Dock (LastWave-Native inspiration)
            FloatingNavDock(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination
            )
        }
    }
}

/**
 * Floating pill navigation dock hovering near the bottom with 4 primary destinations.
 * Styled with LiquidGlass translucent surface and frosted gradient borders.
 */
@Composable
fun FloatingNavDock(
    selectedDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        shape = PillShape,
        containerColor = LiquidGlassDefaults.ElevatedContainerColor,
        borderBrush = LiquidGlassDefaults.BorderBrush,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationDestination.PrimaryDestinations.forEach { dest ->
                val isSelected = dest == selectedDestination
                val pillColor = if (isSelected) PrimaryDark.copy(alpha = 0.22f) else Color.Transparent
                val iconTint = if (isSelected) PrimaryDark else OnSurfaceVariantDark.copy(alpha = 0.65f)
                val borderColor = if (isSelected) PrimaryDark.copy(alpha = 0.40f) else Color.Transparent

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
                                color = PrimaryDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. MEDIUM VIEWPORT (600dp .. 839dp): Vertical NavigationRail + Docked MiniPlayer
// =========================================================================
@Composable
private fun MediumLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Vertical NavigationRail on the start side
            LiquidGlassSurface(
                shape = RectangleShape,
                containerColor = SurfaceContainerLowestDark,
                borderBrush = Brush.horizontalGradient(
                    listOf(OutlineVariantDark.copy(alpha = 0.5f), Color.Transparent)
                ),
                borderWidth = 1.dp,
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Brand Icon
                    Box(
                        modifier = Modifier
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

                    // Destinations
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NavigationDestination.PrimaryDestinations.forEach { dest ->
                            val isSelected = dest == currentDestination
                            IconButton(
                                onClick = { onSelectDestination(dest) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(SquircleShapeSmall)
                                    .background(
                                        if (isSelected) PrimaryDark.copy(alpha = 0.20f) else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryDark.copy(alpha = 0.45f) else Color.Transparent,
                                        SquircleShapeSmall
                                    )
                            ) {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.title,
                                    tint = if (isSelected) PrimaryDark else OnSurfaceVariantDark.copy(alpha = 0.65f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Server status indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (serverConnected) SecondaryDark else LosslessGold)
                    )
                }
            }

            // Primary Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding()
            ) {
                DestinationContent(
                    destination = currentDestination,
                    coordinator = coordinator,
                    playerState = playerState,
                    serverConnected = serverConnected,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    displayedTracks = displayedTracks,
                    onOpenSettings = onOpenSettings,
                    contentBottomPadding = if (playerState.currentTrack != null) 90.dp else 16.dp
                )
            }
        }

        // MiniPlayerBar docked at bottom
        AnimatedVisibility(
            visible = playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
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
                    onOpenNowPlaying = onOpenNowPlaying,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// =========================================================================
// 3. EXPANDED VIEWPORT (>= 840dp): Canonical Google Supporting Pane Layout
// =========================================================================
@Composable
private fun ExpandedLayout(
    currentDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    activeSupportingPane: SupportingPaneType?,
    onToggleSupportingPane: (SupportingPaneType) -> Unit,
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    isShuffle: Boolean,
    onToggleShuffle: () -> Unit,
    isRepeat: Boolean,
    onToggleRepeat: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Main Three-Zone Body
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ----------------------------------------------------
            // ZONE 1: Left Persistent Navigation Drawer (~240dp)
            // ----------------------------------------------------
            PersistentNavigationDrawer(
                selectedDestination = currentDestination,
                onSelectDestination = onSelectDestination,
                serverConnected = serverConnected,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )

            // ----------------------------------------------------
            // ZONE 2: Primary Content Pane (Center, flex weight)
            // ----------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                DestinationContent(
                    destination = currentDestination,
                    coordinator = coordinator,
                    playerState = playerState,
                    serverConnected = serverConnected,
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedFilter = selectedFilter,
                    onSelectFilter = onSelectFilter,
                    displayedTracks = displayedTracks,
                    onOpenSettings = onOpenSettings,
                    contentBottomPadding = 16.dp
                )
            }

            // ----------------------------------------------------
            // ZONE 3: Right Contextual Supporting Pane (~340dp)
            // ----------------------------------------------------
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
                        coordinator = coordinator,
                        playerState = playerState,
                        onClose = { onToggleSupportingPane(paneType) },
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Persistent Full-Width Bottom Player Bar
        PersistentBottomPlayer(
            track = playerState.currentTrack,
            playbackInfo = playerState.playbackInfo,
            status = playerState.status,
            positionMs = playerState.positionMs,
            durationMs = playerState.durationMs,
            artworkUrl = playerState.currentTrack?.let { coordinator.apiClient.getArtworkUrl(it.id, 200) } ?: "",
            onTogglePlayPause = { coordinator.togglePlayPause() },
            onSeekTo = { pos -> coordinator.seekTo(pos) },
            onPlayNext = { coordinator.playNext() },
            onPlayPrevious = { coordinator.playPrevious() },
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

/**
 * Left Persistent Navigation Drawer for Expanded viewports.
 */
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
            // Top: Brand Header & Status Pill
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

                // Daemon status pill
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

                // Destination Items
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

            // Bottom Audio Engine Badge
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

/**
 * Right Contextual Supporting Pane container for Expanded viewports.
 * Displays Queue, Lyrics, or Poweramp Signal Path Inspector.
 */
@Composable
private fun SupportingPaneContainer(
    paneType: SupportingPaneType,
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Pane Header
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

            // Pane Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                when (paneType) {
                    SupportingPaneType.QUEUE -> {
                        QueuePaneContent(
                            coordinator = coordinator,
                            playerState = playerState
                        )
                    }

                    SupportingPaneType.LYRICS -> {
                        LyricsPaneContent(
                            currentTrack = playerState.currentTrack
                        )
                    }

                    SupportingPaneType.SIGNAL_PATH -> {
                        SignalPathPaneContent(
                            track = playerState.currentTrack,
                            playbackInfo = playerState.playbackInfo,
                            serverUrl = playerState.serverUrl,
                            isDevMode = playerState.isDevMode
                        )
                    }
                }
            }
        }
    }
}

/**
 * Supporting Pane: Playback Queue
 */
@Composable
private fun QueuePaneContent(
    coordinator: PlaybackCoordinator,
    playerState: PlayerState
) {
    val queue = playerState.queue
    if (queue.isEmpty()) {
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
            items(items = queue, key = { it.id }) { track ->
                val isPlaying = playerState.currentTrack?.id == track.id
                TrackRow(
                    track = track,
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track.id, 120),
                    isPlaying = isPlaying && playerState.status == PlaybackStatus.PLAYING,
                    onTrackClick = { coordinator.playTrack(it, queue) }
                )
            }
        }
    }
}

/**
 * Supporting Pane: Synced Lyrics Preview
 */
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

            // Sample expressive kinetic synced lyrics lines
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

/**
 * Supporting Pane: Poweramp Signal Path Inspector (ADR 0002 & CONTEXT.md)
 * Maps the bit-perfect lossless audio chain from storage origin to output hardware sink.
 */
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
            // Stage 1: Source
            SignalPathStageCard(
                stageNumber = "1",
                stageName = "SOURCE ORIGIN",
                primaryInfo = if (track.is_cached) "Telegram Dump Channel (Instant)" else "${track.provider.uppercase()} Mirror",
                secondaryInfo = "Endpoint: $serverUrl • ${if (isDevMode) "Dev Mode Direct" else "Signed Ticket"}",
                accentColor = SecondaryDark
            )

            // Stage 2: Track Format & Specs
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

            // Stage 3: Platform Decoder Engine
            SignalPathStageCard(
                stageNumber = "3",
                stageName = "PLATFORM DECODER",
                primaryInfo = "Native Multiplatform Audio Engine",
                secondaryInfo = "Bit-perfect PCM Uncompressed Buffer",
                accentColor = PrimaryDark
            )

            // Stage 4: Stream Pipe & Cache
            SignalPathStageCard(
                stageNumber = "4",
                stageName = "STREAM PIPE / CACHE",
                primaryInfo = "HTTP Chunk Buffer (Chunked Transfer)",
                secondaryInfo = "Latency: <120ms • Gapless Engine Active",
                accentColor = SecondaryDark
            )

            // Stage 5: Output & Hardware Sink
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

// =========================================================================
// DESTINATION CONTENT DISPATCHER
// =========================================================================
@Composable
private fun DestinationContent(
    destination: NavigationDestination,
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    onOpenSettings: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    when (destination) {
        NavigationDestination.HOME -> {
            HomeDestinationView(
                coordinator = coordinator,
                playerState = playerState,
                serverConnected = serverConnected,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                displayedTracks = displayedTracks,
                onOpenSettings = onOpenSettings,
                contentBottomPadding = contentBottomPadding
            )
        }

        NavigationDestination.SEARCH -> {
            SearchDestinationView(
                coordinator = coordinator,
                playerState = playerState,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                displayedTracks = displayedTracks,
                onOpenSettings = onOpenSettings,
                contentBottomPadding = contentBottomPadding
            )
        }

        NavigationDestination.LIBRARY -> {
            LibraryDestinationView(
                coordinator = coordinator,
                playerState = playerState,
                allTracks = displayedTracks,
                contentBottomPadding = contentBottomPadding
            )
        }

        NavigationDestination.SETTINGS -> {
            SettingsDestinationView(
                playerState = playerState,
                serverConnected = serverConnected,
                onOpenSettingsDialog = onOpenSettings,
                contentBottomPadding = contentBottomPadding
            )
        }
    }
}

@Composable
private fun HomeDestinationView(
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    serverConnected: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    onOpenSettings: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Expressive Search Bar
        ExpressiveSearchBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            onClearQuery = { onQueryChange("") },
            selectedFilter = selectedFilter,
            onFilterSelect = onSelectFilter,
            onOpenSettings = onOpenSettings,
            isDevMode = playerState.isDevMode,
            serverUrl = playerState.serverUrl
        )

        // Section Title
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
            items(displayedTracks, key = { it.id }) { track ->
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

@Composable
private fun SearchDestinationView(
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    onOpenSettings: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    HomeDestinationView(
        coordinator = coordinator,
        playerState = playerState,
        serverConnected = true,
        searchQuery = searchQuery,
        onQueryChange = onQueryChange,
        selectedFilter = selectedFilter,
        onSelectFilter = onSelectFilter,
        displayedTracks = displayedTracks,
        onOpenSettings = onOpenSettings,
        contentBottomPadding = contentBottomPadding
    )
}

@Composable
private fun LibraryDestinationView(
    coordinator: PlaybackCoordinator,
    playerState: PlayerState,
    allTracks: List<TrackSummaryDto>,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
    var selectedTab by remember { mutableStateOf("All Saved") }
    val tabs = listOf("All Saved", "Cached Downloads", "Playlists")

    val cachedOnly = remember(allTracks) { allTracks.filter { it.is_cached } }
    val tracksToShow = if (selectedTab == "Cached Downloads") cachedOnly else allTracks

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Library Sub-tabs
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
                val isPlaying = playerState.currentTrack?.id == track.id &&
                        playerState.status == PlaybackStatus.PLAYING

                TrackRow(
                    track = track,
                    artworkUrl = coordinator.apiClient.getArtworkUrl(track.id, 200),
                    isPlaying = isPlaying,
                    onTrackClick = { clicked ->
                        coordinator.playTrack(clicked, tracksToShow)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsDestinationView(
    playerState: PlayerState,
    serverConnected: Boolean,
    onOpenSettingsDialog: () -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp
) {
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

        // Server Connection Card
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
                    text = "Endpoint: ${playerState.serverUrl}",
                    style = ExpressiveTypography.bodyMedium,
                    color = OnSurfaceVariantDark
                )

                Text(
                    text = if (playerState.isDevMode) "Dev Mode Direct Stream Active (Bypasses Telegram OTP)" else "Production Ticket Auth Active",
                    style = ExpressiveTypography.bodySmall,
                    color = if (playerState.isDevMode) LosslessGold else PrimaryDark
                )
            }
        }

        // Audio Engine Card
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
