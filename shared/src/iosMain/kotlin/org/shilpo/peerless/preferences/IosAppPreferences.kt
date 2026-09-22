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

    private val _liquidGlassEnabled = MutableStateFlow(
        if (defaults.objectForKey(LIQUID_GLASS_KEY) == null) true
        else defaults.boolForKey(LIQUID_GLASS_KEY)
    )
    override val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = LIQUID_GLASS_KEY)
        _liquidGlassEnabled.value = enabled
    }

    private companion object {
        const val LYRICS_PRESENTATION_KEY = "peerless_lyrics_presentation"
        const val LIQUID_GLASS_KEY = "peerless_liquid_glass_enabled"
    }
}

actual fun createPlatformAppPreferences(): AppPreferences = IosAppPreferences()
