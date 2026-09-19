package org.shilpo.peerless.sync

import org.shilpo.peerless.auth.InMemoryTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSyncDeviceIdentityTest {
    @Test
    fun managersFromTheSameInstallationReuseTheirDeviceId() {
        val storage = InMemoryTokenStorage()

        val first = PlaybackSyncManager(storage)
        val second = PlaybackSyncManager(storage)

        assertEquals(first.selfDeviceId, second.selfDeviceId)
    }
}
