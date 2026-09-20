package org.shilpo.peerless.home

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.CanonicalTrack
import org.shilpo.peerless.model.LastFmUserTrack
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient

data class HomeFeedItem(
    val track: TrackSummaryDto,
    val canonicalTrack: CanonicalTrack? = null
)

data class HomeFeedState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val recentTracks: List<HomeFeedItem> = emptyList(),
    val quickPicks: List<HomeFeedItem> = emptyList(),
    val newDiscoveries: List<HomeFeedItem> = emptyList()
)

/** Combines public Last.fm listening signals with tracks Peerless can play or rip. */
class HomeFeedRepository(
    private val lastFmClient: LastFmClient,
    private val apiClient: PeerlessApiClient
) {
    suspend fun load(username: String, libraryTracks: List<TrackSummaryDto>): HomeFeedState {
        if (username.isBlank()) return HomeFeedState()

        val (recentResult, topResult) = coroutineScope {
            val recent = async { lastFmClient.getRecentTracks(username, limit = LAST_FM_TRACK_LIMIT) }
            val top = async { lastFmClient.getTopTracks(username, limit = LAST_FM_TRACK_LIMIT) }
            recent.await() to top.await()
        }

        if (recentResult.isFailure && topResult.isFailure) {
            return HomeFeedState(hasError = true)
        }

        val recentSignals = recentResult.getOrDefault(emptyList())
            .distinctBy(::trackKey)
            .take(RECENT_TRACK_COUNT)
        val topSignals = topResult.getOrDefault(emptyList())
            .sortedByDescending { it.playCount }
            .distinctBy(::trackKey)
        val recentKeys = recentSignals.mapTo(mutableSetOf(), ::trackKey)
        val quickPickSignals = (topSignals.filterNot { trackKey(it) in recentKeys } + topSignals)
            .distinctBy(::trackKey)
            .take(QUICK_PICK_COUNT)

        val matchSignals = (recentSignals + quickPickSignals).distinctBy(::trackKey)
        val resolvedTracks = resolveSignals(matchSignals, libraryTracks)
            .associateBy { trackKey(it.first) }

        val recentItems = recentSignals.mapNotNull { signal ->
            resolvedTracks[trackKey(signal)]?.second
        }
        val quickPicks = quickPickSignals.mapNotNull { signal ->
            resolvedTracks[trackKey(signal)]?.second
        }

        val listeningArtist = recentSignals.firstOrNull()?.artist
            ?: topSignals.firstOrNull()?.artist
        val discoveryItems = findNewDiscoveries(listeningArtist, libraryTracks)

        return HomeFeedState(
            recentTracks = recentItems,
            quickPicks = quickPicks,
            newDiscoveries = discoveryItems
        )
    }

    private suspend fun resolveSignals(
        signals: List<LastFmUserTrack>,
        libraryTracks: List<TrackSummaryDto>
    ): List<Pair<LastFmUserTrack, HomeFeedItem>> = coroutineScope {
        val searchSemaphore = Semaphore(SEARCH_CONCURRENCY)
        signals.map { signal ->
            async {
                val fromLibrary = bestLibraryMatch(libraryTracks, signal)
                val resolved = fromLibrary ?: searchSemaphore.withPermit {
                    searchTrack(signal)
                }
                resolved?.let { signal to it }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun searchTrack(signal: LastFmUserTrack): HomeFeedItem? {
        val response = apiClient.search(
            query = "${signal.title} ${signal.artist}",
            limit = SEARCH_RESULT_LIMIT
        ).getOrNull() ?: return null

        val canonicalTracks = CanonicalDeduplicator.deduplicate(
            cachedTracks = response.cached,
            liveTracks = response.live,
            baseUrl = apiClient.baseUrl
        )
        val canonicalTrack = bestCanonicalMatch(canonicalTracks, signal) ?: return null
        return HomeFeedItem(canonicalTrack.toSummaryDto(), canonicalTrack)
    }

    private suspend fun findNewDiscoveries(
        listeningArtist: String?,
        libraryTracks: List<TrackSummaryDto>
    ): List<HomeFeedItem> {
        if (listeningArtist.isNullOrBlank()) return emptyList()
        val similarArtists = lastFmClient.getArtistInfo(listeningArtist)
            .getOrNull()
            ?.similarArtists
            ?.distinctBy(::normalize)
            ?.filter { normalize(it) != normalize(listeningArtist) }
            ?.take(RELATED_ARTIST_LIMIT)
            .orEmpty()

        for (artist in similarArtists) {
            val fromLibrary = libraryTracks.filter { normalize(it.artist) == normalize(artist) }
            val tracks = if (fromLibrary.isNotEmpty()) {
                fromLibrary.mapNotNull { candidate ->
                    bestLibraryMatch(
                        candidates = listOf(candidate),
                        signal = LastFmUserTrack(
                            title = candidate.title,
                            artist = artist,
                            playCount = 0
                        ),
                        siblingCandidates = libraryTracks
                    )
                }.distinctBy { item -> item.canonicalTrack?.isrc ?: trackKey(item.track) }
                    .take(RELATED_TRACK_COUNT)
            } else {
                val response = apiClient.search(artist, limit = SEARCH_RESULT_LIMIT).getOrNull()
                    ?: continue
                CanonicalDeduplicator.deduplicate(
                    cachedTracks = response.cached,
                    liveTracks = response.live,
                    baseUrl = apiClient.baseUrl
                ).filter { normalize(it.artist) == normalize(artist) }
                    .map { HomeFeedItem(it.toSummaryDto(), it) }
                    .take(RELATED_TRACK_COUNT)
            }
            if (tracks.isNotEmpty()) return tracks
        }

        return emptyList()
    }

    private fun bestMatch(
        candidates: List<TrackSummaryDto>,
        signal: LastFmUserTrack
    ): TrackSummaryDto? = candidates
        .mapNotNull { candidate -> matchScore(candidate, signal)?.let { candidate to it } }
        .maxWithOrNull(
            compareBy<Pair<TrackSummaryDto, Int>> { it.second }
                .thenBy { it.first.is_cached }
        )
        ?.first

    private fun bestLibraryMatch(
        candidates: List<TrackSummaryDto>,
        signal: LastFmUserTrack,
        siblingCandidates: List<TrackSummaryDto> = candidates
    ): HomeFeedItem? {
        val bestTrack = bestMatch(candidates, signal) ?: return null
        val matchingIsrc = bestTrack.isrc?.takeIf { it.isNotBlank() }
        val relatedSources = if (matchingIsrc == null) {
            listOf(bestTrack)
        } else {
            siblingCandidates.filter { it.isrc?.equals(matchingIsrc, ignoreCase = true) == true }
        }
        val canonicalTrack = CanonicalDeduplicator.deduplicateTracks(
            relatedSources,
            baseUrl = apiClient.baseUrl
        ).firstOrNull() ?: return HomeFeedItem(bestTrack)
        return HomeFeedItem(canonicalTrack.toSummaryDto(), canonicalTrack)
    }

    private fun bestCanonicalMatch(
        candidates: List<CanonicalTrack>,
        signal: LastFmUserTrack
    ): CanonicalTrack? = candidates
        .mapNotNull { candidate ->
            matchScore(candidate.toSummaryDto(), signal)?.let { candidate to it }
        }
        .maxWithOrNull(
            compareBy<Pair<CanonicalTrack, Int>> { it.second }
                .thenBy { it.first.isCached }
        )
        ?.first

    private fun matchScore(candidate: TrackSummaryDto, signal: LastFmUserTrack): Int? {
        val candidateTitle = normalize(candidate.title)
        val requestedTitle = normalize(signal.title)
        val candidateArtist = normalize(candidate.artist)
        val requestedArtist = normalize(signal.artist)

        val artistScore = when {
            candidateArtist == requestedArtist -> 5
            candidateArtist.contains(requestedArtist) && requestedArtist.length >= 3 -> 3
            requestedArtist.contains(candidateArtist) && candidateArtist.length >= 3 -> 3
            else -> 0
        }
        val titleScore = when {
            candidateTitle == requestedTitle -> 6
            candidateTitle.startsWith(requestedTitle) && requestedTitle.length >= 4 -> 4
            requestedTitle.startsWith(candidateTitle) && candidateTitle.length >= 4 -> 4
            candidateTitle.contains(requestedTitle) && requestedTitle.length >= 4 -> 3
            requestedTitle.contains(candidateTitle) && candidateTitle.length >= 4 -> 3
            else -> 0
        }

        return (artistScore + titleScore).takeIf { artistScore > 0 && titleScore > 0 && it >= 8 }
    }

    private fun trackKey(track: LastFmUserTrack): String =
        "${normalize(track.artist)}|${normalize(track.title)}"

    private fun trackKey(track: TrackSummaryDto): String =
        "${normalize(track.artist)}|${normalize(track.title)}"

    private fun normalize(value: String): String = buildString {
        value.lowercase().forEach { character ->
            append(if (character.isLetterOrDigit()) character else ' ')
        }
    }.trim().replace(WHITESPACE, " ")

    private companion object {
        const val LAST_FM_TRACK_LIMIT = 30
        const val RECENT_TRACK_COUNT = 6
        const val QUICK_PICK_COUNT = 6
        const val RELATED_ARTIST_LIMIT = 2
        const val RELATED_TRACK_COUNT = 4
        const val SEARCH_RESULT_LIMIT = 12
        const val SEARCH_CONCURRENCY = 3
        val WHITESPACE = Regex("\\s+")
    }
}
