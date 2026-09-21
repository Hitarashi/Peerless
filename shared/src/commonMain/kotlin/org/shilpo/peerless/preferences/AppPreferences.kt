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
}

class InMemoryAppPreferences(
    initialLyricsPresentation: LyricsPresentation = LyricsPresentation.VISUAL
) : AppPreferences {
    private val _lyricsPresentation = MutableStateFlow(initialLyricsPresentation)
    override val lyricsPresentation: StateFlow<LyricsPresentation> =
        _lyricsPresentation.asStateFlow()

    override fun setLyricsPresentation(presentation: LyricsPresentation) {
        _lyricsPresentation.value = presentation
    }
}

expect fun createPlatformAppPreferences(): AppPreferences

val LocalAppPreferences = staticCompositionLocalOf<AppPreferences> {
    error("No AppPreferences provided")
}
