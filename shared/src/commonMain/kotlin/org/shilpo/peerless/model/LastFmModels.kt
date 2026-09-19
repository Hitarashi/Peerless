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
