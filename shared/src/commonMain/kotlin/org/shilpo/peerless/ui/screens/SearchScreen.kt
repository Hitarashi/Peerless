package org.shilpo.peerless.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.CanonicalTrack
import org.shilpo.peerless.model.LastFmArtist
import org.shilpo.peerless.model.LastFmTag
import org.shilpo.peerless.model.LastFmTrackMatch
import org.shilpo.peerless.model.SearchFilter
import org.shilpo.peerless.model.TrackSearchResults
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.tasks.LocalRipCoordinator
import org.shilpo.peerless.ui.components.ExpressiveSearchBar
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.TrackRow

private data class TasteMixCardData(
    val id: String,
    val title: String,
    val subtitle: String,
    val queryKeyword: String
)

private val CuratedTasteMixes: List<TasteMixCardData> = listOf(
    TasteMixCardData(
        id = "audiophile_ref",
        title = "Audiophile Reference",
        subtitle = "Master tapes & wide dynamic range",
        queryKeyword = "Pink Floyd"
    ),
    TasteMixCardData(
        id = "synth_electro",
        title = "Synthwave & French Touch",
        subtitle = "Analog synthesizers & punchy transients",
        queryKeyword = "Daft Punk"
    ),
    TasteMixCardData(
        id = "ambient_focus",
        title = "Late Night Ambient",
        subtitle = "Expansive spatial soundscapes",
        queryKeyword = "Ambient"
    ),
    TasteMixCardData(
        id = "studio_classics",
        title = "Studio Master Classics",
        subtitle = "Legendary multi-track transfers",
        queryKeyword = "Radiohead"
    )
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    var lastFmMatches by remember { mutableStateOf<List<LastFmTrackMatch>>(emptyList()) }
    var artistSpotlight by remember { mutableStateOf<LastFmArtist?>(null) }
    var zeroStateTags by remember { mutableStateOf<List<LastFmTag>>(emptyList()) }
    var searchFailed by remember { mutableStateOf(false) }

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
            lastFmMatches = emptyList()
            artistSpotlight = null
            searchFailed = false
            return@LaunchedEffect
        }

        isSearching = true
        canonicalTracks = emptyList()
        lastFmMatches = emptyList()
        artistSpotlight = null
        searchFailed = false
        delay(450)

        val providerParam = selectedFilter.providerQuery

        coroutineScope {
            val backendSearch = async {
                apiClient.search(
                    query = trimmedQuery,
                    provider = providerParam
                )
            }
            val lastFmSearch = async {
                if (selectedFilter != SearchFilter.CACHED &&
                    selectedFilter != SearchFilter.ALBUMS &&
                    selectedFilter != SearchFilter.ARTISTS
                ) {
                    lastFmClient.searchTracks(trimmedQuery)
                } else {
                    Result.success(emptyList())
                }
            }
            val artistSearch = if (selectedFilter == SearchFilter.ARTISTS) {
                async { lastFmClient.getArtistInfo(trimmedQuery) }
            } else {
                null
            }

            val response = backendSearch.await().getOrElse {
                searchFailed = true
                lastFmSearch.cancel()
                artistSearch?.cancel()
                return@coroutineScope
            }
            canonicalTracks = TrackSearchResults.merge(
                response = response,
                query = trimmedQuery,
                filter = selectedFilter,
                baseUrl = apiClient.baseUrl,
                lastFmMatches = emptyList()
            )

            lastFmMatches = lastFmSearch.await().getOrDefault(emptyList())
            canonicalTracks = TrackSearchResults.merge(
                response = response,
                query = trimmedQuery,
                filter = selectedFilter,
                baseUrl = apiClient.baseUrl,
                lastFmMatches = lastFmMatches
            )

            artistSpotlight = artistSearch?.await()?.getOrNull()
        }

        isSearching = false
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth().fillMaxSize()
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
                    contentBottomPadding = contentBottomPadding
                )
            } else {
                SearchResultsContent(
                    query = searchQuery,
                    isSearching = isSearching,
                    searchFailed = searchFailed,
                    hasLastFmMatches = lastFmMatches.isNotEmpty(),
                    artistSpotlight = artistSpotlight,
                    canonicalTracks = canonicalTracks,
                    playerConnection = playerConnection,
                    currentTrackDto = currentTrackDto,
                    status = status,
                    onSelectTag = { tag -> searchQuery = tag },
                    onSelectArtist = { artist -> searchQuery = artist },
                    onOpenSettings = onOpenSettings,
                    onRipClick = onRipClick,
                    contentBottomPadding = contentBottomPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZeroStateDiscovery(
    tags: List<LastFmTag>,
    onSelectTag: (LastFmTag) -> Unit,
    onSelectTasteMix: (TasteMixCardData) -> Unit,
    contentBottomPadding: Dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 20.dp,
            bottom = contentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item(key = "discovery_hero") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MUSIC DISCOVERY",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                        Text(
                            text = "Find your next favorite.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "Search your catalog. Last.fm helps bring the closest track matches to the top.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                    }
                    Surface(
                        modifier = Modifier.size(104.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PeerlessIcon(
                                icon = PeerlessIcons.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }
        }

        if (tags.isNotEmpty()) {
            item(key = "popular_genres") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Popular genres",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            AssistChip(
                                onClick = { onSelectTag(tag) },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    PeerlessIcon(
                                        icon = PeerlessIcons.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "quick_searches_heading") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Start with a collection",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                CuratedTasteMixes.chunked(2).forEach { rowMixes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowMixes.forEach { mix ->
                            ElevatedCard(
                                onClick = { onSelectTasteMix(mix) },
                                modifier = Modifier.weight(1f).heightIn(min = 104.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = mix.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = mix.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    PeerlessIcon(
                                        icon = PeerlessIcons.Sparkle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (rowMixes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchResultsContent(
    query: String,
    isSearching: Boolean,
    searchFailed: Boolean,
    hasLastFmMatches: Boolean,
    artistSpotlight: LastFmArtist?,
    canonicalTracks: List<CanonicalTrack>,
    playerConnection: PlayerConnection,
    currentTrackDto: TrackSummaryDto?,
    status: PlaybackStatus,
    onSelectTag: (String) -> Unit,
    onSelectArtist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRipClick: ((TrackSummaryDto) -> Unit)?,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    val ripCoordinator = LocalRipCoordinator.current
    val coroutineScope = rememberCoroutineScope()
    val cachedQueue = remember(canonicalTracks) {
        canonicalTracks.filter { it.isCached }.map { it.toTrack() }
    }
    val isEmptyResult = !isSearching && canonicalTracks.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = contentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        if (artistSpotlight != null && !artistSpotlight.bioSummary.isNullOrBlank()) {
            item(key = "artist_spotlight") {
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    ArtistSpotlightCard(
                        artist = artistSpotlight,
                        onSelectTag = onSelectTag,
                        onSelectArtist = onSelectArtist
                    )
                }
            }
        }

        if (isSearching && canonicalTracks.isEmpty()) {
            item(key = "search_progress") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }

        if (canonicalTracks.isNotEmpty()) {
            item(key = "result_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${canonicalTracks.size} results",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasLastFmMatches) {
                        Text(
                            text = "Last.fm cross-check applied",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        itemsIndexed(
            items = canonicalTracks,
            key = { _, canonical -> "canonical_${canonical.id}" }
        ) { index, canonical ->
            val activeSource = canonical.immediatePlaySource() ?: canonical.bestSource
            val trackDto = canonical.toSummaryDto(activeSource)
            val isPlaying = currentTrackDto?.id == trackDto.id &&
                    status == PlaybackStatus.PLAYING

            TrackRow(
                canonicalTrack = canonical,
                artworkUrl = canonical.artworkUrl ?: apiClient.getArtworkUrl(trackDto, 200),
                isPlaying = isPlaying,
                index = index,
                count = canonicalTracks.size,
                onTrackClick = { clickedCanonical ->
                    val playSource =
                        clickedCanonical.immediatePlaySource() ?: clickedCanonical.bestSource
                    val playTrack = clickedCanonical.toTrack(playSource)
                    if (currentTrackDto?.id == playTrack.id) {
                        playerConnection.togglePlayPause()
                    } else {
                        playerConnection.play(
                            playTrack,
                            if (clickedCanonical.isCached) cachedQueue else emptyList()
                        )
                    }
                    val bgRip = clickedCanonical.backgroundRipSource()
                    if (bgRip != null) {
                        coroutineScope.launch {
                            apiClient.createRipTask(
                                provider = bgRip.provider.raw,
                                trackId = bgRip.providerTrackId,
                                codec = bgRip.codec.raw,
                                title = trackDto.title,
                                artist = trackDto.artist,
                                album = trackDto.album,
                                duration = trackDto.duration
                            ).onSuccess { ripCoordinator?.refreshServerTasks() }
                        }
                    }
                },
                onSelectSource = { source ->
                    playerConnection.play(canonical.toTrack(source), emptyList())
                },
                onRipClick = onRipClick,
                onPlayNext = if (trackDto.is_cached) {
                    { clicked -> playerConnection.playNextInQueue(clicked.toTrack()) }
                } else null,
                onAddToQueue = if (trackDto.is_cached) {
                    { clicked -> playerConnection.addToQueue(clicked.toTrack()) }
                } else null,
                onStartRadio = if (trackDto.is_cached) {
                    { clicked -> playerConnection.startRadio(clicked.toTrack()) }
                } else null
            )
        }

        if (isEmptyResult) {
            item(key = "empty_result") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when {
                                searchFailed -> "Catalog unavailable"
                                hasLastFmMatches -> "No catalog sources found"
                                else -> "No matches for “$query”"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when {
                                searchFailed -> "Check your server connection, then try the search again."
                                hasLastFmMatches -> "Last.fm found track matches, but the backend returned no matching track source."
                                else -> "Try a different title, artist, or provider filter."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchFailed) {
                            TextButton(onClick = onOpenSettings) {
                                Text("Server settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSpotlightCard(
    artist: LastFmArtist,
    onSelectTag: (String) -> Unit,
    onSelectArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Artist",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            artist.bioSummary?.let { bio ->
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (artist.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(artist.tags.take(6), key = { it.name }) { tag ->
                        AssistChip(
                            onClick = { onSelectTag(tag.name) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }
            if (artist.similarArtists.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(artist.similarArtists, key = { it }) { similarArtist ->
                        SuggestionChip(
                            onClick = { onSelectArtist(similarArtist) },
                            label = { Text(similarArtist) }
                        )
                    }
                }
            }
        }
    }
}
