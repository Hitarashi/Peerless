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
    val is_cached: Boolean = true
)

@Serializable
data class UncachedTrackDto(
    val provider: String,
    val item_id: String,
    val track_id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val is_cached: Boolean = false
)

@Serializable
data class SearchResponse(
    val cached: List<TrackSummaryDto> = emptyList(),
    val live: List<UncachedTrackDto> = emptyList()
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
data class ExchangeResponse(
    val token_type: String,
    val token: String,
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val expires_at: String,
    val expires_at_unix: Long,
    val user: UserDto
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
