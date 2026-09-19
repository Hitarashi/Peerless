package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

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
