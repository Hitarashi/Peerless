package org.shilpo.peerless.player

class IosQueueStorage : QueueStorage by InMemoryQueueStorage()

actual fun createPlatformQueueStorage(): QueueStorage = IosQueueStorage()

actual fun platformSetup(playerConnection: PlayerConnection) {}
