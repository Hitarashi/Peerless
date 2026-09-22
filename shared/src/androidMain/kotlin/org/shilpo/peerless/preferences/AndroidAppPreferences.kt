package org.shilpo.peerless.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.shilpo.peerless.auth.AndroidContextProvider

class AndroidAppPreferences(
    private val contextProvider: () -> Context? = { AndroidContextProvider.context }
) : AppPreferences {
    private val preferences: SharedPreferences?
        get() = contextProvider()?.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

    private val _lyricsPresentation = MutableStateFlow(
        LyricsPresentation.fromStoredValue(
            preferences?.getString(LYRICS_PRESENTATION_KEY, null)
        )
    )
    override val lyricsPresentation: StateFlow<LyricsPresentation> =
        _lyricsPresentation.asStateFlow()

    override fun setLyricsPresentation(presentation: LyricsPresentation) {
        preferences?.edit()?.putString(LYRICS_PRESENTATION_KEY, presentation.name)?.apply()
        _lyricsPresentation.value = presentation
    }

    private val _liquidGlassEnabled = MutableStateFlow(
        preferences?.getBoolean(LIQUID_GLASS_KEY, true) ?: true
    )
    override val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        preferences?.edit()?.putBoolean(LIQUID_GLASS_KEY, enabled)?.apply()
        _liquidGlassEnabled.value = enabled
    }

    private companion object {
        const val PREFERENCES_FILE = "peerless_app_preferences"
        const val LYRICS_PRESENTATION_KEY = "lyrics_presentation"
        const val LIQUID_GLASS_KEY = "liquid_glass_enabled"
    }
}

actual fun createPlatformAppPreferences(): AppPreferences = AndroidAppPreferences()
