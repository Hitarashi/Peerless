package org.shilpo.peerless.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.PlaybackStateSnapshot
import platform.Foundation.NSUserDefaults

class IosQueueStorage : QueueStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val key = "peerless_queue_state"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun saveState(snapshot: PlaybackStateSnapshot) = withContext(Dispatchers.Default) {
        try {
            val serialized = json.encodeToString(snapshot)
            userDefaults.setObject(serialized, forKey = key)
        } catch (_: Exception) {
        }
    }

    override suspend fun loadState(): PlaybackStateSnapshot? = withContext(Dispatchers.Default) {
        try {
            val content = userDefaults.stringForKey(key)
            if (!content.isNullOrBlank()) {
                json.decodeFromString<PlaybackStateSnapshot>(content)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun clearState() = withContext(Dispatchers.Default) {
        try {
            userDefaults.removeObjectForKey(key)
        } catch (_: Exception) {
        }
    }
}

actual fun createPlatformQueueStorage(): QueueStorage = IosQueueStorage()

actual fun platformSetup(playerConnection: PlayerConnection) {}
