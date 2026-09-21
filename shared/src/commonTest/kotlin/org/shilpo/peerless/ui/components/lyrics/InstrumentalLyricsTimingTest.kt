package org.shilpo.peerless.ui.components.lyrics

import org.shilpo.peerless.model.LyricsLineDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstrumentalLyricsTimingTest {
    @Test
    fun `instrumental dots stay hidden before their interval`() {
        assertFalse(
            isInstrumentalLineVisible(
                line = instrumentalLine(startMs = 1_000L, endMs = 3_000L),
                fallbackEndMs = null,
                positionMs = 999L
            )
        )
    }

    @Test
    fun `instrumental dots appear at interval start and disappear at end`() {
        val line = instrumentalLine(startMs = 1_000L, endMs = 3_000L)

        assertTrue(isInstrumentalLineVisible(line, fallbackEndMs = null, positionMs = 1_000L))
        assertTrue(isInstrumentalLineVisible(line, fallbackEndMs = null, positionMs = 2_999L))
        assertFalse(isInstrumentalLineVisible(line, fallbackEndMs = null, positionMs = 3_000L))
    }

    @Test
    fun `instrumental dots use next lyric as missing end`() {
        val line = instrumentalLine(startMs = 1_000L, endMs = 1_000L)

        assertTrue(isInstrumentalLineVisible(line, fallbackEndMs = 2_500L, positionMs = 2_000L))
        assertFalse(isInstrumentalLineVisible(line, fallbackEndMs = 2_500L, positionMs = 2_500L))
        assertFalse(isInstrumentalLineVisible(line, fallbackEndMs = null, positionMs = 2_000L))
    }

    @Test
    fun `non instrumental lines never show dots`() {
        val line = LyricsLineDto(text = "Hello", start_ms = 1_000L, end_ms = 3_000L)

        assertFalse(isInstrumentalLineVisible(line, fallbackEndMs = null, positionMs = 2_000L))
    }

    private fun instrumentalLine(startMs: Long, endMs: Long) = LyricsLineDto(
        text = "",
        start_ms = startMs,
        end_ms = endMs,
        is_instrumental = true
    )
}
