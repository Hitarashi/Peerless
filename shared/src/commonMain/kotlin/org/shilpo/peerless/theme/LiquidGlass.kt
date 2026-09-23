package org.shilpo.peerless.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.shilpo.peerless.preferences.LocalAppPreferences

// ---------------------------------------------------------------------------
// Liquid glass / Frosted glass blur:
// Integrates Chris Banes' Haze engine to deliver authentic, hardware-accelerated
// frosted Gaussian blur and adaptive material translucency across Android, iOS,
// and Desktop JVM.
//
// Public surface:
//   rememberLiquidGlassState()   shared state (settings flag, haze state)
//   LocalLiquidGlassState        ambient access for surfaces
//   Modifier.liquidGlassSource() marks content-behind (source capture for blur)
//   Modifier.liquidGlass()       applies frosted glass blur to a surface
//   rememberLiquidGlassTint()    theme/artwork-derived tint
//
// When the state is absent or disabled every modifier here is a no-op, so call
// sites keep their opaque fallback backgrounds.
// ---------------------------------------------------------------------------

private val BLUR_RADIUS_FROSTED: Dp = 95.dp
private val BLUR_RADIUS_CLEAR: Dp = 90.dp
private const val NOISE_FACTOR: Float = 0.10f

private const val DEFAULT_TINT_ALPHA: Float = 0.95f
private const val ARTWORK_TINT_BLEND: Float = 0.0f

@Stable
class LiquidGlassState internal constructor(
    val hazeState: HazeState,
    val isEnabled: Boolean,
)

val LocalLiquidGlassState: ProvidableCompositionLocal<LiquidGlassState?> =
    staticCompositionLocalOf { null }

@Composable
fun rememberLiquidGlassState(): LiquidGlassState {
    val prefs = LocalAppPreferences.current
    val enabled by prefs.liquidGlassEnabled.collectAsState()
    val hazeState = rememberHazeState()

    return remember(enabled, hazeState) {
        LiquidGlassState(
            hazeState = hazeState,
            isEnabled = enabled,
        )
    }
}

fun Modifier.liquidGlassSource(state: LiquidGlassState?): Modifier {
    if (state == null || !state.isEnabled) return this
    return this.hazeSource(state = state.hazeState)
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color? = null,
    frosted: Boolean = true,
): Modifier {
    val state = LocalLiquidGlassState.current
    if (state == null || !state.isEnabled) {
        return this
    }

    val colorScheme = MaterialTheme.colorScheme
    val resolvedTint = tint ?: colorScheme.surfaceContainerLow.copy(alpha = DEFAULT_TINT_ALPHA)
    val blurRadius = if (frosted) BLUR_RADIUS_FROSTED else BLUR_RADIUS_CLEAR

    val blurStyle = remember(resolvedTint, blurRadius, shape) {
        HazeBlurStyle {
            blurRadius(blurRadius)
            noiseFactor(NOISE_FACTOR)
            backgroundColor(resolvedTint)
            blurredEdgeTreatment(BlurredEdgeTreatment(shape))
        }
    }
    val hazeInput = remember(state.hazeState) {
        HazeInput.Sources(state.hazeState)
    }

    return this
        .clip(shape)
        .hazeBlur(
            input = hazeInput,
            style = blurStyle,
        )
}

@Composable
fun rememberLiquidGlassTint(
    artworkColor: Color? = null,
    alpha: Float = DEFAULT_TINT_ALPHA,
): Color {
    val colorScheme = MaterialTheme.colorScheme
    val base = colorScheme.surfaceContainerLow.copy(alpha = alpha)
    if (artworkColor == null) return base
    return lerp(base, artworkColor.copy(alpha = alpha), ARTWORK_TINT_BLEND)
}
