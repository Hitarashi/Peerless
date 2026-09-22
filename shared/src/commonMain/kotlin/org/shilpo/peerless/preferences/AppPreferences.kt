package org.shilpo.peerless.preferences

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LyricsPresentation {
    VISUAL,
    READABLE;

    companion object {
        fun fromStoredValue(value: String?): LyricsPresentation =
            entries.firstOrNull { it.name == value } ?: VISUAL
    }
}

interface AppPreferences {
    val lyricsPresentation: StateFlow<LyricsPresentation>
    fun setLyricsPresentation(presentation: LyricsPresentation)

    val liquidGlassEnabled: StateFlow<Boolean>
    fun setLiquidGlassEnabled(enabled: Boolean)
}

class InMemoryAppPreferences(
    initialLyricsPresentation: LyricsPresentation = LyricsPresentation.VISUAL,
    initialLiquidGlassEnabled: Boolean = true
) : AppPreferences {
    private val _lyricsPresentation = MutableStateFlow(initialLyricsPresentation)
    override val lyricsPresentation: StateFlow<LyricsPresentation> =
        _lyricsPresentation.asStateFlow()

    override fun setLyricsPresentation(presentation: LyricsPresentation) {
        _lyricsPresentation.value = presentation
    }

    private val _liquidGlassEnabled = MutableStateFlow(initialLiquidGlassEnabled)
    override val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        _liquidGlassEnabled.value = enabled
    }
}

expect fun createPlatformAppPreferences(): AppPreferences

val LocalAppPreferences = staticCompositionLocalOf<AppPreferences> {
    error("No AppPreferences provided")
}
