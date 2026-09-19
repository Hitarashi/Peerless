package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

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
