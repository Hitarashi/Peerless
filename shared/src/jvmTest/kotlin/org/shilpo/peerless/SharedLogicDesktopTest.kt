package org.shilpo.peerless

import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.InMemoryQueueStorage
import org.shilpo.peerless.player.MprisServer
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.player.createAudioEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SharedLogicDesktopTest {

    @Test
    fun testDesktopAudioEngineInitialization() {
        val engine = createAudioEngine()
        assertNotNull(engine)
        assertEquals(PlaybackStatus.IDLE, engine.state.value.status)
        engine.release()
    }


    @Test
    fun testMprisServerDbus() = kotlinx.coroutines.test.runTest {
        val fakeEngine = FakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )
        val mpris = MprisServer(player)
        mpris.start()
        Thread.sleep(1000)

        val pb = ProcessBuilder(
            "busctl",
            "--user",
            "introspect",
            "org.mpris.MediaPlayer2.peerless",
            "/org/mpris/MediaPlayer2"
        )
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val output = proc.inputStream.bufferedReader().readText()
        val exitCode = proc.waitFor()
        println("busctl introspect output:\n$output")
        mpris.stop()
        assertEquals(
            0,
            exitCode,
            "busctl introspect failed with exitCode $exitCode. Output:\n$output"
        )
        kotlin.test.assertTrue(
            output.contains("org.mpris.MediaPlayer2"),
            "Output should contain org.mpris.MediaPlayer2: $output"
        )
    }
}

