package org.shilpo.peerless.sync

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackSyncWireFormatTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun every_client_message_has_server_discriminator_and_payload() {
        val messages = listOf(
            ClientSyncMessage.Hello(
                HelloPayload(
                    device_id = "device_test",
                    device_name = "Desktop",
                    platform = "JVM"
                )
            ) to "hello",
            ClientSyncMessage.ReportState(
                ReportStatePayload(buildJsonObject { put("playing", true) })
            ) to "report_state",
            ClientSyncMessage.Command(CommandPayload(action = "play", data = JsonNull)) to "command",
            ClientSyncMessage.TransferPlayback(TransferPlaybackPayload("device_other")) to "transfer_playback"
        )

        messages.forEach { (message, expectedType) ->
            val frame = json.parseToJsonElement(json.encodeClientSyncMessage(message)).jsonObject

            assertEquals(expectedType, frame["type"]?.jsonPrimitive?.content)
            assertTrue(frame["payload"] is JsonObject)
        }
    }
}
