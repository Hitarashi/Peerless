package org.shilpo.peerless

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.Codec
import org.shilpo.peerless.model.Provider
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeAudioEngine : AudioEngine {
    val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    val _events = MutableSharedFlow<AudioEngineEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<AudioEngineEvent> = _events.asSharedFlow()

    val _signalPath = MutableStateFlow<SignalPathSnapshot?>(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = _signalPath.asStateFlow()

    var lastPreparedUrl: String? = null
    var lastPreparedHeaders: Map<String, String> = emptyMap()
    var isPlaying = false
    var isReleased = false

    override fun prepare(
        url: String,
        headers: Map<String, String>,
        title: String?,
        artist: String?,
        artworkUrl: String?
    ) {
        lastPreparedUrl = url
        lastPreparedHeaders = headers
        _state.value = AudioEngineState(status = PlaybackStatus.BUFFERING)
    }

    override fun play() {
        isPlaying = true
        _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
    }

    override fun pause() {
        isPlaying = false
        _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun release() {
        isReleased = true
        isPlaying = false
        _state.value = AudioEngineState(status = PlaybackStatus.IDLE)
    }
}

class SharedCommonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testTrackSummaryDtoSerialization() {
        val jsonString = """
            {
                "id": 42,
                "provider": "apple",
                "track_id": "12345",
                "title": "Starboy",
                "artist": "The Weeknd",
                "album": "Starboy",
                "duration": 230,
                "codec": "alac",
                "bit_depth": 24,
                "sample_rate": 48000,
                "is_cached": true,
                "artwork_url": "https://example.com/art.jpg"
            }
        """.trimIndent()

        val dto = json.decodeFromString<TrackSummaryDto>(jsonString)
        assertEquals(42, dto.id)
        assertEquals("Starboy", dto.title)
        assertEquals("alac", dto.codec)
        assertEquals(24, dto.bit_depth)
        assertEquals(48000, dto.sample_rate)
        assertEquals("apple", dto.provider)
        assertTrue(dto.is_cached)
    }

    @Test
    fun testRealPlayerConnectionQueueLogic() = runTest {
        val fakeEngine = FakeAudioEngine()
        val storage = InMemoryQueueStorage()
        val client = PeerlessApiClient("http://127.0.0.1:4444")
        val player = RealPlayerConnection(
            apiClient = client,
            audioEngine = fakeEngine,
            storage = storage,
            scope = backgroundScope,
            isDevMode = true
        )

        val t1 = Track(
            id = 1,
            title = "Track 1",
            artist = "Artist",
            album = "Album",
            durationSeconds = 200,
            artworkUrl = "https://example.com/art.jpg",
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 48000,
            provider = Provider.Apple,
            providerTrackId = "1",
            isCached = true
        )
        val t2 = t1.copy(id = 2, title = "Track 2", providerTrackId = "2", artworkUrl = "https://example.com/art2.jpg")

        player.play(t1, listOf(t1, t2))
        assertEquals(t1, player.currentTrack.value)
        assertEquals(0, player.currentIndex.value)
        assertTrue(player.canSkipNext.value)
        assertFalse(player.canSkipPrevious.value)

        player.playNext()
        assertEquals(t2, player.currentTrack.value)
        assertEquals(1, player.currentIndex.value)
        assertFalse(player.canSkipNext.value)
        assertTrue(player.canSkipPrevious.value)
    }
}
