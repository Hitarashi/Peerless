package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

@Serializable
data class RipTaskRequest(
    val provider: String,
    val track_id: String,
    val codec: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: Int? = null
)

@Serializable
data class RipTaskResponse(
    val task_id: String,
    val status: String
)

@Serializable
data class RipTaskSnapshotDto(
    val task_id: String,
    val provider: String,
    val source_track_id: String,
    val codec: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: Int? = null,
    val stage: String,
    val percent: Float? = null,
    val speed: String? = null,
    val result_track_id: Int? = null,
    val is_cached: Boolean? = null,
    val completed: Boolean = false,
    val error: String? = null,
    val is_owner: Boolean = false
) {
    val isTerminal: Boolean
        get() = completed || error != null || stage.equals("completed", ignoreCase = true) ||
                stage.equals("failed", ignoreCase = true) || stage.equals(
            "error",
            ignoreCase = true
        ) ||
                stage.equals("cancelled", ignoreCase = true)
}

@Serializable
enum class RipStage(val displayName: String, val emoji: String = "") {
    QUEUED("Queued", ""),
    DOWNLOADING("Downloading", ""),
    DECRYPTING("Decrypting", ""),
    TAGGING("Tagging", ""),
    UPLOADING("Uploading to Telegram", ""),
    COMPLETED("Ready to Stream", ""),
    CANCELLED("Cancelled", ""),
    ERROR("Rip Failed", "");

    companion object {
        fun fromStage(stage: String): RipStage = when (stage.lowercase().trim()) {
            "queued" -> QUEUED
            "downloading" -> DOWNLOADING
            "decrypting" -> DECRYPTING
            "tagging" -> TAGGING
            "uploading", "uploading_telegram", "uploading to telegram" -> UPLOADING
            "completed" -> COMPLETED
            "cancelled" -> CANCELLED
            "error", "failed" -> ERROR
            else -> QUEUED
        }
    }
}

@Serializable
data class ActiveRipTask(
    val taskId: String,
    val track: TrackSummaryDto,
    val stage: RipStage = RipStage.QUEUED,
    val percent: Float = 0f,
    val speed: String? = null,
    val completed: Boolean = false,
    val error: String? = null,
    val resultingTrackId: String? = null,
    val isOwner: Boolean = true,
    val isAutoPlayPending: Boolean = false
) {
    val isFinished: Boolean
        get() = completed || stage == RipStage.COMPLETED || stage == RipStage.ERROR || stage == RipStage.CANCELLED

    val isCompleted: Boolean
        get() = stage == RipStage.COMPLETED

    val isError: Boolean
        get() = stage == RipStage.ERROR

    val provider: String get() = track.provider
    val trackId: String get() = track.track_id
}
