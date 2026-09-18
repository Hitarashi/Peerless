package org.shilpo.peerless.player

import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.PlaybackStateSnapshot

val queueJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

interface QueueStorage {
    suspend fun saveState(snapshot: PlaybackStateSnapshot)
    suspend fun loadState(): PlaybackStateSnapshot?
    suspend fun clearState()
}

class InMemoryQueueStorage(initialSnapshot: PlaybackStateSnapshot? = null) : QueueStorage {
    private var snapshot: PlaybackStateSnapshot? = initialSnapshot

    override suspend fun saveState(snapshot: PlaybackStateSnapshot) {
        this.snapshot = snapshot
    }

    override suspend fun loadState(): PlaybackStateSnapshot? = snapshot

    override suspend fun clearState() {
        snapshot = null
    }
}

expect fun createPlatformQueueStorage(): QueueStorage
