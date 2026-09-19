package org.shilpo.peerless.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.*
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.SampleLosslessLibrary
import org.shilpo.peerless.ui.components.ExpressiveSearchBar
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.TrackRow

data class TasteMixCardData(
    val id: String,
    val title: String,
    val subtitle: String,
    val tagSpecs: String,
    val artistsSummary: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val queryKeyword: String
)

val CuratedTasteMixes: List<TasteMixCardData>
    @Composable get() = listOf(
        TasteMixCardData(
            id = "audiophile_ref",
            title = "Audiophile Reference",
            subtitle = "Master tapes & wide dynamic range",
            tagSpecs = "24-BIT / 192KHZ • FLAC",
            artistsSummary = "Pink Floyd, Miles Davis, Eagles",
            primaryColor = MaterialTheme.colorScheme.tertiary,
            secondaryColor = Color(0xFF5322C7),
            queryKeyword = "Pink Floyd"
        ),
        TasteMixCardData(
            id = "synth_electro",
            title = "Synthwave & French Touch",
            subtitle = "Analog synthesizers & punchy transients",
            tagSpecs = "24-BIT / 96KHZ • ALAC",
            artistsSummary = "Daft Punk, M83, Kavinsky",
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            queryKeyword = "Daft Punk"
        ),
        TasteMixCardData(
            id = "ambient_focus",
            title = "Late Night Ambient",
            subtitle = "Expansive spatial soundscapes",
            tagSpecs = "24-BIT / 88.2KHZ • FLAC",
            artistsSummary = "Ambient, Brian Eno, Tycho",
            primaryColor = MaterialTheme.colorScheme.secondary,
            secondaryColor = Color(0xFF1E3C72),
            queryKeyword = "Ambient"
        ),
        TasteMixCardData(
            id = "studio_classics",
            title = "Studio Master Classics",
            subtitle = "Legendary multi-track transfers",
            tagSpecs = "24-BIT / 96KHZ • ALAC",
            artistsSummary = "The Beatles, Fleetwood Mac, Radiohead",
            primaryColor = LosslessPurple,
            secondaryColor = Color(0xFF16222F),
            queryKeyword = "Radiohead"
        )
    )

@Composable
fun SearchScreen(
    playerConnection: PlayerConnection,
    onOpenSettings: () -> Unit = {},
    contentBottomPadding: Dp = 0.dp,
    onRipClick: ((TrackSummaryDto) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    initialFilter: SearchFilter = SearchFilter.ALL
) {
    val apiClient = LocalPeerlessApiClient.current
    val currentTrack by playerConnection.currentTrack.collectAsState()
    val status by playerConnection.status.collectAsState()
    val currentTrackDto = currentTrack?.toSummaryDto()

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var isSearching by remember { mutableStateOf(false) }

    var canonicalTracks by remember { mutableStateOf<List<CanonicalTrack>>(emptyList()) }
    var artistSpotlight by remember { mutableStateOf<LastFmArtist?>(null) }
    var zeroStateTags by remember { mutableStateOf<List<LastFmTag>>(emptyList()) }

    val lastFmClient = remember { LastFmClient() }

    LaunchedEffect(Unit) {
        val tagsRes = lastFmClient.getTopTags()
        zeroStateTags = tagsRes.getOrElse { LastFmClient.fallbackTopTags() }
    }

    LaunchedEffect(searchQuery, selectedFilter, apiClient.baseUrl) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isBlank()) {
            isSearching = false
            canonicalTracks = emptyList()
            artistSpotlight = null
            return@LaunchedEffect
        }

        isSearching = true
        delay(300)

        val providerParam = selectedFilter.providerQuery

        coroutineScope {
            launch {
                val searchRes = apiClient.search(
                    query = trimmedQuery,
                    provider = providerParam
                )

                searchRes.onSuccess { response ->
                    val filteredCached = when (selectedFilter) {
                        SearchFilter.CACHED -> response.cached
                        SearchFilter.APPLE_MUSIC -> response.cached.filter {
                            it.provider.contains(
                                "apple",
                                ignoreCase = true
                            )
                        }

                        SearchFilter.QOBUZ -> response.cached.filter {
                            it.provider.contains(
                                "qobuz",
                                ignoreCase = true
                            )
                        }

                        SearchFilter.TRACKS -> response.cached.filter {
                            it.title.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        SearchFilter.ALBUMS -> response.cached.filter {
                            it.album.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        SearchFilter.ARTISTS -> response.cached.filter {
                            it.artist.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        else -> response.cached
                    }

                    val rawLive = when (selectedFilter) {
                        SearchFilter.CACHED -> emptyList()
                        SearchFilter.APPLE_MUSIC -> response.live.filter {
                            it.provider.contains(
                                "apple",
                                ignoreCase = true
                            )
                        }

                        SearchFilter.QOBUZ -> response.live.filter { it.provider.contains("qobuz", ignoreCase = true) }
                        SearchFilter.TRACKS -> response.live.filter {
                            it.title.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        SearchFilter.ALBUMS -> response.live.filter {
                            it.album.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        SearchFilter.ARTISTS -> response.live.filter {
                            it.artist.contains(
                                trimmedQuery,
                                ignoreCase = true
                            )
                        }

                        else -> response.live
                    }

                    val deduplicated = CanonicalDeduplicator.deduplicate(
                        cachedTracks = filteredCached,
                        liveTracks = rawLive,
                        baseUrl = apiClient.baseUrl
                    )
                    canonicalTracks = deduplicated
                }.onFailure {
                    val localMatches = SampleLosslessLibrary.filter {
                        val matchesQuery = it.title.contains(trimmedQuery, ignoreCase = true) ||
                                it.artist.contains(trimmedQuery, ignoreCase = true) ||
                                it.album.contains(trimmedQuery, ignoreCase = true)
                        val matchesFilter = when (selectedFilter) {
                            SearchFilter.CACHED -> it.is_cached
                            SearchFilter.APPLE_MUSIC -> it.provider.contains("apple", ignoreCase = true)
                            SearchFilter.QOBUZ -> it.provider.contains("qobuz", ignoreCase = true)
                            SearchFilter.TRACKS -> it.title.contains(trimmedQuery, ignoreCase = true)
                            SearchFilter.ALBUMS -> it.album.contains(trimmedQuery, ignoreCase = true)
                            SearchFilter.ARTISTS -> it.artist.contains(trimmedQuery, ignoreCase = true)
                            else -> true
                        }
                        matchesQuery && matchesFilter
                    }

                    val cachedLocal = localMatches.filter { it.is_cached }
                    val liveLocal = if (selectedFilter == SearchFilter.CACHED) {
                        emptyList()
                    } else {
                        localMatches
                            .filter { !it.is_cached }
                            .map {
                                UncachedTrackDto(
                                    provider = it.provider,
                                    item_id = it.track_id,
                                    track_id = it.track_id,
                                    title = it.title,
                                    artist = it.artist,
                                    album = it.album,
                                    duration = it.duration,
                                    is_cached = false
                                )
                            }
                    }
                    canonicalTracks = CanonicalDeduplicator.deduplicate(
                        cachedTracks = cachedLocal,
                        liveTracks = liveLocal,
                        baseUrl = apiClient.baseUrl
                    )
                }
            }

            launch {
                if (selectedFilter != SearchFilter.TRACKS && selectedFilter != SearchFilter.ALBUMS) {
                    val artistRes = lastFmClient.getArtistInfo(trimmedQuery)
                    artistSpotlight = artistRes.getOrNull()
                } else {
                    artistSpotlight = null
                }
            }
        }

        isSearching = false
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ExpressiveSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearQuery = { searchQuery = "" },
                selectedFilter = selectedFilter,
                onFilterSelect = { selectedFilter = it },
                onOpenSettings = onOpenSettings,
                isSearching = isSearching,
                serverUrl = apiClient.baseUrl
            )

            if (searchQuery.isBlank()) {
                ZeroStateDiscovery(
                    tags = zeroStateTags,
                    onSelectTag = { tag -> searchQuery = tag.name },
                    onSelectTasteMix = { mix -> searchQuery = mix.queryKeyword },
                    cachedSampleTracks = SampleLosslessLibrary.filter { it.is_cached },
                    playerConnection = playerConnection,
                    currentTrackDto = currentTrackDto,
                    status = status,
                    contentBottomPadding = contentBottomPadding
                )
            } else {
                SearchResultsContent(
                    query = searchQuery,
                    isSearching = isSearching,
                    artistSpotlight = artistSpotlight,
                    canonicalTracks = canonicalTracks,
                    playerConnection = playerConnection,
                    currentTrackDto = currentTrackDto,
                    status = status,
                    onSelectTag = { tag -> searchQuery = tag },
                    onSelectArtist = { artist -> searchQuery = artist },
                    onRipClick = onRipClick,
                    contentBottomPadding = contentBottomPadding
                )
            }
        }
    }
}

@Composable
private fun ZeroStateDiscovery(
    tags: List<LastFmTag>,
    onSelectTag: (LastFmTag) -> Unit,
    onSelectTasteMix: (TasteMixCardData) -> Unit,
    cachedSampleTracks: List<TrackSummaryDto>,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = contentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.LosslessWave,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "EXPLORE SOUNDSCAPES",
                        style = SpecBadgeTypography.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "• LAST.FM INTELLIGENCE",
                        style = SpecBadgeTypography.copy(
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tags, key = { it.name }) { tag ->
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
                                .clickable { onSelectTag(tag) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = tag.name,
                                style = ExpressiveTypography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FEATURED TASTE MIXES",
                        style = SpecBadgeTypography.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "• BIT-PERFECT STARTERS",
                        style = SpecBadgeTypography.copy(
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CuratedTasteMixes.chunked(2).forEach { rowMixes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowMixes.forEach { mix ->
                                Box(modifier = Modifier.weight(1f)) {
                                    TasteMixCard(
                                        mix = mix,
                                        onClick = { onSelectTasteMix(mix) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "INSTANT PLAYBACK CACHE",
                            style = SpecBadgeTypography.copy(
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "<200ms LATENCY",
                        style = SpecBadgeTypography.copy(
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Text(
                    text = "Pre-ripped bit-perfect FLAC/ALAC masters verified in the Telegram dump channel.",
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        items(cachedSampleTracks.take(4), key = { it.id }) { track ->
            val isPlaying = currentTrackDto?.id == track.id &&
                    status == PlaybackStatus.PLAYING

            TrackRow(
                track = track,
                artworkUrl = apiClient.getArtworkUrl(track, 200),
                isPlaying = isPlaying,
                onTrackClick = {
                    if (currentTrackDto?.id == it.id) {
                        playerConnection.togglePlayPause()
                    } else {
                        playerConnection.play(it.toTrack(), cachedSampleTracks.map { t -> t.toTrack() })
                    }
                }
            )
        }
    }
}

@Composable
private fun TasteMixCard(
    mix: TasteMixCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        shape = SquircleShapeMedium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        borderBrush = Brush.linearGradient(
            listOf(
                mix.primaryColor.copy(alpha = 0.45f),
                mix.secondaryColor.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.outlineVariant
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(mix.primaryColor.copy(alpha = 0.16f))
                    .border(1.dp, mix.primaryColor.copy(alpha = 0.4f), PillShape)
                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mix.tagSpecs,
                    style = SpecBadgeTypography.copy(
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = mix.primaryColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = mix.title,
                    style = ExpressiveTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mix.subtitle,
                    style = ExpressiveTypography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = mix.artistsSummary,
                style = SpecBadgeTypography.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchResultsContent(
    query: String,
    isSearching: Boolean,
    artistSpotlight: LastFmArtist?,
    canonicalTracks: List<CanonicalTrack>,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    onSelectTag: (String) -> Unit,
    onSelectArtist: (String) -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)?,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    val coroutineScope = rememberCoroutineScope()
    val cachedTracks = remember(canonicalTracks) { canonicalTracks.filter { it.isCached } }
    val liveTracks = remember(canonicalTracks) { canonicalTracks.filter { !it.isCached } }
    val isEmptyResult = !isSearching && canonicalTracks.isEmpty() && artistSpotlight == null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 4.dp,
            bottom = contentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (artistSpotlight != null && !artistSpotlight.bioSummary.isNullOrBlank()) {
            item(key = "artist_spotlight") {
                ArtistSpotlightCard(
                    artist = artistSpotlight,
                    onSelectTag = onSelectTag,
                    onSelectArtist = onSelectArtist
                )
            }
        }

        if (cachedTracks.isNotEmpty()) {
            item(key = "header_cached") {
                SectionHeader(
                    title = "Playable Now (<200ms)",
                    subtitle = "TELEGRAM DUMP CHANNEL",
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    badgeIcon = PeerlessIcons.CloudDone,
                    countText = "${cachedTracks.size} tracks"
                )
            }

            items(cachedTracks, key = { "canonical_cached_${it.id}" }) { canonical ->
                val activeSource = canonical.immediatePlaySource() ?: canonical.bestSource
                val trackDto = canonical.toSummaryDto(activeSource)
                val isPlaying = currentTrackDto?.id == trackDto.id &&
                        status == PlaybackStatus.PLAYING

                TrackRow(
                    canonicalTrack = canonical,
                    artworkUrl = canonical.artworkUrl ?: apiClient.getArtworkUrl(trackDto, 200),
                    isPlaying = isPlaying,
                    onTrackClick = { clickedCanonical ->
                        val playSource = clickedCanonical.immediatePlaySource() ?: clickedCanonical.bestSource
                        val playTrack = clickedCanonical.toTrack(playSource)
                        if (currentTrackDto?.id == playTrack.id) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection.play(playTrack, cachedTracks.map { it.toTrack() })
                        }
                        val bgRip = clickedCanonical.backgroundRipSource()
                        if (bgRip != null) {
                            coroutineScope.launch {
                                apiClient.createRipTask(
                                    provider = bgRip.provider.raw,
                                    trackId = bgRip.providerTrackId,
                                    codec = bgRip.codec.raw
                                )
                            }
                        }
                    },
                    onSelectSource = { source ->
                        val playTrack = canonical.toTrack(source)
                        playerConnection.play(playTrack, cachedTracks.map { it.toTrack() })
                    },
                    onRipClick = onRipClick
                )
            }
        }

        if (liveTracks.isNotEmpty()) {
            item(key = "header_live") {
                SectionHeader(
                    title = "Live Catalog (On-Demand Rip)",
                    subtitle = "APPLE MUSIC & QOBUZ",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    badgeIcon = PeerlessIcons.LosslessWave,
                    countText = "${liveTracks.size} available"
                )
            }

            items(liveTracks, key = { "canonical_live_${it.id}" }) { canonical ->
                val activeSource = canonical.immediatePlaySource() ?: canonical.bestSource
                val trackDto = canonical.toSummaryDto(activeSource)
                val isPlaying = currentTrackDto?.id == trackDto.id &&
                        status == PlaybackStatus.PLAYING

                TrackRow(
                    canonicalTrack = canonical,
                    artworkUrl = canonical.artworkUrl ?: apiClient.getArtworkUrl(trackDto, 200),
                    isPlaying = isPlaying,
                    onTrackClick = { clickedCanonical ->
                        val playSource = clickedCanonical.immediatePlaySource() ?: clickedCanonical.bestSource
                        val playTrack = clickedCanonical.toTrack(playSource)
                        if (currentTrackDto?.id == playTrack.id) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection.play(playTrack, emptyList())
                        }
                        val bgRip = clickedCanonical.backgroundRipSource()
                        if (bgRip != null) {
                            coroutineScope.launch {
                                apiClient.createRipTask(
                                    provider = bgRip.provider.raw,
                                    trackId = bgRip.providerTrackId,
                                    codec = bgRip.codec.raw
                                )
                            }
                        }
                    },
                    onSelectSource = { source ->
                        val playTrack = canonical.toTrack(source)
                        playerConnection.play(playTrack, emptyList())
                    },
                    onRipClick = onRipClick
                )
            }
        }

        if (isEmptyResult) {
            item(key = "empty_result") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassSurface(
                        shape = SquircleShapeLarge,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = PeerlessIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No lossless matches for “$query”",
                                style = ExpressiveTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try searching by exact artist, track title, or choose another provider filter chip above.",
                                style = ExpressiveTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistSpotlightCard(
    artist: LastFmArtist,
    onSelectTag: (String) -> Unit,
    onSelectArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        shape = SquircleShapeMedium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        borderBrush = Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.outlineVariant
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "ARTIST SPOTLIGHT",
                        style = SpecBadgeTypography.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "LAST.FM TASTE ENGINE",
                    style = SpecBadgeTypography.copy(
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }

            Text(
                text = artist.name,
                style = ExpressiveTypography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            artist.bioSummary?.let { bio ->
                Text(
                    text = bio,
                    style = ExpressiveTypography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (artist.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    artist.tags.take(6).forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), PillShape)
                                .clickable { onSelectTag(tag.name) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = SpecBadgeTypography.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (artist.similarArtists.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIMILAR:",
                        style = SpecBadgeTypography.copy(
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    artist.similarArtists.take(5).forEach { simArtist ->
                        Text(
                            text = simArtist,
                            style = SpecBadgeTypography.copy(
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f), PillShape)
                                .clickable { onSelectArtist(simArtist) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    badgeColor: Color,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    countText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = ExpressiveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(1.dp, badgeColor.copy(alpha = 0.40f), PillShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = subtitle,
                    style = SpecBadgeTypography.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = badgeColor
                )
            }
        }

        Text(
            text = countText,
            style = SpecBadgeTypography.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
