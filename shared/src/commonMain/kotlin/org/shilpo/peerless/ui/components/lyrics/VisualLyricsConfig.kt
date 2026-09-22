package org.shilpo.peerless.ui.components.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight

@Immutable
data class VisualLyricsConfig(
    val enableSparkles: Boolean = false,
    val enableBlur: Boolean = true,
    val enableVocalElevation: Boolean = true,
    val enableElasticScroll: Boolean = true,
    val enableWaitingDots: Boolean = true,
    val enableAnticipation: Boolean = false,
    val fontSizeScale: Float = 1.0f,
    val fontWeight: FontWeight = FontWeight.Bold,
    val idleRecoveryMs: Long = 2000L,
    val blurFactor: Float = 1.5f,
    val maxBlurRadiusDp: Float = 4.5f,
    val syncOffsetMs: Long = -750L
)
