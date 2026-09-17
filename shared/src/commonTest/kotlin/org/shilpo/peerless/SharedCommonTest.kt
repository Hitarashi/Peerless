package org.shilpo.peerless

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.shilpo.peerless.auth.InMemoryTokenStorage
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.model.*
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.AudioEngine
import org.shilpo.peerless.player.AudioEngineState
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.player.PlaybackStatus
import kotlin.test.*

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
    fun testCanonicalTrackMultiSourceResolution() {
        val appleSource = TrackSource(
            id = 10,
            provider = Provider.Apple,
            providerTrackId = "apple_100",
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 48000,
            isCached = false
        )
        val qobuzSource = TrackSource(
            id = 20,
            provider = Provider.Qobuz,
            providerTrackId = "qobuz_200",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 192000,
            isCached = true
        )

        val track = CanonicalTrack(
            id = "canonical_work_1",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            durationSeconds = 244,
            sources = listOf(appleSource, qobuzSource)
        )

        assertTrue(track.isCached)
        // Best source should resolve to cached Qobuz 24-bit/192kHz stream
        val best = track.bestSource
        assertNotNull(best)
        assertEquals(Provider.Qobuz, best.provider)
        assertEquals(192000, best.sampleRate)
        assertTrue(best.isCached)
    }

    @Test
    fun testPowerampStyleAudioSpecsBadge() {
        val alacSpecs = AudioSpecs(
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 44100,
            bitrateKbps = 1671
        )
        assertEquals("24 BIT  44.1 KHZ  1671 KBPS  ALAC", alacSpecs.badgeText)
        assertTrue(alacSpecs.isHiRes)

        val hiResFlacSpecs = AudioSpecs(
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 192000,
            bitrateKbps = null
        )
        assertEquals("24 BIT  192 KHZ  FLAC", hiResFlacSpecs.badgeText)
        assertTrue(hiResFlacSpecs.isHiRes)

        val standardAac = AudioSpecs(
            codec = Codec.Aac,
            bitDepth = 16,
            sampleRate = 44100,
            bitrateKbps = 256
        )
        assertEquals("16 BIT  44.1 KHZ  256 KBPS  AAC", standardAac.badgeText)
        assertFalse(standardAac.isHiRes)
    }

    @Test
    fun testInMemoryTokenStorageFlow() = runTest {
        val storage = InMemoryTokenStorage()
        assertNull(storage.getToken())
        assertNull(storage.tokenFlow.value)

        val testToken = "256bit_opaque_csprng_random_test_token"
        storage.saveToken(testToken)

        assertEquals(testToken, storage.getToken())
        assertEquals(testToken, storage.tokenFlow.value)

        storage.clearToken()
        assertNull(storage.getToken())
        assertNull(storage.tokenFlow.value)
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
    fun testAlbumSummaryDtoSerialization() {
        val sampleJson = """
            {
                "album": "Random Access Memories",
                "artist": "Daft Punk"
            }
        """.trimIndent()

        val album = json.decodeFromString<AlbumSummaryDto>(sampleJson)
        assertEquals("Random Access Memories", album.album)
        assertEquals("Daft Punk", album.artist)
    }

    @Test
    fun testRipTaskDtoSerialization() {
        val reqJson = """{"task_id":"task_789","status":"queued"}"""
        val resp = json.decodeFromString<RipTaskResponse>(reqJson)
        assertEquals("task_789", resp.task_id)
        assertEquals("queued", resp.status)

        val eventJson = """
            {
                "task_id": "task_789",
                "stage": "tagging",
                "percent": 65.5,
                "track_id": 42
            }
        """.trimIndent()
        val event = json.decodeFromString<TaskProgressEvent>(eventJson)
        assertEquals("tagging", event.stage)
        assertEquals("tagging", event.effectiveStage)
        assertEquals(65.5f, event.percent)
        assertEquals(65.5f, event.effectivePercent)
        assertEquals("42", event.track_id)
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

        // In Dev mode: direct track_id stream URL
        val directUrl = client.resolveStreamUrl(42)
        assertEquals("http://localhost:4444/api/v1/stream?track_id=42", directUrl)

        // In Prod mode with ticket: signed ticket URL
        val ticketUrl = client.resolveStreamUrl(42, "xyz_ticket")
        assertEquals("http://localhost:4444/api/v1/stream?ticket=xyz_ticket", ticketUrl)

        val artworkUrl = client.getArtworkUrl(42, 300)
        assertEquals("http://localhost:4444/api/v1/assets/tracks/42/artwork?size=300", artworkUrl)

        client.baseUrl = "https://music.example.com/"
        assertEquals("https://music.example.com", client.baseUrl)
        assertEquals("https://music.example.com/api/v1/stream?track_id=42", client.resolveStreamUrl(42))
    }

    @Test
    fun testPlaybackCoordinatorStateManagement() {
        val fakeEngine = FakeAudioEngine()
        val client = PeerlessApiClient("http://localhost:4444")
        val coordinator = PlaybackCoordinator(apiClient = client, audioEngine = fakeEngine)
        coordinator.setDevMode(true)

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

    @Test
    fun testSearchFilterLabels() {
        assertEquals("All", SearchFilter.ALL.label)
        assertNull(SearchFilter.ALL.providerQuery)

        assertEquals("Tracks", SearchFilter.TRACKS.label)
        assertNull(SearchFilter.TRACKS.providerQuery)

        assertEquals("Albums", SearchFilter.ALBUMS.label)
        assertNull(SearchFilter.ALBUMS.providerQuery)

        assertEquals("Artists", SearchFilter.ARTISTS.label)
        assertNull(SearchFilter.ARTISTS.providerQuery)

        assertEquals("Apple Music", SearchFilter.APPLE_MUSIC.label)
        assertEquals("apple", SearchFilter.APPLE_MUSIC.providerQuery)

        assertEquals("Qobuz", SearchFilter.QOBUZ.label)
        assertEquals("qobuz", SearchFilter.QOBUZ.providerQuery)

        assertEquals("Cached", SearchFilter.CACHED.label)
        assertNull(SearchFilter.CACHED.providerQuery)
    }

    @Test
    fun testCanonicalDeduplicator() {
        val cached = listOf(
            TrackSummaryDto(
                id = 10,
                provider = "apple",
                track_id = "apple_101",
                title = "Midnight City",
                artist = "M83",
                album = "Hurry Up, We're Dreaming",
                duration = 244,
                codec = "alac",
                bit_depth = 24,
                sample_rate = 48000,
                is_cached = true
            )
        )

        val live = listOf(
            UncachedTrackDto(
                provider = "qobuz",
                item_id = "qobuz_202",
                track_id = "qobuz_202",
                title = "Midnight City",
                artist = "M83",
                album = "Hurry Up, We're Dreaming",
                duration = 244,
                is_cached = false
            ),
            UncachedTrackDto(
                provider = "qobuz",
                item_id = "qobuz_303",
                track_id = "qobuz_303",
                title = "Get Lucky",
                artist = "Daft Punk",
                album = "Random Access Memories",
                duration = 369,
                is_cached = false
            )
        )

        val deduplicated = CanonicalDeduplicator.deduplicate(
            cachedTracks = cached,
            liveTracks = live,
            baseUrl = "http://127.0.0.1:4444"
        )

        assertEquals(2, deduplicated.size)

        // Verify the merged Midnight City track
        val midnightCity = deduplicated.first { it.title == "Midnight City" }
        assertEquals("10", midnightCity.id)
        assertEquals("M83", midnightCity.artist)
        assertEquals("Hurry Up, We're Dreaming", midnightCity.album)
        assertEquals(244, midnightCity.durationSeconds)
        assertEquals("http://127.0.0.1:4444/api/v1/assets/tracks/10/artwork", midnightCity.artworkUrl)
        assertEquals(2, midnightCity.sources.size)

        // Cached Apple source should be the best source
        val bestSource = midnightCity.bestSource
        assertNotNull(bestSource)
        assertEquals(Provider.Apple, bestSource.provider)
        assertTrue(bestSource.isCached)
        assertEquals(24, bestSource.bitDepth)
        assertEquals(48000, bestSource.sampleRate)

        // Qobuz live source should be present in sources
        val qobuzSource = midnightCity.sources.firstOrNull { it.provider == Provider.Qobuz }
        assertNotNull(qobuzSource)
        assertEquals("qobuz_202", qobuzSource.providerTrackId)
        assertFalse(qobuzSource.isCached)

        // Verify Get Lucky track (live-only)
        val getLucky = deduplicated.first { it.title == "Get Lucky" }
        assertEquals("qobuz_qobuz_303", getLucky.id)
        assertEquals("Daft Punk", getLucky.artist)
        assertEquals(1, getLucky.sources.size)
        assertFalse(getLucky.isCached)
    }

    @Test
    fun testLastFmClientFallback() = runTest {
        // Point to an unreachable port to trigger the fallback pathway
        val client = LastFmClient(
            baseUrl = "http://127.0.0.1:59999",
            enableFallback = true
        )

        // Test fallback artist info
        val artistResult = client.getArtistInfo("M83")
        assertTrue(artistResult.isSuccess)
        val artist = artistResult.getOrNull()
        assertNotNull(artist)
        assertEquals("M83", artist.name)
        assertNotNull(artist.bioSummary)
        assertTrue(artist.tags.isNotEmpty())
        assertTrue(artist.tags.any { it.name == "Electronic" })
        assertTrue(artist.similarArtists.isNotEmpty())

        // Test fallback track info
        val trackResult = client.getTrackInfo("M83", "Midnight City")
        assertTrue(trackResult.isSuccess)
        val trackInfo = trackResult.getOrNull()
        assertNotNull(trackInfo)
        assertEquals("Midnight City", trackInfo.title)
        assertEquals("M83", trackInfo.artist)
        assertNotNull(trackInfo.wikiSummary)
        assertTrue(trackInfo.tags.isNotEmpty())
        assertTrue(trackInfo.playcount > 0L)

        // Test fallback top tags
        val tagsResult = client.getTopTags()
        assertTrue(tagsResult.isSuccess)
        val tags = tagsResult.getOrNull()
        assertNotNull(tags)
        assertTrue(tags.size >= 5)
        assertTrue(tags.any { it.name == "Electronic" })
        assertTrue(tags.any { it.name == "Rock" })
    }

    @Test
    fun testPowerampLosslessBadgeWithBitrate() {
        // Spec test: 24 BIT 44.1 KHZ 1671 KBPS ALAC
        val alacSpecs = AudioSpecs(
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 44100
        )
        assertTrue(alacSpecs.isHiRes)
        assertEquals(1671, alacSpecs.effectiveBitrateKbps)
        assertEquals("24 BIT  44.1 KHZ  1671 KBPS  ALAC", alacSpecs.fullBadgeText)

        // Hi-Res Studio Master FLAC test
        val flacSpecs = AudioSpecs(
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 96000
        )
        assertTrue(flacSpecs.isHiRes)
        assertNotNull(flacSpecs.effectiveBitrateKbps)
        assertTrue(flacSpecs.fullBadgeText.contains("24 BIT  96 KHZ"))
        assertTrue(flacSpecs.fullBadgeText.endsWith("FLAC"))
    }

    @Test
    fun testRipStageAndTaskProgressEventServerJson() {
        val serverJson = """
            {
                "task_id": "task_abc123",
                "stage": "uploading",
                "percent": 88.5,
                "speed": "4.2 MB/s",
                "track_id": "cuid_999",
                "is_cached": true,
                "completed": false,
                "error": null
            }
        """.trimIndent()
        val event = json.decodeFromString<TaskProgressEvent>(serverJson)
        assertEquals("task_abc123", event.task_id)
        assertEquals("uploading", event.stage)
        assertEquals("uploading", event.effectiveStage)
        assertEquals(88.5f, event.percent)
        assertEquals(88.5f, event.effectivePercent)
        assertEquals("4.2 MB/s", event.speed)
        assertEquals("cuid_999", event.track_id)
        assertEquals(true, event.is_cached)
        assertFalse(event.completed)
        assertFalse(event.isFinished)

        // Test RipStage mapping
        assertEquals(RipStage.UPLOADING, RipStage.fromStage(event.effectiveStage))
        assertEquals(RipStage.COMPLETED, RipStage.fromStage("completed"))
        assertEquals(RipStage.DOWNLOADING, RipStage.fromStage("downloading"))
        assertEquals(RipStage.ERROR, RipStage.fromStage("failed"))
    }
}