package org.shilpo.peerless.ui.components

import org.shilpo.peerless.preferences.LyricsPresentation
import org.shilpo.peerless.ui.components.lyrics.VisualLyricsConfig

data class LyricsAnimationOptions(
    val wordByWord: Boolean = true,
    val blur: Boolean = true,
    val fadeOut: Boolean = true,
    val edgeFeathering: Boolean = true,
    val outOfSightScale: Boolean = true,
    val glow: Boolean = true,
    val shadow: Boolean = false,
    val scale: Boolean = true,
    val float: Boolean = true,
    val breathing: Boolean = false,
    val fan: Boolean = false,
    val threeDimensional: Boolean = false
)

data class LyricsBackgroundOptions(
    val fluid: Boolean = true,
    val snow: Boolean = false,
    val fog: Boolean = false,
    val rain: Boolean = false
)

internal fun LyricsPresentation.animationOptions(): LyricsAnimationOptions = when (this) {
    LyricsPresentation.VISUAL -> LyricsAnimationOptions()
    LyricsPresentation.READABLE -> LyricsAnimationOptions(
        blur = false,
        fadeOut = false,
        edgeFeathering = false,
        outOfSightScale = false,
        glow = false,
        shadow = false,
        scale = false,
        float = false,
        breathing = false,
        fan = false,
        threeDimensional = false
    )
}

internal fun LyricsPresentation.visualConfig(syncOffsetMs: Long): VisualLyricsConfig = when (this) {
    LyricsPresentation.VISUAL -> VisualLyricsConfig(syncOffsetMs = syncOffsetMs)
    LyricsPresentation.READABLE -> VisualLyricsConfig(
        enableSparkles = false,
        enableBlur = false,
        enableVocalElevation = false,
        enableElasticScroll = false,
        syncOffsetMs = syncOffsetMs
    )
}
