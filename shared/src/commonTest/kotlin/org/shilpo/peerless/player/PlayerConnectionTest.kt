package org.shilpo.peerless.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.shilpo.peerless.model.Codec
import org.shilpo.peerless.model.Provider
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.network.PeerlessApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class TestFakeAudioEngine : AudioEngine {
    val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    val _events = MutableSharedFlow<AudioEngineEvent>(replay = 1, extraBufferCapacity = 64)
    override val events: SharedFlow<AudioEngineEvent> = _events.asSharedFlow()

    val _signalPath = MutableStateFlow<SignalPathSnapshot?>(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = _signalPath.asStateFlow()

    var lastPreparedUrl: String? = null
    var lastPreparedNextUrl: String? = null
    var isPlaying = false
    var currentVolume = 1.0f

    override fun prepare(
        url: String,
        headers: Map<String, String>,
        title: String?,
        artist: String?,
        artworkUrl: String?
    ) {
        lastPreparedUrl = url
        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)
    }

    override fun prepareNext(url: String?, headers: Map<String, String>) {
        lastPreparedNextUrl = url
    }

    override fun play() {
        isPlaying = true
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
    }

    override fun pause() {
        isPlaying = false
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
    }

    override fun stop() {
        pause()
        _state.value = _state.value.copy(status = PlaybackStatus.IDLE)
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume
    }

    override fun release() {
        isPlaying = false
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
    }
}

class PlayerConnectionTest {

    @Test
    fun remoteSnapshotPlaybackStateDrivesTransportVisuals() {
        assertEquals(PlaybackStatus.PLAYING, remotePlaybackStatus(isPlaying = true))
        assertEquals(PlaybackStatus.PAUSED, remotePlaybackStatus(isPlaying = false))
    }

    private fun createTrack(id: Int, title: String): Track = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationSeconds = 180,
        artworkUrl = "https://example.com/art.jpg",
        codec = Codec.Alac,
        bitDepth = 24,
        sampleRate = 48000,
        provider = Provider.Apple,
        providerTrackId = "track_$id",
        isCached = true
    )

    @Test
    fun testQueueMutations() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        val t1 = createTrack(1, "Track 1")
        val t2 = createTrack(2, "Track 2")
        val t3 = createTrack(3, "Track 3")
        val t4 = createTrack(4, "Track 4")

        player.play(t1, listOf(t1, t2, t3))

        assertEquals(t1, player.currentTrack.value)
        assertEquals(3, player.queue.value.size)
        assertEquals(0, player.currentIndex.value)
        assertTrue(player.canSkipNext.value)
        assertFalse(player.canSkipPrevious.value)

        player.playNextInQueue(t4)
        assertEquals(4, player.queue.value.size)
        assertEquals(t4, player.queue.value[1])

        player.moveInQueue(1, 3)
        assertEquals(t4, player.queue.value[3])
        assertEquals(0, player.currentIndex.value)

        player.removeAt(0)
        assertEquals(t2, player.currentTrack.value)
        assertEquals(0, player.currentIndex.value)
        assertEquals(3, player.queue.value.size)

        player.removeAt(2)
        assertEquals(2, player.queue.value.size)
    }

    @Test
    fun testNonDestructiveShuffle() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        val tracks = (1..10).map { createTrack(it, "Track $it") }
        player.play(tracks[2], tracks)

        assertEquals(2, player.currentIndex.value)
        assertEquals(tracks[2], player.currentTrack.value)

        player.setShuffleMode(true)
        assertTrue(player.shuffleMode.value)
        assertEquals(tracks[2], player.currentTrack.value)
        assertEquals(0, player.currentIndex.value)
        assertEquals(10, player.queue.value.size)

        player.setShuffleMode(false)
        assertFalse(player.shuffleMode.value)
        assertEquals(tracks, player.queue.value)
        assertEquals(2, player.currentIndex.value)
        assertEquals(tracks[2], player.currentTrack.value)
    }

    @Test
    fun testRepeatModesAndSkipFlags() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        val t1 = createTrack(1, "Track 1")
        val t2 = createTrack(2, "Track 2")
        player.play(t2, listOf(t1, t2))

        assertEquals(1, player.currentIndex.value)
        assertFalse(player.canSkipNext.value)

        player.setRepeatMode(RepeatMode.ALL)
        assertTrue(player.canSkipNext.value)

        player.playNext()
        assertEquals(0, player.currentIndex.value)
        assertEquals(t1, player.currentTrack.value)

        player.setRepeatMode(RepeatMode.ONE)
        assertEquals(RepeatMode.ONE, player.repeatMode.value)
    }

    @Test
    fun testVolumeAndSeekControl() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        player.setVolume(0.75f)
        assertEquals(0.75f, player.volume.value)
        assertEquals(0.75f, fakeEngine.currentVolume)

        player.seekTo(45000L)
        assertEquals(45000L, fakeEngine._state.value.positionMs)
    }

    @Test
    fun testGaplessTransitionHandoff() = runTest {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val fakeEngine = TestFakeAudioEngine()
            val storage = InMemoryQueueStorage()
            val client = PeerlessApiClient("http://127.0.0.1:4444")
            val player = RealPlayerConnection(
                apiClient = client,
                audioEngine = fakeEngine,
                storage = storage,
                scope = testScope
            )

            val t1 = createTrack(1, "Track 1")
            val t2 = createTrack(2, "Track 2")
            val t3 = createTrack(3, "Track 3")
            player.play(t1, listOf(t1, t2, t3))

            assertEquals(0, player.currentIndex.value)
            assertEquals(t1, player.currentTrack.value)

            fakeEngine._events.emit(AudioEngineEvent.TransitionedToNext("http://127.0.0.1:4444/api/v1/stream/2"))

            assertEquals(1, player.currentIndex.value)
            assertEquals(t2, player.currentTrack.value)

            fakeEngine._events.emit(AudioEngineEvent.TransitionedToNext("http://127.0.0.1:4444/api/v1/stream/3"))

            assertEquals(2, player.currentIndex.value)
            assertEquals(t3, player.currentTrack.value)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testSignalPathAndDolbyAtmosExposure() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        assertNull(player.signalPath.value)

        val atmosSnapshot = SignalPathSnapshot(
            sourceFormat = "Dolby Atmos (E-AC-3 JOC) 24-bit / 48.0 kHz",
            sampleRateHz = 48000,
            bitDepth = 24,
            channels = 8,
            channelLayout = "7.1.4 (Dolby Atmos)",
            bitRateKbps = 768,
            decoder = "libmpv (ffmpeg/eac3)",
            outputSink = "PipeWire Direct",
            isBitPerfect = true,
            isDolbyAtmos = true
        )

        fakeEngine._signalPath.value = atmosSnapshot
        assertEquals(atmosSnapshot, player.signalPath.value)
        assertTrue(player.signalPath.value?.isDolbyAtmos == true)
        assertEquals(48000, player.signalPath.value?.sampleRateHz)
        assertEquals(24, player.signalPath.value?.bitDepth)
    }

    @Test
    fun testReactivePositionDurationAndIntentionMethods() = runTest {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val fakeEngine = TestFakeAudioEngine()
            val storage = InMemoryQueueStorage()
            val client = PeerlessApiClient("http://127.0.0.1:4444")
            val player = RealPlayerConnection(
                apiClient = client,
                audioEngine = fakeEngine,
                storage = storage,
                scope = testScope
            )

            val t1 = createTrack(1, "Track 1")
            val t2 = createTrack(2, "Track 2")
            player.play(t1, listOf(t1, t2))

            assertEquals(180_000L, player.durationMs.value)
            assertEquals(180_000L, player.currentDurationMs)

            player.pause()
            assertFalse(player.isPlaying.value)
            assertEquals(PlaybackStatus.PAUSED, player.status.value)

            player.play()
            assertTrue(player.isPlaying.value)
            assertEquals(PlaybackStatus.PLAYING, player.status.value)

            player.seekTo(45_000L)
            assertEquals(45_000L, player.positionMs.value)
            assertEquals(45_000L, player.currentPositionMs)

            assertFalse(player.shuffleMode.value)
            player.toggleShuffle()
            assertTrue(player.shuffleMode.value)
            player.toggleShuffle()
            assertFalse(player.shuffleMode.value)

            assertEquals(RepeatMode.OFF, player.repeatMode.value)
            player.cycleRepeatMode()
            assertEquals(RepeatMode.ALL, player.repeatMode.value)
            player.cycleRepeatMode()
            assertEquals(RepeatMode.ONE, player.repeatMode.value)
            player.cycleRepeatMode()
            assertEquals(RepeatMode.OFF, player.repeatMode.value)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testHydrationWithDeferredSeek() = runTest {
        val storage = InMemoryQueueStorage()
        val t1 = createTrack(1, "Track 1")
        storage.saveState(
            org.shilpo.peerless.model.PlaybackStateSnapshot(
                queue = listOf(t1),
                currentIndex = 0,
                positionMs = 32_000L,
                shuffleMode = false,
                repeatMode = RepeatMode.OFF
            )
        )

        val fakeEngine = TestFakeAudioEngine()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        fakeEngine._state.value =
            AudioEngineState(status = PlaybackStatus.PAUSED, durationMs = 180_000L)

        kotlinx.coroutines.delay(100)
        assertEquals(t1, player.currentTrack.value)
        assertEquals(32_000L, player.positionMs.value)
    }

    @Test
    fun testOutputLatencyMsPropagation() = runTest {
        val fakeEngine = TestFakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope
        )

        fakeEngine._state.value = AudioEngineState(
            status = PlaybackStatus.PLAYING,
            positionMs = 5000L,
            durationMs = 100000L,
            outputLatencyMs = -750L
        )

        kotlinx.coroutines.delay(100)
        assertEquals(-750L, player.outputLatencyMs.value)

        fakeEngine._state.value = fakeEngine._state.value.copy(outputLatencyMs = -800L)
        kotlinx.coroutines.delay(100)
        assertEquals(-800L, player.outputLatencyMs.value)
    }
}
