package org.shilpo.peerless.model

object CanonicalDeduplicator {
    fun normalize(metadataText: String): String =
        metadataText.trim().lowercase().replace(Regex("\\s+"), " ")

    data class DeduplicationItem(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val artworkUrl: String? = null,
        val isrc: String? = null,
        val sources: List<TrackSource> = emptyList()
    )

    fun matches(firstTrack: DeduplicationItem, secondTrack: DeduplicationItem): Boolean {
        // 1) Same provider & providerTrackId
        for (firstSource in firstTrack.sources) {
            for (secondSource in secondTrack.sources) {
                if (firstSource.provider != Provider.Unknown &&
                    firstSource.provider == secondSource.provider &&
                    firstSource.providerTrackId.isNotBlank() &&
                    firstSource.providerTrackId == secondSource.providerTrackId
                ) {
                    return true
                }
            }
        }

        // 2) Same ISRC (when available and non-blank)
        val firstIsrc = firstTrack.isrc?.trim()
        val secondIsrc = secondTrack.isrc?.trim()
        if (!firstIsrc.isNullOrBlank() && !secondIsrc.isNullOrBlank() &&
            firstIsrc.equals(secondIsrc, ignoreCase = true)
        ) {
            return true
        }

        // 3) Normalized title + normalized artist + duration within 3 seconds.
        val firstTitle = normalize(firstTrack.title)
        val secondTitle = normalize(secondTrack.title)
        val firstArtist = normalize(firstTrack.artist)
        val secondArtist = normalize(secondTrack.artist)
        if (firstTitle.isNotEmpty() && firstTitle == secondTitle &&
            firstArtist.isNotEmpty() && firstArtist == secondArtist
        ) {
            if (firstTrack.durationSeconds <= 0 || secondTrack.durationSeconds <= 0 ||
                kotlin.math.abs(firstTrack.durationSeconds - secondTrack.durationSeconds) <= 3
            ) {
                return true
            }
        }

        return false
    }

    private class DisjointSet(size: Int) {
        val parent = IntArray(size) { it }
        fun find(index: Int): Int {
            var root = index
            while (root != parent[root]) {
                root = parent[root]
            }
            var currentIndex = index
            while (currentIndex != root) {
                val nextIndex = parent[currentIndex]
                parent[currentIndex] = root
                currentIndex = nextIndex
            }
            return root
        }

        fun union(firstIndex: Int, secondIndex: Int) {
            val firstRoot = find(firstIndex)
            val secondRoot = find(secondIndex)
            if (firstRoot != secondRoot) {
                parent[firstRoot] = secondRoot
            }
        }
    }

    private fun groupItems(items: List<DeduplicationItem>): List<List<DeduplicationItem>> {
        if (items.isEmpty()) return emptyList()
        val itemCount = items.size
        val disjointSet = DisjointSet(itemCount)
        for (firstIndex in 0 until itemCount) {
            for (secondIndex in firstIndex + 1 until itemCount) {
                if (matches(items[firstIndex], items[secondIndex])) {
                    disjointSet.union(firstIndex, secondIndex)
                }
            }
        }
        val groups = LinkedHashMap<Int, MutableList<DeduplicationItem>>()
        for (index in 0 until itemCount) {
            val root = disjointSet.find(index)
            groups.getOrPut(root) { mutableListOf() }.add(items[index])
        }
        return groups.values.toList()
    }

    private fun mergeGroup(group: List<DeduplicationItem>): CanonicalTrack {
        val mergedSources = mutableListOf<TrackSource>()
        for (item in group) {
            for (source in item.sources) {
                val existingIndex = mergedSources.indexOfFirst {
                    it.provider == source.provider && it.providerTrackId == source.providerTrackId
                }
                if (existingIndex >= 0) {
                    val existingSource = mergedSources[existingIndex]
                    if (source.isCached && !existingSource.isCached) {
                        mergedSources[existingIndex] = source
                    } else if (source.isCached == existingSource.isCached) {
                        if (sourceComparator(false).compare(source, existingSource) > 0) {
                            mergedSources[existingIndex] = source
                        }
                    }
                } else {
                    mergedSources.add(source)
                }
            }
        }

        val sortedSources = mergedSources.sortedWith { leftSource, rightSource ->
            if (leftSource.isCached != rightSource.isCached) {
                if (leftSource.isCached) -1 else 1
            } else {
                sourceComparator(false).compare(rightSource, leftSource)
            }
        }

        val cachedItem = group.firstOrNull { item -> item.sources.any { source -> source.isCached } } ?: group.first()
        val bestItem = group.maxByOrNull { it.durationSeconds } ?: cachedItem

        val id = group.firstNotNullOfOrNull { item ->
            item.id.takeIf { it.isNotBlank() && !it.contains("_") }
        } ?: cachedItem.id

        val artworkUrl = group.firstNotNullOfOrNull { item ->
            item.artworkUrl?.takeIf { candidateUrl -> candidateUrl.isNotBlank() }
        }
        val isrc = group.firstNotNullOfOrNull { item ->
            item.isrc?.takeIf { candidateIsrc -> candidateIsrc.isNotBlank() }
        }
        val title = cachedItem.title.ifBlank { bestItem.title }
        val artist = cachedItem.artist.ifBlank { bestItem.artist }
        val album = group.firstNotNullOfOrNull { item ->
            item.album.takeIf { albumName -> albumName.isNotBlank() }
        } ?: cachedItem.album
        val duration = if (cachedItem.durationSeconds > 0) cachedItem.durationSeconds else bestItem.durationSeconds

        return CanonicalTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            artworkUrl = artworkUrl,
            isrc = isrc,
            sources = sortedSources
        )
    }

    fun deduplicate(
        cachedTracks: List<TrackSummaryDto>,
        liveTracks: List<UncachedTrackDto>,
        baseUrl: String = ""
    ): List<CanonicalTrack> {
        val items = mutableListOf<DeduplicationItem>()

        for (cached in cachedTracks) {
            val providerEnum = Provider.fromString(cached.provider)
            val codecEnum = Codec.fromString(cached.codec)
            val source = TrackSource(
                id = cached.id,
                provider = providerEnum,
                providerTrackId = cached.track_id,
                codec = codecEnum,
                bitDepth = cached.bit_depth,
                sampleRate = cached.sample_rate,
                isCached = cached.is_cached
            )
            val artworkUrl = if (cached.id > 0 && baseUrl.isNotBlank()) {
                "${baseUrl.trimEnd('/')}/api/v1/assets/tracks/${cached.id}/artwork"
            } else cached.artwork_url

            items.add(
                DeduplicationItem(
                    id = if (cached.id > 0) cached.id.toString() else "${cached.provider}_${cached.track_id}",
                    title = cached.title,
                    artist = cached.artist,
                    album = cached.album,
                    durationSeconds = cached.duration,
                    artworkUrl = artworkUrl,
                    sources = listOf(source)
                )
            )
        }

        for (live in liveTracks) {
            val providerEnum = Provider.fromString(live.provider)
            val codecEnum = if (providerEnum == Provider.Qobuz) Codec.Flac else Codec.Alac
            val source = TrackSource(
                id = 0,
                provider = providerEnum,
                providerTrackId = live.track_id,
                codec = codecEnum,
                isCached = false
            )
            items.add(
                DeduplicationItem(
                    id = "${live.provider}_${live.track_id}",
                    title = live.title,
                    artist = live.artist,
                    album = live.album,
                    durationSeconds = live.duration,
                    artworkUrl = live.artwork_url,
                    sources = listOf(source)
                )
            )
        }

        return groupItems(items).map { mergeGroup(it) }
    }

    fun deduplicateTracks(
        tracks: List<TrackSummaryDto>,
        baseUrl: String = ""
    ): List<CanonicalTrack> {
        val cached = tracks.filter { it.is_cached }
        val live = tracks.filter { !it.is_cached }.map {
            UncachedTrackDto(
                provider = it.provider,
                item_id = it.track_id,
                track_id = it.track_id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                duration = it.duration,
                is_cached = false,
                artwork_url = it.artwork_url
            )
        }
        return deduplicate(cached, live, baseUrl)
    }

    fun deduplicateCanonical(
        tracks: List<CanonicalTrack>
    ): List<CanonicalTrack> {
        val items = tracks.map {
            DeduplicationItem(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                durationSeconds = it.durationSeconds,
                artworkUrl = it.artworkUrl,
                isrc = it.isrc,
                sources = it.sources
            )
        }
        return groupItems(items).map { mergeGroup(it) }
    }
}
