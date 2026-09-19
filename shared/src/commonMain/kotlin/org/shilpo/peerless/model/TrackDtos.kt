package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

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
