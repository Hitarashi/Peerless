package org.shilpo.peerless.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DesktopAppPreferences : AppPreferences {
    private val presentationFile: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        File(File(userHome, ".config/peerless"), "lyrics.presentation")
    }

    private val _lyricsPresentation = MutableStateFlow(
        LyricsPresentation.fromStoredValue(readPresentation())
    )
    override val lyricsPresentation: StateFlow<LyricsPresentation> =
        _lyricsPresentation.asStateFlow()

    override fun setLyricsPresentation(presentation: LyricsPresentation) {
        try {
            presentationFile.parentFile?.mkdirs()
            presentationFile.writeText(presentation.name)
        } catch (_: Exception) {
        }
        _lyricsPresentation.value = presentation
    }

    private fun readPresentation(): String? = try {
        presentationFile.takeIf(File::exists)?.readText()?.trim()
    } catch (_: Exception) {
        null
    }

    private val liquidGlassFile: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        File(File(userHome, ".config/peerless"), "liquid_glass.enabled")
    }

    private val _liquidGlassEnabled = MutableStateFlow(readLiquidGlass())
    override val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        try {
            liquidGlassFile.parentFile?.mkdirs()
            liquidGlassFile.writeText(if (enabled) "true" else "false")
        } catch (_: Exception) {
        }
        _liquidGlassEnabled.value = enabled
    }

    private fun readLiquidGlass(): Boolean = try {
        liquidGlassFile.takeIf(File::exists)?.readText()?.trim() != "false"
    } catch (_: Exception) {
        true
    }
}

actual fun createPlatformAppPreferences(): AppPreferences = DesktopAppPreferences()
