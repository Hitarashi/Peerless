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
    Unknown("unknown", "Unknown");

    companion object {
        fun fromString(value: String): Codec = when (value.lowercase()) {
            "alac" -> Alac
            "flac" -> Flac
            "aac" -> Aac
            "opus" -> Opus
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

@Serializable
data class AudioSpecs(
    val codec: Codec,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val bitrateKbps: Int? = null
) {
    val isHiRes: Boolean
        get() = (bitDepth ?: 16) > 16 || (sampleRate ?: 44100) > 48000

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
                Codec.Unknown -> (pcmBitrate * 0.70).toInt()
            }
        }

    val badgeText: String
        get() = buildString {
            if (bitDepth != null) append("${bitDepth} BIT  ")
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
        get() = buildString {
            if (bitDepth != null) append("${bitDepth} BIT  ")
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
        get() = sources.maxWithOrNull(
            compareBy<TrackSource> { it.isCached }
                .thenBy { it.bitDepth ?: 16 }
                .thenBy { it.sampleRate ?: 44100 }
        ) ?: sources.firstOrNull()
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
    val name: String? = null
)

@Serializable
data class SessionDto(
    val session_id: String,
    val device_name: String? = null,
    val platform: String? = null,
    val created_at: String? = null,
    val expires_at: String? = null
)

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
    private fun normalize(str: String): String =
        str.trim().lowercase().replace(Regex("\\s+"), " ")

    private data class TrackMetadata(
        var id: String,
        val title: String,
        val artist: String,
        var album: String,
        var durationSeconds: Int,
        var artworkUrl: String? = null,
        var isrc: String? = null
    )

    fun deduplicate(
        cachedTracks: List<TrackSummaryDto>,
        liveTracks: List<UncachedTrackDto>,
        baseUrl: String = ""
    ): List<CanonicalTrack> {
        val sourcesMap = LinkedHashMap<Pair<String, String>, MutableList<TrackSource>>()
        val metadataMap = LinkedHashMap<Pair<String, String>, TrackMetadata>()

        for (cached in cachedTracks) {
            val key = normalize(cached.title) to normalize(cached.artist)
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

            val existingSources = sourcesMap.getOrPut(key) { mutableListOf() }
            if (existingSources.none { it.provider == source.provider && it.providerTrackId == source.providerTrackId }) {
                existingSources.add(source)
            }

            if (!metadataMap.containsKey(key)) {
                val artworkUrl = if (cached.id > 0 && baseUrl.isNotBlank()) {
                    "${baseUrl.trimEnd('/')}/api/v1/assets/tracks/${cached.id}/artwork"
                } else null

                metadataMap[key] = TrackMetadata(
                    id = if (cached.id > 0) cached.id.toString() else "${cached.provider}_${cached.track_id}",
                    title = cached.title,
                    artist = cached.artist,
                    album = cached.album,
                    durationSeconds = cached.duration,
                    artworkUrl = artworkUrl
                )
            } else {
                val meta = metadataMap[key]!!
                if (cached.id > 0 && meta.id.contains("_")) {
                    meta.id = cached.id.toString()
                    if (baseUrl.isNotBlank()) {
                        meta.artworkUrl = "${baseUrl.trimEnd('/')}/api/v1/assets/tracks/${cached.id}/artwork"
                    }
                }
                if (meta.album.isBlank() && cached.album.isNotBlank()) {
                    meta.album = cached.album
                }
                if (meta.durationSeconds <= 0 && cached.duration > 0) {
                    meta.durationSeconds = cached.duration
                }
            }
        }

        for (live in liveTracks) {
            val key = normalize(live.title) to normalize(live.artist)
            val providerEnum = Provider.fromString(live.provider)
            val codecEnum = if (providerEnum == Provider.Qobuz) Codec.Flac else Codec.Alac
            val source = TrackSource(
                id = 0,
                provider = providerEnum,
                providerTrackId = live.track_id,
                codec = codecEnum,
                isCached = false
            )

            val existingSources = sourcesMap.getOrPut(key) { mutableListOf() }
            if (existingSources.none { it.provider == source.provider && it.providerTrackId == source.providerTrackId }) {
                existingSources.add(source)
            }

            if (!metadataMap.containsKey(key)) {
                metadataMap[key] = TrackMetadata(
                    id = "${live.provider}_${live.track_id}",
                    title = live.title,
                    artist = live.artist,
                    album = live.album,
                    durationSeconds = live.duration
                )
            } else {
                val meta = metadataMap[key]!!
                if (meta.album.isBlank() && live.album.isNotBlank()) {
                    meta.album = live.album
                }
                if (meta.durationSeconds <= 0 && live.duration > 0) {
                    meta.durationSeconds = live.duration
                }
            }
        }

        return metadataMap.map { (key, meta) ->
            val sources = sourcesMap[key] ?: emptyList()
            val sortedSources = sources.sortedWith(
                compareByDescending<TrackSource> { it.isCached }
                    .thenByDescending { it.bitDepth ?: 16 }
                    .thenByDescending { it.sampleRate ?: 44100 }
            )

            CanonicalTrack(
                id = meta.id,
                title = meta.title,
                artist = meta.artist,
                album = meta.album,
                durationSeconds = meta.durationSeconds,
                artworkUrl = meta.artworkUrl,
                isrc = meta.isrc,
                sources = sortedSources
            )
        }
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
