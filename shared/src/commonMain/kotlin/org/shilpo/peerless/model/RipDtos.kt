package org.shilpo.peerless.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class RipTaskRequest(
    val provider: String,
    val track_id: String,
    val codec: String? = null
)

@Serializable
data class RipTaskResponse(
    val task_id: String,
    val status: String
)

object StringOrIntSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive && element !is JsonNull) {
                return element.content
            }
            return element.toString()
        }
        return decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class TaskProgressEvent(
    val task_id: String,
    val stage: String = "",
    val percent: Float? = null,
    val speed: String? = null,
    @Serializable(with = StringOrIntSerializer::class)
    val track_id: String? = null,
    val is_cached: Boolean? = null,
    val completed: Boolean = false,
    val error: String? = null
) {
    val effectiveStage: String get() = stage.ifBlank { "queued" }
    val effectivePercent: Float get() = percent ?: 0f
    val isFinished: Boolean get() = completed || error != null || effectiveStage == "completed" || effectiveStage == "error" || effectiveStage == "failed" || effectiveStage == "cancelled"
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
        get() = (completed || stage == RipStage.COMPLETED) && stage != RipStage.ERROR && error == null

    val isError: Boolean
        get() = stage == RipStage.ERROR || error != null

    val provider: String get() = track.provider
    val trackId: String get() = track.track_id
}
