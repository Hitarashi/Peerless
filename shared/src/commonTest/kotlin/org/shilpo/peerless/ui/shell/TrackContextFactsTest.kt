package org.shilpo.peerless.ui.shell

import org.shilpo.peerless.model.LastFmTrackInfo
import org.shilpo.peerless.model.TrackDetailDto
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackContextFactsTest {
    @Test
    fun `lastfm links are https and reject foreign hosts`() {
        assertEquals(
            "https://www.last.fm/music/Daft+Punk/Discovery",
            lastFmExternalUrl("http://www.last.fm/music/Daft+Punk/Discovery")
        )
        assertEquals(
            "https://last.fm/music/Daft+Punk",
            lastFmExternalUrl("https://last.fm/music/Daft+Punk")
        )
        assertEquals(null, lastFmExternalUrl("https://last.fm.evil.example/artist"))
        assertEquals(null, lastFmExternalUrl("javascript:alert(1)"))
    }

    @Test
    fun `track context includes available metadata`() {
        val facts = trackContextFacts(
            TrackDetailDto(
                id = 42,
                provider = "apple_music",
                track_id = "song-42",
                title = "Example",
                artist = "Artist",
                album = "Album",
                duration = 210,
                codec = "alac",
                genre = "Alternative",
                release_date = "2021-03-12",
                track_number = 4,
                track_count = 12,
                composer = "Composer",
                disc_number = 2
            )
        )

        assertEquals(
            listOf(
                TrackContextFact("Genre", "Alternative"),
                TrackContextFact("Track released", "2021"),
                TrackContextFact("Composer", "Composer"),
                TrackContextFact("Album position", "Track 4 of 12 · Disc 2")
            ),
            facts
        )
    }

    @Test
    fun `missing track details produce no facts`() {
        assertEquals(emptyList(), trackContextFacts(null))
    }

    @Test
    fun `track context includes available Last fm stats`() {
        val facts = trackContextFacts(
            details = null,
            lastFmInfo = LastFmTrackInfo(
                title = "Example",
                artist = "Artist",
                listeners = 124,
                playcount = 1_409
            )
        )

        assertEquals(
            listOf(
                TrackContextFact("Last.fm listeners", "124"),
                TrackContextFact("Last.fm plays", "1,409")
            ),
            facts
        )
    }

    @Test
    fun `track context omits unavailable Last fm stats`() {
        assertEquals(
            emptyList(),
            trackContextFacts(
                details = null,
                lastFmInfo = LastFmTrackInfo(title = "Example", artist = "Artist")
            )
        )
    }

    @Test
    fun `blank and unknown metadata are omitted`() {
        val facts = trackContextFacts(
            TrackDetailDto(
                id = 42,
                provider = "qobuz",
                track_id = "song-42",
                title = "Example",
                artist = "Artist",
                album = "Album",
                duration = 210,
                codec = "flac",
                genre = " ",
                release_date = "",
                track_number = 0,
                track_count = 0,
                composer = null,
                disc_number = 0
            )
        )

        assertEquals(emptyList(), facts)
    }
}
