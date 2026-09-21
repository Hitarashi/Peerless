package org.shilpo.peerless.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.SessionState
import org.shilpo.peerless.home.HomeFeedEmptyReason
import org.shilpo.peerless.home.HomeFeedItem
import org.shilpo.peerless.home.HomeFeedState
import org.shilpo.peerless.home.LocalHomeFeedRepository
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.LocalWindowWidthSizeClass
import org.shilpo.peerless.theme.SquircleShapeSmall
import org.shilpo.peerless.theme.WindowWidthSizeClass
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.TrackRow

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

@Composable
fun HomeTopHeader(
    userDisplayName: String,
    avatarBytes: ByteArray?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showDisplayName = LocalWindowWidthSizeClass.current != WindowWidthSizeClass.COMPACT
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.MusicNote,
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Open settings for $userDisplayName"
                }
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                if (avatarBytes != null) {
                    AsyncImage(
                        model = avatarBytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = avatarInitials(userDisplayName),
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (showDisplayName) {
                Text(
                    text = userDisplayName,
                    style = ExpressiveTypography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 168.dp)
                )
            }
        }
    }
}

internal fun avatarInitials(displayName: String): String {
    val normalizedName = displayName.trim().removePrefix("@")
    if (normalizedName.startsWith("User #", ignoreCase = true)) return "U"

    return normalizedName
        .split(' ')
        .mapNotNull { word -> word.firstOrNull { it.isLetter() }?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "U" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTrackCarousel(
    title: String,
    subtitle: String? = null,
    picks: List<HomeFeedItem>,
    currentTrackId: Int?,
    isPlaying: Boolean,
    onTrackClick: (TrackSummaryDto) -> Unit,
    onPlayNext: ((TrackSummaryDto) -> Unit)? = null,
    onAddToQueue: ((TrackSummaryDto) -> Unit)? = null,
    onStartRadio: ((TrackSummaryDto) -> Unit)? = null,
    onRipClick: ((TrackSummaryDto) -> Unit)?,
    getArtworkUrl: (TrackSummaryDto) -> String,
    modifier: Modifier = Modifier
) {
    if (picks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val visibleCardCapacity = when {
                maxWidth >= 1_100.dp -> 3
                maxWidth >= 640.dp -> 2
                else -> 1
            }
            val fullCardsVisible = minOf(picks.size, visibleCardCapacity)
            val hasPeekItem = picks.size > fullCardsVisible
            val itemSpacing = 10.dp
            val horizontalInset = 16.dp
            val peekWidth = if (hasPeekItem) 56.dp else 0.dp
            val gapCount =
                if (hasPeekItem) fullCardsVisible else (fullCardsVisible - 1).coerceAtLeast(0)
            val availableHeroWidth =
                maxWidth - horizontalInset * 2 - itemSpacing * gapCount - peekWidth
            val heroMaxWidth = (availableHeroWidth.value / fullCardsVisible).dp
                .coerceAtLeast(232.dp)
                .coerceAtMost(440.dp)
            val heroHeight = (heroMaxWidth * 0.9f).coerceIn(220.dp, 380.dp)

            val carouselState = rememberCarouselState { picks.size }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = ExpressiveTypography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = ExpressiveTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = heroMaxWidth,
                    itemSpacing = itemSpacing,
                    contentPadding = PaddingValues(horizontal = horizontalInset),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                ) { index ->
                    val pick = picks[index]
                    val track = pick.track
                    val isCurrentPlaying = currentTrackId == track.id && isPlaying

                    QuickPickCard(
                        track = track,
                        artworkUrl = getArtworkUrl(track),
                        isPlaying = isCurrentPlaying,
                        onClick = {
                            if (track.is_cached) onTrackClick(track) else onRipClick?.invoke(track)
                        },
                        onPlayNext = onPlayNext?.takeIf { track.is_cached }?.let { action ->
                            { action(track) }
                        },
                        onAddToQueue = onAddToQueue?.takeIf { track.is_cached }?.let { action ->
                            { action(track) }
                        },
                        onStartRadio = onStartRadio?.takeIf { track.is_cached }?.let { action ->
                            { action(track) }
                        },
                        canActivate = track.is_cached || onRipClick != null,
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                    )
                }
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
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    canActivate: Boolean,
    modifier: Modifier = Modifier
) {
    var showTrackMenu by remember(track.id) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                )
            )
            .border(
                1.dp,
                if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.5f
                ),
                MaterialTheme.shapes.extraLarge
            )
            .clickable(enabled = canActivate, onClick = onClick)
    ) {
        PeerlessIcon(
            icon = PeerlessIcons.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
        )

        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                        1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.88f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, top = 12.dp, end = 64.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = track.title,
                style = ExpressiveTypography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = track.artist,
                style = ExpressiveTypography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }

        if (onPlayNext != null || onAddToQueue != null || onStartRadio != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = { showTrackMenu = true }) {
                    PeerlessIcon(
                        icon = PeerlessIcons.MoreVert,
                        contentDescription = "${track.title} options",
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
                DropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { showTrackMenu = false }
                ) {
                    onPlayNext?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Play next") },
                            onClick = { showTrackMenu = false; action() }
                        )
                    }
                    onAddToQueue?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Add to queue") },
                            onClick = { showTrackMenu = false; action() }
                        )
                    }
                    onStartRadio?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Start radio") },
                            onClick = { showTrackMenu = false; action() }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(enabled = canActivate, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = when {
                        isPlaying -> PeerlessIcons.LosslessWave
                        track.is_cached -> PeerlessIcons.Play
                        else -> PeerlessIcons.Download
                    },
                    contentDescription = when {
                        isPlaying -> "Now playing ${track.title}"
                        track.is_cached -> "Play ${track.title}"
                        else -> "Rip ${track.title}"
                    },
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchResultsSectionHeader(
    resultCount: Int,
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
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Search results",
                style = ExpressiveTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$resultCount tracks",
                style = ExpressiveTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.Play,
                        contentDescription = "Play all",
                        tint = MaterialTheme.colorScheme.primary,
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun HomeFeedSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp)
            .semantics { heading() },
        style = ExpressiveTypography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeFeedLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun HomeFeedMessage(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeerlessIcon(
                icon = PeerlessIcons.Sparkle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    playerConnection: PlayerConnection,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    displayedTracks: List<TrackSummaryDto>,
    allTracks: List<TrackSummaryDto>,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    contentBottomPadding: Dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentTrack by playerConnection.currentTrack.collectAsState()
    val currentTrackDto = currentTrack?.toSummaryDto()
    val status by playerConnection.status.collectAsState()
    val apiClient = LocalPeerlessApiClient.current
    val sessionManager = LocalSessionManager.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val currentUser = (sessionState as? SessionState.Authenticated)?.user
    val userDisplayName = currentUser?.displayName ?: "Account"
    var avatarBytes by remember(currentUser?.telegram_id, apiClient.baseUrl) {
        mutableStateOf<ByteArray?>(null)
    }
    val lastFmUsername by sessionManager.lastFmUsername.collectAsState()
    val homeFeedRepository = LocalHomeFeedRepository.current
    val libraryCatalogRevision = remember(allTracks) {
        allTracks.map {
            "${it.id}|${it.provider}|${it.track_id}|${it.is_cached}|${it.isrc.orEmpty()}"
        }.sorted()
    }
    val latestLibraryTracks by rememberUpdatedState(allTracks)
    var homeFeed by remember(lastFmUsername) {
        mutableStateOf(HomeFeedState(isLoading = !lastFmUsername.isNullOrBlank()))
    }
    var homeFeedRefreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser?.telegram_id, apiClient.baseUrl) {
        avatarBytes = apiClient.getUserAvatar().getOrNull()
    }

    LaunchedEffect(
        homeFeedRepository,
        lastFmUsername,
        apiClient.baseUrl,
        searchQuery.isBlank(),
        homeFeedRefreshKey,
        libraryCatalogRevision
    ) {
        if (searchQuery.isNotBlank()) return@LaunchedEffect
        val username = lastFmUsername
        if (username.isNullOrBlank()) {
            homeFeed = HomeFeedState()
            return@LaunchedEffect
        }

        homeFeed = homeFeed.copy(isLoading = true, hasError = false)
        homeFeed = homeFeedRepository.load(username, latestLibraryTracks)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "top_header") {
            HomeTopHeader(
                userDisplayName = userDisplayName,
                avatarBytes = avatarBytes,
                onOpenSettings = onNavigateToSettings
            )
        }

        if (searchQuery.isBlank()) {
            if (homeFeed.isLoading && homeFeed.yourRotation.isEmpty() &&
                homeFeed.recentTracks.isEmpty() && homeFeed.similarToTaste.isEmpty()
            ) {
                item(key = "home_feed_loading") { HomeFeedLoading() }
            } else if (homeFeed.hasError) {
                item(key = "home_feed_error") {
                    HomeFeedMessage(
                        message = "Your Last.fm listening feed could not be loaded.",
                        actionLabel = "Retry",
                        onAction = { homeFeedRefreshKey += 1 }
                    )
                }
            } else when (homeFeed.emptyReason) {
                HomeFeedEmptyReason.NO_LISTENING_HISTORY -> item(key = "home_feed_no_history") {
                    HomeFeedMessage(
                        message = "Last.fm doesn't show any listening history yet. Start scrobbling on Last.fm and your Home feed will fill in."
                    )
                }

                HomeFeedEmptyReason.NO_PLAYABLE_MATCHES -> item(key = "home_feed_no_matches") {
                    HomeFeedMessage(
                        message = "Your Last.fm history is available, but we couldn't match those tracks in the Peerless catalog.",
                        actionLabel = "Search",
                        onAction = onNavigateToSearch
                    )
                }

                null -> Unit
            }

            if (homeFeed.yourRotation.isNotEmpty()) {
                item(key = "your_rotation") {
                    HomeTrackCarousel(
                        title = "Your rotation",
                        subtitle = "From your Last.fm listening",
                        picks = homeFeed.yourRotation,
                        currentTrackId = currentTrackDto?.id,
                        isPlaying = status == PlaybackStatus.PLAYING,
                        onTrackClick = { clicked ->
                            playerConnection.playFromContext(
                                clicked.toTrack(),
                                homeFeed.yourRotation.map { it.track.toTrack() }
                            )
                        },
                        onPlayNext = { clicked -> playerConnection.playNextInQueue(clicked.toTrack()) },
                        onAddToQueue = { clicked -> playerConnection.addToQueue(clicked.toTrack()) },
                        onStartRadio = { clicked -> playerConnection.startRadio(clicked.toTrack()) },
                        onRipClick = onRipClick,
                        getArtworkUrl = { track ->
                            apiClient.getArtworkUrl(track, 300)
                        }
                    )
                }
            }

            if (homeFeed.similarToTaste.isNotEmpty()) {
                item(key = "similar_to_your_taste") {
                    HomeTrackCarousel(
                        title = "Similar to your taste",
                        picks = homeFeed.similarToTaste,
                        currentTrackId = currentTrackDto?.id,
                        isPlaying = status == PlaybackStatus.PLAYING,
                        onTrackClick = { clicked ->
                            playerConnection.playFromContext(
                                clicked.toTrack(),
                                homeFeed.similarToTaste.map { it.track.toTrack() }
                            )
                        },
                        onPlayNext = { clicked -> playerConnection.playNextInQueue(clicked.toTrack()) },
                        onAddToQueue = { clicked -> playerConnection.addToQueue(clicked.toTrack()) },
                        onStartRadio = { clicked -> playerConnection.startRadio(clicked.toTrack()) },
                        onRipClick = onRipClick,
                        getArtworkUrl = { track -> apiClient.getArtworkUrl(track, 300) }
                    )
                }
            }

            if (homeFeed.recentTracks.isNotEmpty()) {
                item(key = "listen_again_header") {
                    HomeFeedSectionHeader(title = "Listen again")
                }
                items(
                    items = homeFeed.recentTracks,
                    key = { "recent_${it.track.provider}_${it.track.track_id}" }
                ) { item ->
                    val track = item.track
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        TrackRow(
                            track = track,
                            artworkUrl = apiClient.getArtworkUrl(track, 200),
                            isPlaying = currentTrackDto?.id == track.id && status == PlaybackStatus.PLAYING,
                            canonicalTrack = item.canonicalTrack,
                            onTrackClick = { clicked ->
                                playerConnection.playFromContext(
                                    clicked.toTrack(),
                                    homeFeed.recentTracks.map { it.track.toTrack() }
                                )
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
                            onRipClick = onRipClick
                        )
                    }
                }
            }

        }

        if (searchQuery.isNotBlank()) {
            item(key = "search_results_header") {
                SearchResultsSectionHeader(
                    resultCount = displayedTracks.size,
                    onShuffle = {
                        if (displayedTracks.isNotEmpty()) {
                            val shuffled = displayedTracks.shuffled()
                            playerConnection.play(
                                shuffled.first().toTrack(),
                                shuffled.map { it.toTrack() })
                        }
                    },
                    onPlayAll = {
                        if (displayedTracks.isNotEmpty()) {
                            playerConnection.play(
                                displayedTracks.first().toTrack(),
                                displayedTracks.map { it.toTrack() })
                        }
                    }
                )
            }

            if (displayedTracks.isEmpty()) {
                item(key = "empty_search_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching tracks found",
                            style = ExpressiveTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                if (currentTrackDto?.id == clicked.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.play(
                                        clicked.toTrack(),
                                        displayedTracks.map { it.toTrack() })
                                }
                            },
                            onRipClick = onRipClick
                        )
                    }
                }
            }
        }
    }
}
