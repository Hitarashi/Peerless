package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

@Serializable
data class LastFmTag(val name: String, val count: Int = 0)

@Serializable
data class LastFmArtist(
    val name: String,
    val bioSummary: String? = null,
    val tags: List<LastFmTag> = emptyList(),
    val similarArtists: List<String> = emptyList(),
    val url: String? = null
)

@Serializable
internal data class LastFmAlbumInfo(
    val name: String,
    val artist: String,
    val wikiSummary: String? = null,
    val releaseDate: String? = null,
    val tags: List<LastFmTag> = emptyList(),
    val listeners: Long = 0L,
    val playcount: Long = 0L,
    val url: String? = null
)

@Serializable
data class LastFmTrackInfo(
    val title: String,
    val artist: String,
    val wikiSummary: String? = null,
    val tags: List<LastFmTag> = emptyList(),
    val listeners: Long = 0L,
    val playcount: Long = 0L
)

@Serializable
data class LastFmUserTrack(
    val title: String,
    val artist: String,
    val playCount: Long = 0L,
    val timestampEpochSeconds: Long? = null
)

@Serializable
data class LastFmSimilarTrack(
    val title: String,
    val artist: String,
    val match: Double = 0.0,
    val mbid: String? = null
)
