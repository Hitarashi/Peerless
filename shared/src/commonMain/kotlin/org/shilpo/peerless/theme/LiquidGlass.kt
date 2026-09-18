package org.shilpo.peerless.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object LiquidGlassDefaults {
    val ContainerColor: Color = Color(0xFF14141B).copy(alpha = 0.78f)
    val ElevatedContainerColor: Color = Color(0xFF181824).copy(alpha = 0.88f)
    val SubtleContainerColor: Color = Color(0xFF0F0F16).copy(alpha = 0.65f)

    val BorderBrush: Brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val AccentBorderBrush: Brush = Brush.verticalGradient(
        listOf(
            PrimaryDark.copy(alpha = 0.40f),
            SecondaryDark.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val SpecularSheenBrush: Brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.015f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.12f)
        )
    )

    val BorderWidth: Dp = 1.dp
    val DefaultShape: Shape = RoundedCornerShape(24.dp)
}

fun Modifier.liquidGlass(
    shape: Shape = LiquidGlassDefaults.DefaultShape,
    containerColor: Color = LiquidGlassDefaults.ContainerColor,
    borderBrush: Brush = LiquidGlassDefaults.BorderBrush,
    borderWidth: Dp = LiquidGlassDefaults.BorderWidth,
    specularSheen: Boolean = true
): Modifier = this
    .clip(shape)
    .background(containerColor)
    .then(
        if (specularSheen) {
            Modifier.background(LiquidGlassDefaults.SpecularSheenBrush)
        } else {
            Modifier
        }
    )
    .border(width = borderWidth, brush = borderBrush, shape = shape)

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDefaults.DefaultShape,
    containerColor: Color = LiquidGlassDefaults.ContainerColor,
    borderBrush: Brush = LiquidGlassDefaults.BorderBrush,
    borderWidth: Dp = LiquidGlassDefaults.BorderWidth,
    specularSheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            containerColor = containerColor,
            borderBrush = borderBrush,
            borderWidth = borderWidth,
            specularSheen = specularSheen
        ),
        content = content
    )
}

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
    primaryGlow: Color = PrimaryDark,
    secondaryGlow: Color = SecondaryDark,
    tertiaryGlow: Color = TertiaryDark,
    glowAlpha: Float = 0.25f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .ambientGlow(
                primaryGlow = primaryGlow,
                secondaryGlow = secondaryGlow,
                tertiaryGlow = tertiaryGlow,
                glowAlpha = glowAlpha
            ),
        content = content
    )
}
