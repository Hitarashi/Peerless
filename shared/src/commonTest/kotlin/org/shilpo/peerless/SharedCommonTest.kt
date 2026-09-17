package org.shilpo.peerless

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.ExchangeResponse
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.SearchResponse
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.AudioEngine
import org.shilpo.peerless.player.AudioEngineState
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.player.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeAudioEngine : AudioEngine {
    val _state = MutableStateFlow(AudioEngineState())
    override val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    var lastPreparedUrl: String? = null
    var lastPreparedHeaders: Map<String, String> = emptyMap()
    var isPlaying = false
    var isReleased = false

    override fun prepare(url: String, headers: Map<String, String>) {
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
        val sampleJson = """
            {
                "id": 42,
                "provider": "apple",
                "track_id": "1440857781",
                "title": "Blank Space",
                "artist": "Taylor Swift",
                "album": "1989",
                "duration": 231,
                "codec": "alac",
                "bit_depth": 24,
                "sample_rate": 44100,
                "is_cached": true
            }
        """.trimIndent()

        val track = json.decodeFromString<TrackSummaryDto>(sampleJson)
        assertEquals(42, track.id)
        assertEquals("Blank Space", track.title)
        assertEquals(24, track.bit_depth)
        assertEquals(44100, track.sample_rate)
        assertTrue(track.is_cached)
    }

    @Test
    fun testSearchResponseSerialization() {
        val sampleJson = """
            {
                "cached": [
                    {
                        "id": 1,
                        "provider": "apple",
                        "track_id": "100",
                        "title": "Song A",
                        "artist": "Artist A",
                        "album": "Album A",
                        "duration": 180,
                        "codec": "alac"
                    }
                ],
                "live": [
                    {
                        "provider": "qobuz",
                        "item_id": "item_2",
                        "track_id": "200",
                        "title": "Song B",
                        "artist": "Artist B",
                        "album": "Album B",
                        "duration": 200
                    }
                ]
            }
        """.trimIndent()

        val resp = json.decodeFromString<SearchResponse>(sampleJson)
        assertEquals(1, resp.cached.size)
        assertEquals(1, resp.live.size)
        assertEquals("Song A", resp.cached.first().title)
        assertEquals("Song B", resp.live.first().title)
    }

    @Test
    fun testPlaybackInfoSerialization() {
        val sampleJson = """
            {
                "stream_url": "/api/v1/stream?ticket=abc",
                "expires_in": 7200,
                "mime_type": "audio/mp4",
                "codec": "alac",
                "duration": 231,
                "bit_depth": 24,
                "sample_rate": 44100,
                "file_size": 48920110
            }
        """.trimIndent()

        val info = json.decodeFromString<PlaybackInfo>(sampleJson)
        assertEquals("/api/v1/stream?ticket=abc", info.stream_url)
        assertEquals(7200L, info.expires_in)
        assertEquals(48920110L, info.file_size)
    }

    @Test
    fun testExchangeResponseSerialization() {
        val sampleJson = """
            {
                "token_type": "Bearer",
                "token": "tok123",
                "access_token": "acc123",
                "refresh_token": "ref123",
                "expires_in": 259200,
                "expires_at": "2026-09-20T12:00:00Z",
                "expires_at_unix": 1758369600,
                "user": {
                    "telegram_id": 123456789,
                    "name": "Sayeed"
                }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ExchangeResponse>(sampleJson)
        assertEquals("Bearer", resp.token_type)
        assertEquals("acc123", resp.access_token)
        assertEquals(123456789L, resp.user.telegram_id)
        assertEquals("Sayeed", resp.user.name)
    }

    @Test
    fun testPeerlessApiClientUrlHelpers() {
        val client = PeerlessApiClient("http://localhost:4444")
        assertEquals("http://localhost:4444", client.baseUrl)

        val directUrl = client.getStreamUrl(42)
        assertEquals("http://localhost:4444/api/v1/stream?track_id=42", directUrl)

        val ticketUrl = client.getStreamUrl(42, "xyz_ticket")
        assertEquals("http://localhost:4444/api/v1/stream?ticket=xyz_ticket", ticketUrl)

        val artworkUrl = client.getArtworkUrl(42, 300)
        assertEquals("http://localhost:4444/api/v1/assets/tracks/42/artwork?size=300", artworkUrl)

        client.baseUrl = "https://music.example.com/"
        assertEquals("https://music.example.com", client.baseUrl)
        assertEquals("https://music.example.com/api/v1/stream?track_id=42", client.getStreamUrl(42))
    }

    @Test
    fun testPlaybackCoordinatorStateManagement() {
        val fakeEngine = FakeAudioEngine()
        val client = PeerlessApiClient("http://localhost:4444")
        val coordinator = PlaybackCoordinator(apiClient = client, audioEngine = fakeEngine)

        val track1 = TrackSummaryDto(
            id = 1,
            provider = "apple",
            track_id = "t1",
            title = "Track 1",
            artist = "Artist",
            album = "Album",
            duration = 200,
            codec = "alac"
        )
        val track2 = TrackSummaryDto(
            id = 2,
            provider = "apple",
            track_id = "t2",
            title = "Track 2",
            artist = "Artist",
            album = "Album",
            duration = 180,
            codec = "alac"
        )

        val queue = listOf(track1, track2)
        coordinator.playTrack(track1, queue)

        assertEquals(track1, coordinator.currentTrack)
        assertEquals(2, coordinator.queue.size)
        assertEquals(0, coordinator.currentIndex)
        assertEquals(200_000L, coordinator.state.value.durationMs)

        // Test seekTo
        coordinator.seekTo(15000L)
        assertEquals(15000L, coordinator.state.value.positionMs)

        // Test togglePlayPause
        fakeEngine._state.value = fakeEngine._state.value.copy(status = PlaybackStatus.PLAYING)
        coordinator.togglePlayPause()
        assertEquals(PlaybackStatus.PAUSED, fakeEngine.state.value.status)

        // Test playNext
        coordinator.playNext()
        assertEquals(track2, coordinator.currentTrack)
        assertEquals(1, coordinator.currentIndex)

        // Test playPrevious
        fakeEngine._state.value = fakeEngine._state.value.copy(positionMs = 1000L)
        coordinator.playPrevious()
        assertEquals(track1, coordinator.currentTrack)
        assertEquals(0, coordinator.currentIndex)
    }
}