package org.shilpo.peerless.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosAppPreferences : AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _lyricsPresentation = MutableStateFlow(
        LyricsPresentation.fromStoredValue(defaults.stringForKey(LYRICS_PRESENTATION_KEY))
    )
    override val lyricsPresentation: StateFlow<LyricsPresentation> =
        _lyricsPresentation.asStateFlow()

    override fun setLyricsPresentation(presentation: LyricsPresentation) {
        defaults.setObject(presentation.name, forKey = LYRICS_PRESENTATION_KEY)
        _lyricsPresentation.value = presentation
    }

    private companion object {
        const val LYRICS_PRESENTATION_KEY = "peerless_lyrics_presentation"
    }
}

actual fun createPlatformAppPreferences(): AppPreferences = IosAppPreferences()
