package org.shilpo.peerless.ui.screens


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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeSmall
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.TrackRow
import org.shilpo.peerless.ui.components.formatProviderLabel

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
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onToggleStats),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.Compass,
                    contentDescription = "Explore & Stats",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onNavigateToSearch),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurface,
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                1.dp,
                if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.5f
                ),
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
                color = MaterialTheme.colorScheme.onSurface,
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val hasApple = track.provider.contains("apple", ignoreCase = true)
                val hasQobuz = track.provider.contains("qobuz", ignoreCase = true)
                val hasDolby = track.codec.contains("ec-3", ignoreCase = true) ||
                        track.codec.contains("ec3", ignoreCase = true) ||
                        track.codec.contains("atmos", ignoreCase = true)
                val hasHiRes = (track.bit_depth ?: 16) >= 24 || (track.sample_rate ?: 44100) >= 88200

                if (hasApple) {
                    PeerlessIcon(
                        icon = PeerlessIcons.AppleLogo,
                        contentDescription = "Apple Music",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                }
                if (hasQobuz) {
                    PeerlessIcon(
                        icon = PeerlessIcons.QobuzLogo,
                        contentDescription = "Qobuz",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.height(10.dp).width(25.dp)
                    )
                }
                if (!hasApple && !hasQobuz && track.provider.isNotBlank()) {
                    Text(
                        text = formatProviderLabel(track.provider),
                        style = SpecBadgeTypography.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasDolby) {
                    PeerlessIcon(
                        icon = PeerlessIcons.DolbyAtmos,
                        contentDescription = "Dolby Atmos",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.height(9.dp).width(14.dp)
                    )
                }
                if (hasHiRes) {
                    PeerlessIcon(
                        icon = PeerlessIcons.HiRes,
                        contentDescription = "Hi-Res Audio",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
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
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$trackCount tracks",
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
fun HomeScreenContent(
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
    val apiClient = LocalPeerlessApiClient.current

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
                            if (currentTrackDto?.id == clicked.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.play(clicked.toTrack(), allTracks.map { it.toTrack() })
                            }
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
                        PeerlessIcon(
                            icon = PeerlessIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No matching lossless tracks found",
                            style = ExpressiveTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            if (currentTrackDto?.id == clicked.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.play(clicked.toTrack(), displayedTracks.map { it.toTrack() })
                            }
                        },
                        onRipClick = onRipClick
                    )
                }
            }
        }
    }
}
