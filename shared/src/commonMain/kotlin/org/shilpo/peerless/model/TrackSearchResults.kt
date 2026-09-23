package org.shilpo.peerless.model

internal object TrackSearchResults {
    fun merge(
        response: SearchResponse,
        query: String,
        filter: SearchFilter,
        baseUrl: String,
        lastFmMatches: List<LastFmTrackMatch>
    ): List<CanonicalTrack> {
        val cached = response.cached.filter { track ->
            when (filter) {
                SearchFilter.APPLE_MUSIC -> track.provider.contains("apple", ignoreCase = true)
                SearchFilter.QOBUZ -> track.provider.contains("qobuz", ignoreCase = true)
                SearchFilter.TRACKS -> true
                SearchFilter.ALBUMS -> track.album.contains(query, ignoreCase = true)
                SearchFilter.ARTISTS -> track.artist.contains(query, ignoreCase = true)
                else -> true
            }
        }
        val live = if (filter == SearchFilter.CACHED) {
            emptyList()
        } else {
            response.live.filter { track ->
                when (filter) {
                    SearchFilter.APPLE_MUSIC -> track.provider.contains("apple", ignoreCase = true)
                    SearchFilter.QOBUZ -> track.provider.contains("qobuz", ignoreCase = true)
                    SearchFilter.TRACKS -> true
                    SearchFilter.ALBUMS -> track.album.contains(query, ignoreCase = true)
                    SearchFilter.ARTISTS -> track.artist.contains(query, ignoreCase = true)
                    else -> true
                }
            }
        }

        val canonical = CanonicalDeduplicator.deduplicate(cached, live, baseUrl)
        return rank(canonical, lastFmMatches)
    }

    private fun rank(
        backendResults: List<CanonicalTrack>,
        lastFmMatches: List<LastFmTrackMatch>
    ): List<CanonicalTrack> {
        if (backendResults.size < 2 || lastFmMatches.isEmpty()) return backendResults

        val matchRank = mutableMapOf<String, Int>()
        val titleRanks = mutableMapOf<String, Int>()
        val titleCounts = mutableMapOf<String, Int>()
        lastFmMatches.forEachIndexed { index, match ->
            val key = key(match.title, match.artist)
            if (key !in matchRank) matchRank[key] = index
            val title = normalize(match.title)
            if (title !in titleRanks) titleRanks[title] = index
            titleCounts[title] = (titleCounts[title] ?: 0) + 1
        }

        return backendResults.withIndex()
            .sortedWith(
                compareBy<IndexedValue<CanonicalTrack>> { (_, track) ->
                    val title = normalize(track.title)
                    matchRank[key(track.title, track.artist)]
                        ?: titleRanks[title].takeIf { titleCounts[title] == 1 }
                        ?: Int.MAX_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    private fun key(title: String, artist: String): String =
        "${normalize(title)}|${normalize(artist)}"

    private fun normalize(value: String): String = buildString {
        value.lowercase().forEach { character ->
            if (character.isLetterOrDigit()) append(character)
        }
    }
}
