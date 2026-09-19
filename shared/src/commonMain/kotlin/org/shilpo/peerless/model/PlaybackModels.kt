package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

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
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isPlaying: Boolean = false
)
