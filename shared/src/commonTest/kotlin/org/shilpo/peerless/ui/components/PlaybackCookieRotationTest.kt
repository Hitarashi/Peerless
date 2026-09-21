package org.shilpo.peerless.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackCookieRotationTest {
    @Test
    fun cookieCompletesOneSmoothRotationPerMinute() {
        assertEquals(60_000, PLAYBACK_COOKIE_ROTATION_DURATION_MILLIS)
        assertEquals(0f, playbackCookieRotationDegrees(0f))
        assertEquals(90f, playbackCookieRotationDegrees(0.25f))
        assertEquals(180f, playbackCookieRotationDegrees(0.5f))
        assertEquals(270f, playbackCookieRotationDegrees(0.75f))
        assertEquals(360f, playbackCookieRotationDegrees(1f))
    }
}
