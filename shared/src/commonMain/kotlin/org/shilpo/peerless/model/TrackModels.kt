package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

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

fun sourceComparator(spatialSupported: Boolean = false): Comparator<TrackSource> =
    Comparator { leftSource, rightSource ->
        if (spatialSupported) {
            val leftIsEc3 = leftSource.codec == Codec.Ec3
            val rightIsEc3 = rightSource.codec == Codec.Ec3
            if (leftIsEc3 != rightIsEc3) {
                return@Comparator if (leftIsEc3) 1 else -1
            }
        }
        val leftSampleRate = leftSource.sampleRate ?: 44100
        val rightSampleRate = rightSource.sampleRate ?: 44100
        if (leftSampleRate != rightSampleRate) {
            return@Comparator leftSampleRate.compareTo(rightSampleRate)
        }
        val leftBitDepth = leftSource.bitDepth ?: 16
        val rightBitDepth = rightSource.bitDepth ?: 16
        if (leftBitDepth != rightBitDepth) {
            return@Comparator leftBitDepth.compareTo(rightBitDepth)
        }
        val leftCodecScore = when (leftSource.codec) {
            Codec.Alac -> 2
            Codec.Flac -> 1
            else -> 0
        }
        val rightCodecScore = when (rightSource.codec) {
            Codec.Alac -> 2
            Codec.Flac -> 1
            else -> 0
        }
        if (leftCodecScore != rightCodecScore) {
            return@Comparator leftCodecScore.compareTo(rightCodecScore)
        }
        0
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
