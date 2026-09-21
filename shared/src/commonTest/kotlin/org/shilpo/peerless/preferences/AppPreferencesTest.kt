package org.shilpo.peerless.preferences

import kotlin.test.Test
import kotlin.test.assertEquals

class AppPreferencesTest {
    @Test
    fun visualPresentationIsTheDefault() {
        val preferences = InMemoryAppPreferences()

        assertEquals(LyricsPresentation.VISUAL, preferences.lyricsPresentation.value)
    }

    @Test
    fun presentationChangesAreObservable() {
        val preferences = InMemoryAppPreferences()

        preferences.setLyricsPresentation(LyricsPresentation.READABLE)

        assertEquals(LyricsPresentation.READABLE, preferences.lyricsPresentation.value)
    }

    @Test
    fun invalidPersistedValuesFallBackToVisual() {
        assertEquals(LyricsPresentation.VISUAL, LyricsPresentation.fromStoredValue("unknown"))
    }
}
