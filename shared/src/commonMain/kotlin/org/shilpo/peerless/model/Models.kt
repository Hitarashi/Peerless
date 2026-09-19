package org.shilpo.peerless.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
enum class Provider(val raw: String, val displayName: String) {
    Apple("apple", "Apple Music"),
    Qobuz("qobuz", "Qobuz"),
    Unknown("unknown", "Unknown");

    companion object {
        fun fromString(value: String): Provider = when (value.lowercase()) {
            "apple" -> Apple
            "qobuz" -> Qobuz
            else -> Unknown
        }
    }
}

@Serializable
enum class Codec(val raw: String, val displayName: String) {
    Alac("alac", "ALAC"),
    Flac("flac", "FLAC"),
    Aac("aac", "AAC"),
    Opus("opus", "Opus"),
    Ec3("ec3", "Dolby Atmos"),
    Unknown("unknown", "Unknown");

    companion object {
        fun fromString(value: String): Codec = when (value.lowercase().trim()) {
            "alac" -> Alac
            "flac" -> Flac
            "aac" -> Aac
            "opus" -> Opus
            "ec3", "ec-3", "eac3", "atmos", "dolby", "dolby atmos" -> Ec3
            else -> Unknown
        }
    }
}

@Serializable
enum class SearchFilter(val label: String, val providerQuery: String? = null) {
    ALL("All"),
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    APPLE_MUSIC("Apple Music", "apple"),
    QOBUZ("Qobuz", "qobuz"),
    CACHED("Cached")
}

fun sourceComparator(spatialSupported: Boolean = false): Comparator<TrackSource> = Comparator { a, b ->
    if (spatialSupported) {
        val aIsEc3 = a.codec == Codec.Ec3
        val bIsEc3 = b.codec == Codec.Ec3
        if (aIsEc3 != bIsEc3) {
            return@Comparator if (aIsEc3) 1 else -1
        }
    }
    val aSr = a.sampleRate ?: 44100
    val bSr = b.sampleRate ?: 44100
    if (aSr != bSr) {
        return@Comparator aSr.compareTo(bSr)
    }
    val aBd = a.bitDepth ?: 16
    val bBd = b.bitDepth ?: 16
    if (aBd != bBd) {
        return@Comparator aBd.compareTo(bBd)
    }
    val aCodecScore = when (a.codec) {
        Codec.Alac -> 2
        Codec.Flac -> 1
        else -> 0
    }
    val bCodecScore = when (b.codec) {
        Codec.Alac -> 2
        Codec.Flac -> 1
        else -> 0
    }
    if (aCodecScore != bCodecScore) {
        return@Comparator aCodecScore.compareTo(bCodecScore)
    }
    0
}

fun isSourceSuperior(a: TrackSource, b: TrackSource, spatialSupported: Boolean = false): Boolean {
    return sourceComparator(spatialSupported).compare(a, b) > 0
}

@Serializable
data class AudioSpecs(
    val codec: Codec,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val bitrateKbps: Int? = null
) {
    val isHiRes: Boolean
        get() = (bitDepth ?: 16) > 16 || (sampleRate ?: 44100) > 48000 || codec == Codec.Ec3

    val effectiveBitrateKbps: Int?
        get() {
            if (bitrateKbps != null && bitrateKbps > 0) return bitrateKbps
            if (bitDepth == null || sampleRate == null) return null
            val pcmBitrate = (bitDepth * sampleRate * 2) / 1000
            return when (codec) {
                Codec.Alac -> if (bitDepth == 24 && sampleRate == 44100) 1671 else (pcmBitrate * 0.78).toInt()
                Codec.Flac -> (pcmBitrate * 0.65).toInt()
                Codec.Aac -> 256
                Codec.Opus -> 128
                Codec.Ec3 -> 768
                Codec.Unknown -> (pcmBitrate * 0.70).toInt()
            }
        }

    val badgeText: String
        get() = if (codec == Codec.Ec3) {
            "DOLBY ATMOS"
        } else buildString {
            if (bitDepth != null && bitDepth >= 24) append("${bitDepth} BIT  ")
            if (sampleRate != null) {
                val khz = if (sampleRate % 1000 == 0) {
                    "${sampleRate / 1000}"
                } else {
                    "${sampleRate / 1000.0}"
                }
                append("${khz} KHZ  ")
            }
            if (bitrateKbps != null && bitrateKbps > 0) {
                append("${bitrateKbps} KBPS  ")
            }
            append(codec.displayName.uppercase())
        }.trim()

    val fullBadgeText: String
        get() = if (codec == Codec.Ec3) {
            "DOLBY ATMOS"
        } else buildString {
            if (bitDepth != null && bitDepth >= 24) append("${bitDepth} BIT  ")
            if (sampleRate != null) {
                val khz = if (sampleRate % 1000 == 0) {
                    "${sampleRate / 1000}"
                } else {
                    "${sampleRate / 1000.0}"
                }
                append("${khz} KHZ  ")
            }
            val kbps = effectiveBitrateKbps
            if (kbps != null && kbps > 0) {
                append("${kbps} KBPS  ")
            }
            append(codec.displayName.uppercase())
        }.trim()
}

@Serializable
data class TrackSource(
    val id: Int,
    val provider: Provider,
    val providerTrackId: String,
    val codec: Codec,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val bitrateKbps: Int? = null,
    val isCached: Boolean = true,
    val fileSize: Long? = null
) {
    val audioSpecs: AudioSpecs
        get() = AudioSpecs(
            codec = codec,
            bitDepth = bitDepth,
            sampleRate = sampleRate,
            bitrateKbps = bitrateKbps
        )
}

@Serializable
data class CanonicalTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val artworkUrl: String? = null,
    val isrc: String? = null,
    val sources: List<TrackSource> = emptyList()
) {
    val isCached: Boolean
        get() = sources.any { it.isCached }

    val bestSource: TrackSource?
        get() = resolveBestSource(sources, spatialSupported = false)

    fun resolveBestSource(
        candidateSources: List<TrackSource> = sources,
        spatialSupported: Boolean = false
    ): TrackSource? {
        if (candidateSources.isEmpty()) return null
        return candidateSources.maxWithOrNull(sourceComparator(spatialSupported)) ?: candidateSources.firstOrNull()
    }

    fun immediatePlaySource(spatialSupported: Boolean = false): TrackSource? {
        val cached = sources.filter { it.isCached }
        return if (cached.isNotEmpty()) {
            resolveBestSource(cached, spatialSupported)
        } else {
            sources.firstOrNull()
        }
    }

    fun backgroundRipSource(spatialSupported: Boolean = false): TrackSource? {
        val overallBest = resolveBestSource(sources, spatialSupported) ?: return null
        if (overallBest.isCached) return null
        val immediate = immediatePlaySource(spatialSupported) ?: return overallBest
        val cmp = sourceComparator(spatialSupported).compare(overallBest, immediate)
        return if (cmp > 0) overallBest else null
    }

    fun toSummaryDto(preferredSource: TrackSource? = null): TrackSummaryDto {
        val s = preferredSource ?: immediatePlaySource() ?: bestSource ?: sources.firstOrNull()
        return TrackSummaryDto(
            id = s?.id ?: (id.toIntOrNull() ?: 0),
            provider = s?.provider?.raw ?: "unknown",
            track_id = s?.providerTrackId ?: id,
            title = title,
            artist = artist,
            album = album,
            duration = durationSeconds,
            codec = s?.codec?.raw ?: "flac",
            bit_depth = s?.bitDepth,
            sample_rate = s?.sampleRate,
            is_cached = s?.isCached ?: isCached,
            artwork_url = artworkUrl
        )
    }

    fun toTrack(preferredSource: TrackSource? = null): Track {
        val s = preferredSource ?: immediatePlaySource() ?: bestSource ?: sources.firstOrNull()
        return Track(
            id = s?.id ?: (id.toIntOrNull() ?: 0),
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            artworkUrl = artworkUrl,
            codec = s?.codec ?: Codec.Alac,
            bitDepth = s?.bitDepth,
            sampleRate = s?.sampleRate,
            bitrateKbps = s?.bitrateKbps,
            provider = s?.provider ?: Provider.Apple,
            providerTrackId = s?.providerTrackId ?: id,
            isCached = s?.isCached ?: isCached
        )
    }
}

@Serializable
data class TrackSummaryDto(
    val id: Int,
    val provider: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val codec: String,
    val bit_depth: Int? = null,
    val sample_rate: Int? = null,
    val is_cached: Boolean = true,
    val artwork_url: String? = null
)

@Serializable
data class UncachedTrackDto(
    val provider: String,
    val item_id: String,
    val track_id: String = item_id,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val is_cached: Boolean = false,
    val artwork_url: String? = null
)

@Serializable
data class TrackDetailDto(
    val id: Int,
    val provider: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val codec: String,
    val bit_depth: Int? = null,
    val sample_rate: Int? = null,
    val genre: String? = null,
    val release_date: String? = null,
    val track_number: Int? = null,
    val track_count: Int? = null,
    val is_cached: Boolean = true,
    val isrc: String? = null,
    val composer: String? = null,
    val disc_number: Int? = null
)

@Serializable
data class SearchResponse(
    val cached: List<TrackSummaryDto> = emptyList(),
    val live: List<UncachedTrackDto> = emptyList()
)

@Serializable
data class AlbumSummaryDto(
    val album: String,
    val artist: String
)

@Serializable
data class PlaybackInfo(
    val stream_url: String,
    val expires_in: Long,
    val mime_type: String,
    val codec: String,
    val duration: Int,
    val bit_depth: Int? = null,
    val sample_rate: Int? = null,
    val file_size: Long
)

@Serializable
data class ExchangeRequest(
    val code: String,
    val device_name: String? = null,
    val platform: String? = null
)

@Serializable
data class UserDto(
    val telegram_id: Long,
    val name: String? = null,
    val username: String? = null,
    val first_name: String? = null,
    val last_name: String? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(first_name, last_name).joinToString(" ").takeIf { it.isNotBlank() }
            ?: username?.let { "@$it" }
            ?: "User #$telegram_id"
}

@Serializable
data class SessionDto(
    val id: String? = null,
    val session_id: String? = null,
    val device_name: String? = null,
    val platform: String? = null,
    val ip: String? = null,
    val ip_address: String? = null,
    val user_agent: String? = null,
    val created_at: String? = null,
    val last_active_at: String? = null,
    val expires_at: String? = null
) {
    val effectiveId: String get() = id ?: session_id ?: "session"
    val effectiveDevice: String
        get() = device_name?.takeIf { it.isNotBlank() } ?: (platform?.replaceFirstChar { it.uppercase() } ?: "Device")
    val effectivePlatform: String get() = platform?.takeIf { it.isNotBlank() } ?: "Native Client"
    val effectiveIp: String? get() = ip ?: ip_address
    val effectiveTimestamp: String? get() = last_active_at ?: created_at
}

@Serializable
data class ExchangeResponse(
    val token_type: String = "Bearer",
    val token: String,
    val access_token: String = token,
    val refresh_token: String = token,
    val expires_in: Long = 259200L,
    val expires_at: String = "",
    val expires_at_unix: Long = 0L,
    val user: UserDto
)

@Serializable
data class RefreshRequest(
    val refresh_token: String
)

@Serializable
data class RefreshResponse(
    val access_token: String,
    val refresh_token: String = access_token,
    val expires_in: Long = 259200L,
    val expires_at: String = "",
    val expires_at_unix: Long = 0L
)

@Serializable
data class MeResponse(
    val user: UserDto,
    val sessions: List<SessionDto> = emptyList()
)

@Serializable
data class ServerHealthDto(
    val status: String,
    val workers_total: Int = 0,
    val workers_available: Int = 0,
    val cache_entries: Long = 0,
    val cache_bytes: Long = 0,
    val uptime_seconds: Long = 0
)

@Serializable
data class FavoriteItemDto(
    val id: Int,
    val track_id: Int,
    val created_at: String? = null,
    val track: TrackSummaryDto? = null
)

@Serializable
data class PlaylistDto(
    val id: Int,
    val name: String,
    val track_count: Long = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreatePlaylistRequest(
    val name: String
)

@Serializable
data class UpdatePlaylistRequest(
    val name: String? = null,
    val track_ids: List<Int>? = null
)

@Serializable
data class RipTaskRequest(
    val provider: String,
    val track_id: String,
    val codec: String? = null
)

@Serializable
data class RipTaskResponse(
    val task_id: String,
    val status: String
)

object StringOrIntSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive && element !is JsonNull) {
                return element.content
            }
            return element.toString()
        }
        return decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class TaskProgressEvent(
    val task_id: String,
    val stage: String = "",
    val percent: Float? = null,
    val speed: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val track_id: String? = null,
    val is_cached: Boolean? = null,
    val completed: Boolean = false,
    val error: String? = null
) {
    val effectiveStage: String get() = stage.ifBlank { "queued" }
    val effectivePercent: Float get() = percent ?: 0f
    val isFinished: Boolean get() = completed || error != null || effectiveStage == "completed" || effectiveStage == "error" || effectiveStage == "failed"
}

@Serializable
enum class RipStage(val displayName: String, val emoji: String) {
    QUEUED("Queued", "⏳"),
    DOWNLOADING("Downloading", "⬇️"),
    DECRYPTING("Decrypting", "🔓"),
    TAGGING("Tagging", "🏷️"),
    UPLOADING("Uploading to Telegram", "☁️"),
    COMPLETED("Ready to Stream", "✅"),
    ERROR("Rip Failed", "❌");

    companion object {
        fun fromStage(stage: String): RipStage = when (stage.lowercase().trim()) {
            "queued" -> QUEUED
            "downloading" -> DOWNLOADING
            "decrypting" -> DECRYPTING
            "tagging" -> TAGGING
            "uploading", "uploading_telegram", "uploading to telegram" -> UPLOADING
            "completed" -> COMPLETED
            "error", "failed" -> ERROR
            else -> QUEUED
        }
    }
}

@Serializable
data class ActiveRipTask(
    val taskId: String,
    val track: TrackSummaryDto,
    val stage: RipStage = RipStage.QUEUED,
    val percent: Float = 0f,
    val speed: String? = null,
    val completed: Boolean = false,
    val error: String? = null,
    val resultingTrackId: String? = null
)

@Serializable
data class LyricsWordDto(
    val text: String,
    val start_ms: Long,
    val end_ms: Long
)

@Serializable
data class LyricsLineDto(
    val text: String,
    val start_ms: Long,
    val end_ms: Long,
    val words: List<LyricsWordDto> = emptyList()
)

@Serializable
data class LyricsResponse(
    val track_id: Int,
    val format: String,
    val plain_text: String? = null,
    val lines: List<LyricsLineDto> = emptyList()
)

fun TrackSummaryDto.toCanonicalTrack(baseUrl: String = ""): CanonicalTrack {
    val providerEnum = Provider.fromString(provider)
    val codecEnum = Codec.fromString(codec)
    val source = TrackSource(
        id = id,
        provider = providerEnum,
        providerTrackId = track_id,
        codec = codecEnum,
        bitDepth = bit_depth,
        sampleRate = sample_rate,
        isCached = is_cached
    )
    val artworkUrl = if (id > 0 && baseUrl.isNotBlank()) {
        "${baseUrl.trimEnd('/')}/api/v1/assets/tracks/$id/artwork"
    } else null

    return CanonicalTrack(
        id = if (id > 0) id.toString() else "${provider}_${track_id}",
        title = title,
        artist = artist,
        album = album,
        durationSeconds = duration,
        artworkUrl = artworkUrl,
        sources = listOf(source)
    )
}

fun UncachedTrackDto.toCanonicalTrack(): CanonicalTrack {
    val providerEnum = Provider.fromString(provider)
    val source = TrackSource(
        id = 0,
        provider = providerEnum,
        providerTrackId = track_id,
        codec = Codec.Alac,
        isCached = false
    )
    return CanonicalTrack(
        id = "${provider}_${track_id}",
        title = title,
        artist = artist,
        album = album,
        durationSeconds = duration,
        sources = listOf(source)
    )
}

fun TrackDetailDto.toCanonicalTrack(baseUrl: String = ""): CanonicalTrack {
    val providerEnum = Provider.fromString(provider)
    val codecEnum = Codec.fromString(codec)
    val source = TrackSource(
        id = id,
        provider = providerEnum,
        providerTrackId = track_id,
        codec = codecEnum,
        bitDepth = bit_depth,
        sampleRate = sample_rate,
        isCached = is_cached
    )
    val artworkUrl = if (id > 0 && baseUrl.isNotBlank()) {
        "${baseUrl.trimEnd('/')}/api/v1/assets/tracks/$id/artwork"
    } else null

    return CanonicalTrack(
        id = id.toString(),
        title = title,
        artist = artist,
        album = album,
        durationSeconds = duration,
        artworkUrl = artworkUrl,
        isrc = isrc,
        sources = listOf(source)
    )
}

object CanonicalDeduplicator {
    fun normalize(str: String): String =
        str.trim().lowercase().replace(Regex("\\s+"), " ")

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

    fun matches(a: DeduplicationItem, b: DeduplicationItem): Boolean {
        // 1) Same provider & providerTrackId
        for (sa in a.sources) {
            for (sb in b.sources) {
                if (sa.provider != Provider.Unknown &&
                    sa.provider == sb.provider &&
                    sa.providerTrackId.isNotBlank() &&
                    sa.providerTrackId == sb.providerTrackId
                ) {
                    return true
                }
            }
        }

        // 2) Same ISRC (when available and non-blank)
        val aIsrc = a.isrc?.trim()
        val bIsrc = b.isrc?.trim()
        if (!aIsrc.isNullOrBlank() && !bIsrc.isNullOrBlank() &&
            aIsrc.equals(bIsrc, ignoreCase = true)
        ) {
            return true
        }

        // 3) Normalized title + normalized artist + duration within 3 seconds (|durA - durB| <= 3)
        val normTitleA = normalize(a.title)
        val normTitleB = normalize(b.title)
        val normArtistA = normalize(a.artist)
        val normArtistB = normalize(b.artist)
        if (normTitleA.isNotEmpty() && normTitleA == normTitleB &&
            normArtistA.isNotEmpty() && normArtistA == normArtistB
        ) {
            if (a.durationSeconds <= 0 || b.durationSeconds <= 0 ||
                kotlin.math.abs(a.durationSeconds - b.durationSeconds) <= 3
            ) {
                return true
            }
        }

        return false
    }

    private class DisjointSet(n: Int) {
        val parent = IntArray(n) { it }
        fun find(i: Int): Int {
            var root = i
            while (root != parent[root]) {
                root = parent[root]
            }
            var curr = i
            while (curr != root) {
                val next = parent[curr]
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(i: Int, j: Int) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootI] = rootJ
            }
        }
    }

    private fun groupItems(items: List<DeduplicationItem>): List<List<DeduplicationItem>> {
        if (items.isEmpty()) return emptyList()
        val n = items.size
        val dsu = DisjointSet(n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                if (matches(items[i], items[j])) {
                    dsu.union(i, j)
                }
            }
        }
        val groups = LinkedHashMap<Int, MutableList<DeduplicationItem>>()
        for (i in 0 until n) {
            val root = dsu.find(i)
            groups.getOrPut(root) { mutableListOf() }.add(items[i])
        }
        return groups.values.toList()
    }

    private fun mergeGroup(group: List<DeduplicationItem>): CanonicalTrack {
        val mergedSources = mutableListOf<TrackSource>()
        for (item in group) {
            for (s in item.sources) {
                val existingIndex = mergedSources.indexOfFirst {
                    it.provider == s.provider && it.providerTrackId == s.providerTrackId
                }
                if (existingIndex >= 0) {
                    val existing = mergedSources[existingIndex]
                    if (s.isCached && !existing.isCached) {
                        mergedSources[existingIndex] = s
                    } else if (s.isCached == existing.isCached) {
                        if (sourceComparator(false).compare(s, existing) > 0) {
                            mergedSources[existingIndex] = s
                        }
                    }
                } else {
                    mergedSources.add(s)
                }
            }
        }

        val sortedSources = mergedSources.sortedWith { a, b ->
            if (a.isCached != b.isCached) {
                if (a.isCached) -1 else 1
            } else {
                sourceComparator(false).compare(b, a)
            }
        }

        val cachedItem = group.firstOrNull { it.sources.any { s -> s.isCached } } ?: group.first()
        val bestItem = group.maxByOrNull { it.durationSeconds } ?: cachedItem

        val id = group.firstNotNullOfOrNull { item ->
            item.id.takeIf { it.isNotBlank() && !it.contains("_") }
        } ?: cachedItem.id

        val artworkUrl = group.firstNotNullOfOrNull { it.artworkUrl?.takeIf { u -> u.isNotBlank() } }
        val isrc = group.firstNotNullOfOrNull { it.isrc?.takeIf { c -> c.isNotBlank() } }
        val title = cachedItem.title.ifBlank { bestItem.title }
        val artist = cachedItem.artist.ifBlank { bestItem.artist }
        val album = group.firstNotNullOfOrNull { it.album.takeIf { a -> a.isNotBlank() } } ?: cachedItem.album
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

@Serializable
data class LastFmTag(val name: String, val count: Int = 0)

@Serializable
data class LastFmArtist(
    val name: String,
    val bioSummary: String? = null,
    val tags: List<LastFmTag> = emptyList(),
    val similarArtists: List<String> = emptyList(),
    val imageUrl: String? = null
)

@Serializable
data class LastFmTrackInfo(
    val title: String,
    val artist: String,
    val wikiSummary: String? = null,
    val tags: List<LastFmTag> = emptyList(),
    val playcount: Long = 0L
)

@Serializable
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

@Serializable
data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val artworkUrl: String? = null,
    val codec: Codec = Codec.Alac,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val bitrateKbps: Int? = null,
    val provider: Provider = Provider.Apple,
    val providerTrackId: String = "",
    val isCached: Boolean = true
) {
    val durationMs: Long
        get() = durationSeconds * 1000L

    val audioSpecs: AudioSpecs
        get() = AudioSpecs(
            codec = codec,
            bitDepth = bitDepth,
            sampleRate = sampleRate,
            bitrateKbps = bitrateKbps
        )

    fun toSummaryDto(): TrackSummaryDto = TrackSummaryDto(
        id = id,
        provider = provider.raw,
        track_id = providerTrackId.ifBlank { id.toString() },
        title = title,
        artist = artist,
        album = album,
        duration = durationSeconds,
        codec = codec.raw,
        bit_depth = bitDepth,
        sample_rate = sampleRate,
        is_cached = isCached,
        artwork_url = artworkUrl
    )

    companion object {
        fun fromSummaryDto(dto: TrackSummaryDto): Track = Track(
            id = dto.id,
            title = dto.title,
            artist = dto.artist,
            album = dto.album,
            durationSeconds = dto.duration,
            artworkUrl = dto.artwork_url,
            codec = Codec.fromString(dto.codec),
            bitDepth = dto.bit_depth,
            sampleRate = dto.sample_rate,
            provider = Provider.fromString(dto.provider),
            providerTrackId = dto.track_id,
            isCached = dto.is_cached
        )
    }
}

fun TrackSummaryDto.toTrack(): Track = Track.fromSummaryDto(this)

@Serializable
data class PlaybackStateSnapshot(
    val queue: List<Track>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)
