package org.shilpo.peerless.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.shilpo.peerless.lastfm.LocalLastFmClient
import org.shilpo.peerless.model.LastFmTrackInfo
import org.shilpo.peerless.model.TrackDetailDto
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.theme.ArtworkShape
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SquircleShapeMedium
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import org.shilpo.peerless.ui.components.formatDuration
import org.shilpo.peerless.ui.components.formatProviderLabel

internal data class TrackContextFact(val label: String, val value: String)

internal fun lastFmExternalUrl(rawUrl: String?): String? {
    val url = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val allowedHost = Regex("^https?://(?:www\\.)?last\\.fm/", RegexOption.IGNORE_CASE)
    if (!allowedHost.containsMatchIn(url)) return null
    return if (url.startsWith("http://", ignoreCase = true)) {
        "https://${url.substringAfter("://")}"
    } else {
        url
    }
}

internal fun trackContextFacts(
    details: TrackDetailDto?,
    lastFmInfo: LastFmTrackInfo? = null
): List<TrackContextFact> {
    return buildList {
        if (details != null) {
            details.genre?.trim()?.takeIf(String::isNotEmpty)?.let {
                add(TrackContextFact("Genre", it))
            }
            details.release_date?.trim()?.takeIf(String::isNotEmpty)?.let { releaseDate ->
                val year = releaseDate.take(4).takeIf { value ->
                    value.length == 4 && value.all(Char::isDigit)
                } ?: releaseDate
                add(TrackContextFact("Track released", year))
            }
            details.composer?.trim()?.takeIf(String::isNotEmpty)?.let {
                add(TrackContextFact("Composer", it))
            }

            val trackNumber = details.track_number?.takeIf { it > 0 }
            val trackCount = details.track_count?.takeIf { it > 0 }
            val trackPosition = when {
                trackNumber != null && trackCount != null -> "Track $trackNumber of $trackCount"
                trackNumber != null -> "Track $trackNumber"
                else -> null
            }
            val discNumber = details.disc_number?.takeIf { it > 0 }?.let { "Disc $it" }
            val albumPosition = listOfNotNull(trackPosition, discNumber).joinToString(" · ")
            if (albumPosition.isNotEmpty()) {
                add(TrackContextFact("Album position", albumPosition))
            }
        }

        lastFmInfo?.listeners?.takeIf { it > 0L }?.let {
            add(TrackContextFact("Last.fm listeners", formatCount(it)))
        }
        lastFmInfo?.playcount?.takeIf { it > 0L }?.let {
            add(TrackContextFact("Last.fm plays", formatCount(it)))
        }
    }
}

@Composable
internal fun TrackContextPaneContent(
    currentTrack: TrackSummaryDto?,
    playerConnection: PlayerConnection
) {
    val apiClient = LocalPeerlessApiClient.current
    val lastFmClient = LocalLastFmClient.current
    val uriHandler = LocalUriHandler.current
    val serverUrl by apiClient.baseUrlState.collectAsState()
    val trackId = currentTrack?.id?.takeIf { it > 0 }
    val artistName = currentTrack?.artist.orEmpty()
    val albumName = currentTrack?.album.orEmpty()
    val trackTitle = currentTrack?.title.orEmpty()
    val detailsState = remember(apiClient, serverUrl, trackId) {
        mutableStateOf<TrackDetailDto?>(null)
    }
    val isLoadingState = remember(apiClient, serverUrl, trackId) {
        mutableStateOf(trackId != null)
    }
    val artistInfoState = remember(lastFmClient, artistName) {
        mutableStateOf<org.shilpo.peerless.model.LastFmArtist?>(null)
    }
    val albumInfoState = remember(lastFmClient, artistName, albumName) {
        mutableStateOf<org.shilpo.peerless.model.LastFmAlbumInfo?>(null)
    }
    val trackInfoState = remember(lastFmClient, artistName, trackTitle) {
        mutableStateOf<LastFmTrackInfo?>(null)
    }
    val isTrackInfoLoadingState = remember(lastFmClient, artistName, trackTitle) {
        mutableStateOf(artistName.isNotBlank() && trackTitle.isNotBlank())
    }
    val isLastFmLoadingState = remember(lastFmClient, artistName, albumName) {
        mutableStateOf(artistName.isNotBlank() && albumName.isNotBlank())
    }

    LaunchedEffect(apiClient, serverUrl, trackId) {
        detailsState.value = null
        if (trackId == null) {
            isLoadingState.value = false
            return@LaunchedEffect
        }

        isLoadingState.value = true
        val result = apiClient.getTrack(trackId)
        currentCoroutineContext().ensureActive()
        detailsState.value = result.getOrNull()
        isLoadingState.value = false
    }

    LaunchedEffect(lastFmClient, artistName, albumName) {
        artistInfoState.value = null
        albumInfoState.value = null
        if (artistName.isBlank() || albumName.isBlank()) {
            isLastFmLoadingState.value = false
            return@LaunchedEffect
        }

        isLastFmLoadingState.value = true
        val (artistResult, albumResult) = coroutineScope {
            val artistRequest = async { lastFmClient.getArtistInfo(artistName) }
            val albumRequest = async { lastFmClient.getAlbumInfo(artistName, albumName) }
            artistRequest.await() to albumRequest.await()
        }
        currentCoroutineContext().ensureActive()
        artistInfoState.value = artistResult.getOrNull()
        albumInfoState.value = albumResult.getOrNull()
        isLastFmLoadingState.value = false
    }

    LaunchedEffect(lastFmClient, artistName, trackTitle) {
        trackInfoState.value = null
        if (artistName.isBlank() || trackTitle.isBlank()) {
            isTrackInfoLoadingState.value = false
            return@LaunchedEffect
        }

        isTrackInfoLoadingState.value = true
        val result = lastFmClient.getTrackInfo(artistName, trackTitle)
        currentCoroutineContext().ensureActive()
        trackInfoState.value = result.getOrNull()
        isTrackInfoLoadingState.value = false
    }

    if (currentTrack == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Play a track to see its artist and album details",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val artistInfo = artistInfoState.value
    val albumInfo = albumInfoState.value
    val trackInfo = trackInfoState.value
    val trackFacts = trackContextFacts(detailsState.value, trackInfo)
    val isTrackContextLoading = isLoadingState.value || isTrackInfoLoadingState.value
    val artistExternalUrl = lastFmExternalUrl(artistInfo?.url)
    val albumExternalUrl = lastFmExternalUrl(albumInfo?.url)
    val albumFacts = buildList {
        albumInfo?.releaseDate?.takeIf(String::isNotBlank)?.let { releaseDate ->
            add(TrackContextFact("Album released", releaseDate.substringBefore(',').trim()))
        }
        albumInfo?.listeners?.takeIf { it > 0L }?.let {
            add(TrackContextFact("Last.fm listeners", formatCount(it)))
        }
        albumInfo?.playcount?.takeIf { it > 0L }?.let {
            add(TrackContextFact("Last.fm plays", formatCount(it)))
        }
    }

    val artworkUrl = apiClient.getArtworkUrl(currentTrack, size = 320)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(ArtworkShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(32.dp)
                )
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentTrack.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.artist,
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.album,
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackContextChip(formatProviderLabel(currentTrack.provider))
            TrackContextChip(if (currentTrack.is_cached) "Cached" else "On-demand")
            TrackContextChip(formatDuration(currentTrack.duration))
        }

        FilledTonalButton(
            onClick = { playerConnection.startRadio(currentTrack.toTrack()) },
            enabled = currentTrack.artist.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            PeerlessIcon(
                icon = PeerlessIcons.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Start artist radio")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "About this track",
                style = ExpressiveTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            if (isTrackContextLoading && trackFacts.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (trackFacts.isEmpty() && !isTrackContextLoading) {
                Text(
                    text = "No additional track details available",
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                trackFacts.forEachIndexed { index, fact ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.45f
                            )
                        )
                    }
                    TrackContextFactRow(fact)
                }
            }
        }

        LastFmInfoCard(
            title = "About this artist",
            summary = artistInfo?.bioSummary,
            facts = emptyList(),
            tags = artistInfo?.tags?.take(4)?.map { it.name }.orEmpty(),
            relatedArtists = artistInfo?.similarArtists?.take(3)?.joinToString(" · ")
                ?.takeIf(String::isNotBlank),
            isLoading = isLastFmLoadingState.value,
            emptyMessage = "Artist info is unavailable right now",
            externalUrl = artistExternalUrl,
            externalLinkLabel = "Open artist on Last.fm"
        )

        LastFmInfoCard(
            title = "About this album",
            summary = albumInfo?.wikiSummary,
            facts = albumFacts,
            tags = albumInfo?.tags?.take(4)?.map { it.name }.orEmpty(),
            isLoading = isLastFmLoadingState.value,
            emptyMessage = "Album info is unavailable right now",
            externalUrl = albumExternalUrl,
            externalLinkLabel = "Open album on Last.fm"
        )
    }
}

@Composable
private fun LastFmInfoCard(
    title: String,
    summary: String?,
    facts: List<TrackContextFact>,
    tags: List<String>,
    isLoading: Boolean,
    emptyMessage: String,
    externalUrl: String?,
    externalLinkLabel: String,
    relatedArtists: String? = null
) {
    val uriHandler = LocalUriHandler.current
    val hasContent = !summary.isNullOrBlank() || facts.isNotEmpty() || tags.isNotEmpty() ||
            !relatedArtists.isNullOrBlank() || externalUrl != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = ExpressiveTypography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        if (isLoading && !hasContent) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (!hasContent) {
            Text(
                text = emptyMessage,
                style = ExpressiveTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = ExpressiveTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        facts.forEachIndexed { index, fact ->
            if (index > 0 || !summary.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
            TrackContextFactRow(fact)
        }

        if (tags.isNotEmpty()) {
            Text(
                text = "Tags · ${tags.joinToString(" · ")}",
                style = ExpressiveTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!relatedArtists.isNullOrBlank()) {
            Text(
                text = "Similar artists · $relatedArtists",
                style = ExpressiveTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (externalUrl != null) {
            TextButton(
                onClick = { uriHandler.openUri(externalUrl) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(externalLinkLabel)
                Spacer(Modifier.width(8.dp))
                PeerlessIcon(
                    icon = PeerlessIcons.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackContextFactRow(fact: TrackContextFact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = fact.label,
            style = ExpressiveTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = fact.value,
            style = ExpressiveTypography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TrackContextChip(label: String) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun formatCount(count: Long): String =
    count.toString().reversed().chunked(3).joinToString(",").reversed()
