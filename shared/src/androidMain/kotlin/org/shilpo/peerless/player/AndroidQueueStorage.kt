package org.shilpo.peerless.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.shilpo.peerless.auth.AndroidContextProvider
import org.shilpo.peerless.model.PlaybackStateSnapshot
import java.io.File

class AndroidQueueStorage(
    private val contextProvider: () -> Context? = { AndroidContextProvider.context }
) : QueueStorage {
    private val queueFile: File?
        get() {
            val ctx = contextProvider() ?: return null
            return File(ctx.filesDir, "playback_state.json")
        }

    override suspend fun saveState(snapshot: PlaybackStateSnapshot) = withContext(Dispatchers.IO) {
        try {
            val file = queueFile ?: return@withContext
            val json = queueJson.encodeToString(PlaybackStateSnapshot.serializer(), snapshot)
            file.writeText(json)
        } catch (_: Exception) {
        }
    }

    override suspend fun loadState(): PlaybackStateSnapshot? = withContext(Dispatchers.IO) {
        try {
            val file = queueFile ?: return@withContext null
            if (file.exists()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) {
                    queueJson.decodeFromString(PlaybackStateSnapshot.serializer(), content)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearState() = withContext(Dispatchers.IO) {
        try {
            val file = queueFile ?: return@withContext
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}

actual fun createPlatformQueueStorage(): QueueStorage = AndroidQueueStorage()

actual fun platformSetup(playerConnection: PlayerConnection) {}
