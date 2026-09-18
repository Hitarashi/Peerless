package org.shilpo.peerless.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.PlaybackStateSnapshot
import java.io.File

class DesktopQueueStorage : QueueStorage {
    private val dataDir = File(System.getProperty("user.home") + "/.local/share/peerless").apply { mkdirs() }
    private val queueFile = File(dataDir, "queue_state.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun saveState(snapshot: PlaybackStateSnapshot) = withContext(Dispatchers.IO) {
        try {
            val serialized = json.encodeToString(snapshot)
            queueFile.writeText(serialized)
        } catch (_: Exception) {
        }
    }

    override suspend fun loadState(): PlaybackStateSnapshot? = withContext(Dispatchers.IO) {
        try {
            if (queueFile.exists()) {
                val content = queueFile.readText()
                if (content.isNotBlank()) {
                    json.decodeFromString<PlaybackStateSnapshot>(content)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearState() = withContext(Dispatchers.IO) {
        try {
            if (queueFile.exists()) {
                queueFile.delete()
            }
        } catch (_: Exception) {
        }
    }
}

actual fun createPlatformQueueStorage(): QueueStorage = DesktopQueueStorage()

private var activeMprisServer: MprisServer? = null

actual fun platformSetup(playerConnection: PlayerConnection) {
    if (activeMprisServer == null) {
        activeMprisServer = MprisServer(playerConnection).also { it.start() }
    }
}
