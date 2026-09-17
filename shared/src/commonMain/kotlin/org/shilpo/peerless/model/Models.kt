package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

// ============================================================================
// 1. Core Domain Enums & Entities (Single Source of Truth / TMDB Architecture)
// ============================================================================

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

/**
 * Technical audio specification encapsulation used for Poweramp-style badge rendering.
 */
@Serializable
data class AudioSpecs(
    val codec: Codec,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val bitrateKbps: Int? = null
) {
    val isHiRes: Boolean
        get() = (bitDepth ?: 16) > 16 || (sampleRate ?: 44100) > 48000

    /**
     * Compact uppercase badge text matching Poweramp audiophile styling:
     * e.g. "24 BIT  44.1 KHZ  1671 KBPS  ALAC" or "24 BIT  192 KHZ  FLAC"
     */
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
}

/**
 * Concrete provider-specific offering of a track (e.g. Apple ALAC 24/48 or Qobuz FLAC 24/192).
 */
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

/**
 * Canonical musical work entity (TMDB / IMDb style single source of truth),
 * uniquely identified across the ecosystem and aggregating multiple provider sources.
 */
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

    /**
     * Resolves the optimal available source:
     * 1. Cached in Telegram dump channel first.
     * 2. Highest bit-depth (e.g. 24-bit over 16-bit).
     * 3. Highest sample rate (e.g. 192kHz over 96kHz).
     */
    val bestSource: TrackSource?
        get() = sources.maxWithOrNull(
            compareBy<TrackSource> { it.isCached }
                .thenBy { it.bitDepth ?: 16 }
                .thenBy { it.sampleRate ?: 44100 }
        ) ?: sources.firstOrNull()
}

// ============================================================================
// 2. Server DTOs (Directly aligning with peerless-server Rust Axum API)
// ============================================================================

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
    val is_cached: Boolean = true
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
    val is_cached: Boolean = false
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

// Auth DTOs
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
    val sessions: VecOrList<SessionDto> = emptyList()
)

// Type alias for compatibility
typealias VecOrList<T> = List<T>

// Library DTOs
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

// Tasks & SSE Ripping DTOs
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

@Serializable
data class TaskProgressEvent(
    val task_id: String,
    val status: String,
    val progress_percent: Float? = null,
    val message: String? = null,
    val track_id: Int? = null
)

// Lyrics DTOs
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

// ============================================================================
// 3. Domain Mapping Extensions
// ============================================================================

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
