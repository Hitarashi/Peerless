package org.shilpo.peerless.ui.components

/** Defaults match BetterLyrics' normal word-synced lyrics presentation. */
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

/** Background effects are opt-in except for the artwork-derived fluid color layer. */
data class LyricsBackgroundOptions(
    val fluid: Boolean = true,
    val snow: Boolean = false,
    val fog: Boolean = false,
    val rain: Boolean = false
)
