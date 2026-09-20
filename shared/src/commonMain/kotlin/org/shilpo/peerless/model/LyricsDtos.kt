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
    val words: List<LyricsWordDto> = emptyList(),
    val background_words: List<LyricsWordDto> = emptyList(),
    val alignment: String? = null,
    val agent: String? = null,
    val translations: List<LyricsTranslationDto> = emptyList(),
    val romanization: String? = null,
    val is_instrumental: Boolean = false
)

@Serializable
data class LyricsTranslationDto(
    val language: String,
    val text: String
)

@Serializable
data class LyricsResponse(
    val track_id: Int,
    val format: String,
    val plain_text: String? = null,
    val lines: List<LyricsLineDto> = emptyList(),
    val sync_level: String? = null,
    val provider: String? = null,
    val attribution: String? = null,
    val duration_ms: Long = 0L
)
