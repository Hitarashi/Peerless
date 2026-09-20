package org.shilpo.peerless.home

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.CanonicalDeduplicator
import org.shilpo.peerless.model.CanonicalTrack
import org.shilpo.peerless.model.LastFmSimilarTrack
import org.shilpo.peerless.model.LastFmUserTrack
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.toTrack
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.TrackRecommendationProvider
import org.shilpo.peerless.player.recommendationTrackKey
import kotlin.math.pow
import kotlin.time.Clock

data class HomeFeedItem(
    val track: TrackSummaryDto,
    val canonicalTrack: CanonicalTrack? = null
)

data class HomeFeedState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val recentTracks: List<HomeFeedItem> = emptyList(),
    val quickPicks: List<HomeFeedItem> = emptyList(),
    val similarToTaste: List<HomeFeedItem> = emptyList()
)

/** Blends Last.fm listening windows and resolves recommendations through Peerless. */
class HomeFeedRepository(
    private val lastFmClient: LastFmClient,
    private val apiClient: PeerlessApiClient
) : TrackRecommendationProvider {
    private val similarCacheMutex = Mutex()
    private val similarCache = mutableMapOf<String, SimilarCacheEntry>()

    suspend fun load(username: String, libraryTracks: List<TrackSummaryDto>): HomeFeedState {
        if (username.isBlank()) return HomeFeedState()

        val (recentResult, weeklyResult, overallResult) = coroutineScope {
            val recent =
                async { lastFmClient.getRecentTracks(username, limit = LAST_FM_TRACK_LIMIT) }
            val weekly = async {
                lastFmClient.getTopTracks(username, limit = LAST_FM_TRACK_LIMIT, period = "7day")
            }
            val overall = async {
                lastFmClient.getTopTracks(username, limit = LAST_FM_TRACK_LIMIT, period = "overall")
            }
            Triple(recent.await(), weekly.await(), overall.await())
        }

        if (recentResult.isFailure && weeklyResult.isFailure && overallResult.isFailure) {
            return HomeFeedState(hasError = true)
        }

        val recentSignals = recentResult.getOrDefault(emptyList())
            .distinctBy(::trackKey)
            .take(RECENT_TRACK_COUNT)
        val weeklySignals = weeklyResult.getOrDefault(emptyList())
            .sortedByDescending { it.playCount }
            .distinctBy(::trackKey)
        val overallSignals = overallResult.getOrDefault(emptyList())
            .sortedByDescending { it.playCount }
            .distinctBy(::trackKey)

        val quickPickSignals = rankListeningHistory(
            recent = recentSignals,
            weekly = weeklySignals,
            overall = overallSignals
        ).take(QUICK_PICK_COUNT)
        val knownSignals = (recentSignals + weeklySignals + overallSignals)
            .distinctBy(::trackKey)
        val listeningMatches = resolveSignals(
            (quickPickSignals + recentSignals).distinctBy(::trackKey),
            libraryTracks
        ).associateBy { trackKey(it.first) }

        val similarSignals = findSimilarSignals(
            recent = recentSignals,
            weekly = weeklySignals,
            overall = overallSignals,
            excludedKeys = knownSignals.mapTo(mutableSetOf(), ::trackKey)
        )
        val resolvedSimilar = resolveSignals(similarSignals, libraryTracks)
            .associateBy { trackKey(it.first) }

        return HomeFeedState(
            recentTracks = recentSignals.mapNotNull { listeningMatches[trackKey(it)]?.second },
            quickPicks = quickPickSignals.mapNotNull { listeningMatches[trackKey(it)]?.second },
            similarToTaste = similarSignals.mapNotNull { resolvedSimilar[trackKey(it)]?.second }
        )
    }

    override suspend fun recommend(
        seed: Track,
        excludedTrackKeys: Set<String>,
        limit: Int
    ): List<Track> {
        if (limit <= 0) return emptyList()
        val similar = getSimilarTracks(seed.artist, seed.title, RADIO_SIMILAR_LIMIT)
            .filterNot { trackKey(it.artist, it.title) in excludedTrackKeys }
            .distinctBy { trackKey(it.artist, it.title) }
            .take((limit * 3).coerceAtMost(RADIO_RESOLVE_LIMIT))
            .map { LastFmUserTrack(it.title, it.artist) }
        if (similar.isEmpty()) return emptyList()

        val playable = resolveSignals(similar, emptyList())
            .map { it.second.track.toTrack() }
            .filter { it.isCached }
            .distinctBy { trackKey(it.artist, it.title) }
            .filterNot { trackKey(it.artist, it.title) in excludedTrackKeys }
        val firstFromArtist = playable.distinctBy { normalize(it.artist) }
        val artistRepeatTracks = playable.filterNot { candidate ->
            firstFromArtist.any { it.id == candidate.id }
        }
        return (firstFromArtist + artistRepeatTracks).take(limit)
    }

    private fun rankListeningHistory(
        recent: List<LastFmUserTrack>,
        weekly: List<LastFmUserTrack>,
        overall: List<LastFmUserTrack>
    ): List<LastFmUserTrack> {
        val candidates = linkedMapOf<String, RankedSignal>()
        fun addWindow(tracks: List<LastFmUserTrack>, windowWeight: Double) {
            tracks.forEachIndexed { index, signal ->
                val key = trackKey(signal)
                val rankContribution = windowWeight / (index + 1.0).pow(RANK_EXPONENT)
                val existing = candidates[key]
                if (existing == null) {
                    candidates[key] = RankedSignal(signal, rankContribution)
                } else {
                    candidates[key] = existing.copy(score = existing.score + rankContribution)
                }
            }
        }

        addWindow(recent, RECENT_WEIGHT)
        addWindow(weekly, WEEKLY_WEIGHT)
        addWindow(overall, OVERALL_WEIGHT)
        return candidates.values
            .sortedByDescending { it.score }
            .map { it.signal }
    }

    private suspend fun findSimilarSignals(
        recent: List<LastFmUserTrack>,
        weekly: List<LastFmUserTrack>,
        overall: List<LastFmUserTrack>,
        excludedKeys: Set<String>
    ): List<LastFmUserTrack> = coroutineScope {
        val seeds = buildList {
            recent.take(2).forEach { add(WeightedSeed(it, RECENT_SEED_WEIGHT)) }
            weekly.take(2).forEach { add(WeightedSeed(it, WEEKLY_SEED_WEIGHT)) }
            overall.take(2).forEach { add(WeightedSeed(it, OVERALL_SEED_WEIGHT)) }
        }.distinctBy { trackKey(it.track) }
            .take(SIMILAR_SEED_LIMIT)
        val semaphore = Semaphore(SIMILAR_REQUEST_CONCURRENCY)
        val results = seeds.map { seed ->
            async {
                semaphore.withPermit {
                    seed to getSimilarTracks(
                        seed.track.artist,
                        seed.track.title,
                        SIMILAR_TRACK_LIMIT
                    )
                }
            }
        }.awaitAll()

        val candidates = linkedMapOf<String, RankedSimilar>()
        results.forEach { (seed, tracks) ->
            tracks.forEach candidateLoop@{ candidate ->
                val key = trackKey(candidate.artist, candidate.title)
                if (key in excludedKeys) return@candidateLoop
                val contribution = candidate.match.coerceIn(0.0, 1.0) * seed.weight
                val prior = candidates[key]
                candidates[key] = if (prior == null) {
                    RankedSimilar(candidate, contribution, 1)
                } else {
                    prior.copy(score = prior.score + contribution, seedCount = prior.seedCount + 1)
                }
            }
        }

        val remaining = candidates.values.toMutableList()
        val selected = mutableListOf<LastFmUserTrack>()
        val artistCounts = mutableMapOf<String, Int>()
        while (remaining.isNotEmpty() && selected.size < SIMILAR_HOME_LIMIT) {
            val best = remaining.maxByOrNull { candidate ->
                candidate.score + (candidate.seedCount - 1) * CONSENSUS_BONUS -
                        (artistCounts[normalize(candidate.track.artist)]
                            ?: 0) * ARTIST_DIVERSITY_PENALTY
            } ?: break
            remaining.remove(best)
            selected += LastFmUserTrack(best.track.title, best.track.artist)
            val artist = normalize(best.track.artist)
            artistCounts[artist] = (artistCounts[artist] ?: 0) + 1
        }
        selected
    }

    private suspend fun getSimilarTracks(
        artist: String,
        title: String,
        limit: Int
    ): List<LastFmSimilarTrack> {
        val key = trackKey(artist, title)
        val now = Clock.System.now().toEpochMilliseconds()
        similarCacheMutex.withLock {
            similarCache[key]?.takeIf { it.expiresAtEpochMs > now }?.let { return it.tracks }
        }

        val result = lastFmClient.getSimilarTracks(artist, title, limit)
        val tracks = result.getOrDefault(emptyList())
        if (result.isSuccess) {
            similarCacheMutex.withLock {
                if (similarCache.size >= SIMILAR_CACHE_CAPACITY) similarCache.clear()
                similarCache[key] = SimilarCacheEntry(
                    expiresAtEpochMs = now + SIMILAR_CACHE_TTL_MS,
                    tracks = tracks
                )
            }
        }
        return tracks
    }

    private suspend fun resolveSignals(
        signals: List<LastFmUserTrack>,
        libraryTracks: List<TrackSummaryDto>
    ): List<Pair<LastFmUserTrack, HomeFeedItem>> = coroutineScope {
        val searchSemaphore = Semaphore(SEARCH_CONCURRENCY)
        signals.map { signal ->
            async {
                val fromLibrary = bestLibraryMatch(libraryTracks, signal)
                val resolved = fromLibrary ?: searchSemaphore.withPermit { searchTrack(signal) }
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

    private fun trackKey(track: LastFmUserTrack): String = trackKey(track.artist, track.title)

    private fun trackKey(artist: String, title: String): String =
        recommendationTrackKey(artist, title)

    private fun normalize(value: String): String = buildString {
        value.lowercase().forEach { character ->
            append(if (character.isLetterOrDigit()) character else ' ')
        }
    }.trim().replace(WHITESPACE, " ")

    private data class RankedSignal(val signal: LastFmUserTrack, val score: Double)
    private data class WeightedSeed(val track: LastFmUserTrack, val weight: Double)
    private data class RankedSimilar(
        val track: LastFmSimilarTrack,
        val score: Double,
        val seedCount: Int
    )

    private data class SimilarCacheEntry(
        val expiresAtEpochMs: Long,
        val tracks: List<LastFmSimilarTrack>
    )

    private companion object {
        const val LAST_FM_TRACK_LIMIT = 30
        const val RECENT_TRACK_COUNT = 8
        const val QUICK_PICK_COUNT = 6
        const val SIMILAR_HOME_LIMIT = 8
        const val SIMILAR_SEED_LIMIT = 5
        const val SIMILAR_TRACK_LIMIT = 30
        const val RADIO_SIMILAR_LIMIT = 40
        const val RADIO_RESOLVE_LIMIT = 24
        const val SEARCH_RESULT_LIMIT = 12
        const val SEARCH_CONCURRENCY = 3
        const val SIMILAR_REQUEST_CONCURRENCY = 3
        const val SIMILAR_CACHE_CAPACITY = 128
        const val SIMILAR_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        const val RANK_EXPONENT = 0.7
        const val RECENT_WEIGHT = 3.0
        const val WEEKLY_WEIGHT = 2.0
        const val OVERALL_WEIGHT = 1.8
        const val RECENT_SEED_WEIGHT = 1.2
        const val WEEKLY_SEED_WEIGHT = 1.0
        const val OVERALL_SEED_WEIGHT = 0.85
        const val CONSENSUS_BONUS = 0.12
        const val ARTIST_DIVERSITY_PENALTY = 0.2
        val WHITESPACE = Regex("\\s+")
    }
}

val LocalHomeFeedRepository = staticCompositionLocalOf<HomeFeedRepository> {
    error("No HomeFeedRepository provided")
}
