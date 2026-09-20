package org.shilpo.peerless

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.shilpo.peerless.model.*
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
            scope = backgroundScope
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

    @Test
    fun testSourceResolutionQualityRanking() {
        val s1 = TrackSource(
            id = 1,
            provider = Provider.Apple,
            providerTrackId = "1",
            codec = Codec.Alac,
            bitDepth = 16,
            sampleRate = 44100
        )
        val s2 = TrackSource(
            id = 2,
            provider = Provider.Apple,
            providerTrackId = "2",
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 48000
        )
        val s3 = TrackSource(
            id = 3,
            provider = Provider.Qobuz,
            providerTrackId = "3",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 96000
        )
        val s4 = TrackSource(
            id = 4,
            provider = Provider.Qobuz,
            providerTrackId = "4",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 192000
        )

        val track = CanonicalTrack(
            id = "test_1",
            title = "Test",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            sources = listOf(s1, s2, s3, s4)
        )

        val best = track.resolveBestSource()
        assertEquals(s4, best)
    }

    @Test
    fun testSpatialAudioResolution() {
        val stereoHiRes = TrackSource(
            id = 1,
            provider = Provider.Qobuz,
            providerTrackId = "1",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 192000
        )
        val atmos = TrackSource(
            id = 2,
            provider = Provider.Apple,
            providerTrackId = "2",
            codec = Codec.Ec3,
            bitDepth = 16,
            sampleRate = 48000
        )

        val track = CanonicalTrack(
            id = "test_spatial",
            title = "Spatial Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 200,
            sources = listOf(stereoHiRes, atmos)
        )

        // When spatialSupported = false, stereoHiRes (192kHz > 48kHz) wins
        assertEquals(stereoHiRes, track.resolveBestSource(spatialSupported = false))

        // When spatialSupported = true, Codec.Ec3 wins regardless of sample rate
        assertEquals(atmos, track.resolveBestSource(spatialSupported = true))
    }

    @Test
    fun testAlacTieBreaker() {
        val flac = TrackSource(
            id = 1,
            provider = Provider.Qobuz,
            providerTrackId = "1",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 48000
        )
        val alac = TrackSource(
            id = 2,
            provider = Provider.Apple,
            providerTrackId = "2",
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 48000
        )

        val track = CanonicalTrack(
            id = "test_tie",
            title = "Tie Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            sources = listOf(flac, alac)
        )

        // Equal sample rate and bit depth -> ALAC preferred over FLAC
        assertEquals(alac, track.resolveBestSource())
    }

    @Test
    fun testInstantPlayAndBackgroundRip() {
        // Cached 24/48 ALAC
        val cachedSource = TrackSource(
            id = 1,
            provider = Provider.Apple,
            providerTrackId = "1",
            codec = Codec.Alac,
            bitDepth = 24,
            sampleRate = 48000,
            isCached = true
        )
        // Uncached 24/192 FLAC (superior)
        val uncachedHiRes = TrackSource(
            id = 2,
            provider = Provider.Qobuz,
            providerTrackId = "2",
            codec = Codec.Flac,
            bitDepth = 24,
            sampleRate = 192000,
            isCached = false
        )

        val track = CanonicalTrack(
            id = "test_rip",
            title = "Rip Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 210,
            sources = listOf(cachedSource, uncachedHiRes)
        )

        // Immediate play source must pick the cached source
        assertEquals(cachedSource, track.immediatePlaySource())

        // Background rip source must return the superior uncached 24/192 source
        assertEquals(uncachedHiRes, track.backgroundRipSource())

        // When spatial is supported and an uncached Atmos exists:
        val uncachedAtmos =
            TrackSource(id = 3, provider = Provider.Apple, providerTrackId = "3", codec = Codec.Ec3, isCached = false)
        val trackWithAtmos = track.copy(sources = listOf(cachedSource, uncachedAtmos))
        assertEquals(uncachedAtmos, trackWithAtmos.backgroundRipSource(spatialSupported = true))
    }

    @Test
    fun testCanonicalDeduplicatorProviderAndTrackId() {
        val cached = listOf(
            TrackSummaryDto(
                id = 10,
                provider = "apple",
                track_id = "trk_100",
                title = "Different Title A",
                artist = "Artist",
                album = "Album",
                duration = 200,
                codec = "alac",
                is_cached = true
            )
        )
        val live = listOf(
            UncachedTrackDto(
                provider = "apple",
                item_id = "trk_100",
                title = "Different Title B",
                artist = "Artist",
                album = "Album",
                duration = 200
            )
        )

        val deduplicated = CanonicalDeduplicator.deduplicate(cached, live)
        assertEquals(1, deduplicated.size)
        assertEquals(1, deduplicated[0].sources.size)
        assertTrue(deduplicated[0].isCached)
    }

    @Test
    fun testCanonicalDeduplicatorIsrc() {
        val t1 = CanonicalTrack(
            id = "1",
            title = "Song A",
            artist = "Artist A",
            album = "Album A",
            durationSeconds = 200,
            isrc = "USUM71703861",
            sources = listOf(
                TrackSource(
                    id = 1,
                    provider = Provider.Apple,
                    providerTrackId = "101",
                    codec = Codec.Alac,
                    isCached = true
                )
            )
        )
        val t2 = CanonicalTrack(
            id = "2",
            title = "Song A (Remaster)",
            artist = "Artist A",
            album = "Album B",
            durationSeconds = 220, // Different duration, but identical ISRC
            isrc = "USUM71703861",
            sources = listOf(
                TrackSource(
                    id = 2,
                    provider = Provider.Qobuz,
                    providerTrackId = "202",
                    codec = Codec.Flac,
                    isCached = false
                )
            )
        )

        val deduplicated = CanonicalDeduplicator.deduplicateCanonical(listOf(t1, t2))
        assertEquals(1, deduplicated.size)
        assertEquals(2, deduplicated[0].sources.size)
    }

    @Test
    fun testCanonicalDeduplicatorTitleArtistDuration() {
        val cached = listOf(
            TrackSummaryDto(
                id = 1,
                provider = "apple",
                track_id = "1",
                title = "  Blinding Lights  ",
                artist = " The Weeknd ",
                album = "After Hours",
                duration = 200,
                codec = "alac",
                is_cached = true
            )
        )
        val live = listOf(
            // Duration within 3 seconds (202 vs 200), same normalized title & artist
            UncachedTrackDto(
                provider = "qobuz",
                item_id = "2",
                title = "blinding lights",
                artist = "the weeknd",
                album = "After Hours",
                duration = 202
            )
        )

        val deduplicated = CanonicalDeduplicator.deduplicate(cached, live)
        assertEquals(1, deduplicated.size)
        assertEquals(2, deduplicated[0].sources.size)
    }

    @Test
    fun testCanonicalDeduplicatorMergesCachedVersionsByIsrc() {
        val tracks = listOf(
            TrackSummaryDto(
                id = 492,
                provider = "apple",
                track_id = "1740701537",
                title = "HEARTBREAK CITY (feat. Mooroo, Talhah Yunus & Jani)",
                artist = "Umair",
                album = "ROCKSTAR WITHOUT A GUITAR",
                duration = 200,
                codec = "alac",
                is_cached = true,
                isrc = "AEA182400060"
            ),
            TrackSummaryDto(
                id = 28744,
                provider = "qobuz",
                track_id = "264126443",
                title = "HEARTBREAK CITY",
                artist = "UMAIR",
                album = "ROCKSTAR WITHOUT A GUITAR",
                duration = 200,
                codec = "flac",
                is_cached = true,
                isrc = "AEA182400060"
            )
        )

        val canonicalTracks = CanonicalDeduplicator.deduplicateTracks(tracks)

        assertEquals(1, canonicalTracks.size)
        assertEquals(
            setOf(Provider.Apple, Provider.Qobuz),
            canonicalTracks.single().sources.map { it.provider }.toSet()
        )
    }
}
