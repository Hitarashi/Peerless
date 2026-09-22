package org.shilpo.peerless.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Draws three soft radial glows (primary/secondary/tertiary) behind the content.
 *
 * Moved out of the old fake `LiquidGlass.kt` so the ambient glow decoration survives
 * independently of the real Haze-based liquid glass system.
 */
fun Modifier.ambientGlow(
    primaryGlow: Color = PrimaryDark,
    secondaryGlow: Color = SecondaryDark,
    tertiaryGlow: Color = TertiaryDark,
    glowAlpha: Float = 0.25f
): Modifier = this.drawBehind {
    val canvasWidth = size.width
    val canvasHeight = size.height

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryGlow.copy(alpha = glowAlpha),
                primaryGlow.copy(alpha = glowAlpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(canvasWidth * 0.25f, canvasHeight * 0.2f),
            radius = canvasWidth * 0.75f
        )
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondaryGlow.copy(alpha = glowAlpha * 0.75f),
                secondaryGlow.copy(alpha = glowAlpha * 0.25f),
                Color.Transparent
            ),
            center = Offset(canvasWidth * 0.8f, canvasHeight * 0.45f),
            radius = canvasWidth * 0.65f
        )
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tertiaryGlow.copy(alpha = glowAlpha * 0.6f),
                Color.Transparent
            ),
            center = Offset(canvasWidth * 0.5f, canvasHeight * 0.85f),
            radius = canvasWidth * 0.7f
        )
    )
}

@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    primaryGlow: Color = MaterialTheme.colorScheme.primary,
    secondaryGlow: Color = MaterialTheme.colorScheme.secondary,
    tertiaryGlow: Color = MaterialTheme.colorScheme.tertiary,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    glowAlpha: Float = 0.25f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .ambientGlow(
                primaryGlow = primaryGlow,
                secondaryGlow = secondaryGlow,
                tertiaryGlow = tertiaryGlow,
                glowAlpha = glowAlpha
            ),
        content = content
    )
}
