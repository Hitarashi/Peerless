@file:OptIn(InternalHazeApi::class)

package org.shilpo.peerless.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import dev.chrisbanes.haze.InternalHazeApi
import dev.chrisbanes.haze.RuntimeShaderUniformProvider
import dev.chrisbanes.haze.asComposeRenderEffect
import dev.chrisbanes.haze.createBlurRenderEffect
import dev.chrisbanes.haze.createMutableRuntimeShaderRenderEffect
import dev.chrisbanes.haze.createRuntimeEffect
import dev.chrisbanes.haze.isRuntimeShaderRenderEffectSupported
import kotlinx.coroutines.flow.first
import org.shilpo.peerless.preferences.LocalAppPreferences
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Liquid glass: backdrop capture -> gaussian blur -> edge refraction shader
// -> tint -> hairline rim.
//
// Public surface:
//   rememberLiquidGlassState()   shared state (settings flag, reduced motion)
//   LocalLiquidGlassState        ambient access for surfaces
//   Modifier.liquidGlassSource() marks content-behind (capture + scroll pulse)
//   Modifier.liquidGlass()       applies the material to a surface
//   rememberLiquidGlassTint()    theme/artwork-derived tint
//
// When the state is absent or disabled every modifier here is a no-op, so call
// sites keep their opaque fallback backgrounds.
// ---------------------------------------------------------------------------

private const val BLUR_RADIUS_FROSTED_DP = 40f
private const val BLUR_RADIUS_CLEAR_DP = 6f
private const val THICKNESS_DP = 11f
private const val REFRACT_INTENSITY = 0.75f
private const val REFRACT_INDEX = 1.5f
private const val PULSE_INTENSITY_BOOST = 0.10f

private const val DEFAULT_TINT_ALPHA = 0.85f
private const val ARTWORK_TINT_BLEND = 0.20f

private const val PULSE_STEPS = 5

internal val SHADER_SOURCE = """
uniform shader img;
uniform float2 resolution;
uniform float2 center;
uniform float2 size;
uniform float4 radius;
uniform float thickness;
uniform float refract_index;
uniform float refract_intensity;
uniform float4 foreground_color_premultiplied;

half sdfRect(half2 p, half4 r) {
  r.xy = (p.x > 0.0) ? r.xy : r.zw;
  r.x  = (p.y > 0.0) ? r.x  : r.y;
  half2 q = abs(p) - size + r.x;
  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
}

half4 srcOver(half4 src, half4 dst) {
    half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
    float outA = src.a + (1.0 - src.a) * dst.a;
    return half4(outRGB, outA);
}

half4 main(float2 fragCoord) {
  half2 p = fragCoord - center;
  half sd = sdfRect(p, radius);
  half2 uv = fragCoord;
  if (sd < 0.0) {
    half sdX = sdfRect(p + half2(1.0, 0.0), radius);
    half sdY = sdfRect(p + half2(0.0, 1.0), radius);

    half n_cos = max(thickness + sd, 0.0) / thickness;
    half n_cos2 = n_cos * n_cos;
    half n_sin = sqrt(1.0 - n_cos2);
    half3 normal = normalize(half3((sdX - sd) * n_cos, (sdY - sd) * n_cos, n_sin));

    half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);
    half h = sd < -thickness ? thickness : sqrt(sd * (-2.0 * thickness - sd));
    half refract_length = (h + 8.0 * thickness) / -refract_vec.z;

    uv += refract_vec.xy * refract_length * refract_intensity;
  }

  uv = clamp(uv, float2(0.0), resolution);
  half4 bg = img.eval(uv);
  half luma = dot(bg.rgb, half3(0.2126, 0.7152, 0.0722));
  bg.rgb = mix(half3(luma), bg.rgb, 2.2);
  return srcOver(half4(foreground_color_premultiplied), bg);
}
""".trimIndent()

@Stable
class LiquidGlassState internal constructor(
    internal val backdrop: GraphicsLayer,
    val isEnabled: Boolean,
    internal val reducedMotion: Boolean,
    private val pxPerDp: Float,
) {
    internal val sourcePosition = mutableStateOf(Offset.Zero)

    // Raw scroll activity in 0f..1f; only [pulse] reaches drawing, keeping
    // shader uniform updates quantized to PULSE_STEPS per scroll burst.
    internal val activity = mutableFloatStateOf(0f)

    internal val pulse = mutableIntStateOf(0)

    internal val scrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!reducedMotion && consumed.y != 0f) {
                addActivity(abs(consumed.y) / (PULSE_FULL_SCROLL_DP * pxPerDp))
            }
            return Offset.Zero
        }
    }

    private fun addActivity(delta: Float) {
        activity.floatValue = (activity.floatValue + delta).coerceAtMost(1f)
        publishPulse()
    }

    internal fun decayActivity(seconds: Float) {
        val current = activity.floatValue
        if (current <= 0f) return
        activity.floatValue = (current - PULSE_DECAY_PER_SECOND * seconds).coerceAtLeast(0f)
        publishPulse()
    }

    private fun publishPulse() {
        val bucket = (activity.floatValue * PULSE_STEPS).roundToInt().coerceIn(0, PULSE_STEPS)
        if (bucket != pulse.intValue) pulse.intValue = bucket
    }

    private companion object {
        const val PULSE_FULL_SCROLL_DP = 300f
        const val PULSE_DECAY_PER_SECOND = 1.5f
    }
}

val LocalLiquidGlassState: ProvidableCompositionLocal<LiquidGlassState?> =
    staticCompositionLocalOf { null }

@Composable
fun rememberLiquidGlassState(): LiquidGlassState {
    val prefs = LocalAppPreferences.current
    val enabled by prefs.liquidGlassEnabled.collectAsState()
    val reducedMotion = remember { isReducedMotionEnabled() }
    val backdrop = rememberGraphicsLayer()
    val density = LocalDensity.current.density

    val state = remember(enabled, backdrop, reducedMotion, density) {
        LiquidGlassState(
            backdrop = backdrop,
            isEnabled = enabled,
            reducedMotion = reducedMotion,
            pxPerDp = density,
        )
    }

    LaunchedEffect(state) {
        if (!state.reducedMotion) {
            while (true) {
                snapshotFlow { state.activity.floatValue }.first { it > 0f }
                var lastFrame = 0L
                while (state.activity.floatValue > 0f) {
                    withFrameNanos { now ->
                        if (lastFrame != 0L) {
                            state.decayActivity((now - lastFrame) / 1_000_000_000f)
                        }
                        lastFrame = now
                    }
                }
            }
        }
    }

    return state
}

fun Modifier.liquidGlassSource(state: LiquidGlassState?): Modifier {
    if (state?.isEnabled != true) return this
    return this
        .onGloballyPositioned {
            val pos = it.positionInRoot()
            state.sourcePosition.value = pos
        }
        .nestedScroll(state.scrollConnection)
        .drawWithContent {
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            if (w > 0 && h > 0) {
                state.backdrop.record(IntSize(w, h)) {
                    this@drawWithContent.drawContent()
                }
                drawLayer(state.backdrop)
            } else {
                drawContent()
            }
        }
}

@Composable
fun Modifier.liquidGlass(
    shape: RoundedCornerShape,
    tint: Color? = null,
    frosted: Boolean = true,
): Modifier {
    val state = LocalLiquidGlassState.current
    if (state == null) {
        return this
    }
    if (!state.isEnabled) {
        return this
    }
    if (!isRuntimeShaderRenderEffectSupported()) {
        return this
    }

    val shader = remember {
        runCatching { createRuntimeEffect(SHADER_SOURCE) }
            .getOrNull()
    }
    if (shader == null) {
        return this
    }

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val densityPx = density.density
    val resolvedTint = tint ?: colorScheme.surfaceContainerLow.copy(alpha = DEFAULT_TINT_ALPHA)
    val pulse = state.pulse.intValue

    var sourcePos by remember { mutableStateOf(Offset.Zero) }
    var thisPos by remember { mutableStateOf(Offset.Zero) }
    var nodeSize by remember { mutableStateOf(IntSize.Zero) }

    val blurLayer = rememberGraphicsLayer()
    val surface = rememberGraphicsLayer()

    val blurRadiusDp = if (frosted) BLUR_RADIUS_FROSTED_DP else BLUR_RADIUS_CLEAR_DP
    val blur = remember(densityPx, blurRadiusDp) {
        runCatching {
            createBlurRenderEffect(
                blurRadiusDp * densityPx,
                blurRadiusDp * densityPx,
                TileMode.Clamp,
            )
        }.getOrNull()
    }

    val blurRenderEffect = remember(blur) {
        blur?.asComposeRenderEffect()
    }

    val inputs = remember { arrayOf<dev.chrisbanes.haze.PlatformRenderEffect?>(null) }
    val mutableEffect = remember(shader, inputs) {
        runCatching {
            createMutableRuntimeShaderRenderEffect(shader, arrayOf("img"), inputs)
        }.getOrNull()
    }

    val opticalRenderEffect =
        remember(mutableEffect, resolvedTint, pulse, nodeSize, shape, density) {
            val w = nodeSize.width.toFloat()
            val h = nodeSize.height.toFloat()
            if (w <= 0f || h <= 0f) {
                null
            } else {
                val radii = cornerRadiiPx(shape, w, h, density, densityPx)
                val pulseFraction = pulse / PULSE_STEPS.toFloat()
                runCatching {
                    val effect = mutableEffect?.updateUniforms {
                        setGlassUniforms(
                            w,
                            h,
                            radii,
                            THICKNESS_DP * densityPx,
                            resolvedTint,
                            pulseFraction
                        )
                    }
                    effect?.asComposeRenderEffect()
                }.getOrNull()
            }
        }

    return this
        .onGloballyPositioned {
            thisPos = it.positionInRoot()
            nodeSize = it.size
        }
        .drawBehind {
            sourcePos = state.sourcePosition.value
            val w = size.width
            val h = size.height
            val offsetX = sourcePos.x - thisPos.x
            val offsetY = sourcePos.y - thisPos.y
            if (w <= 0f || h <= 0f) return@drawBehind

            val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
            val path = Path()
            when (outline) {
                is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                is Outline.Rectangle -> path.addRect(outline.rect)
                is Outline.Generic -> path.addPath(outline.path)
            }

            val outset = (blurRadiusDp * densityPx).roundToInt().coerceAtLeast(1)
            blurLayer.setOutsets(outset, outset, outset, outset)
            blurLayer.renderEffect = blurRenderEffect
            val targetSize = IntSize(w.roundToInt(), h.roundToInt())

            clipPath(path) {
                blurLayer.record(targetSize) {
                    translate(offsetX, offsetY) {
                        drawLayer(state.backdrop)
                    }
                }
                drawLayer(blurLayer)
                drawRect(resolvedTint)
            }
        }
}

private fun cornerRadiiPx(
    shape: RoundedCornerShape,
    w: Float,
    h: Float,
    density: androidx.compose.ui.unit.Density,
    densityPx: Float,
): FloatArray {
    val size = Size(w, h)
    var topLeft = shape.topStart.toPx(size, density)
    var topRight = shape.topEnd.toPx(size, density)
    var bottomRight = shape.bottomEnd.toPx(size, density)
    var bottomLeft = shape.bottomStart.toPx(size, density)

    val minDim = minOf(w, h)
    if (topLeft + bottomLeft > minDim && topLeft + bottomLeft > 0f) {
        val scale = minDim / (topLeft + bottomLeft)
        topLeft *= scale
        bottomLeft *= scale
    }
    if (topRight + bottomRight > minDim && topRight + bottomRight > 0f) {
        val scale = minDim / (topRight + bottomRight)
        topRight *= scale
        bottomRight *= scale
    }

    // (rightBottom, rightTop, leftBottom, leftTop)
    return floatArrayOf(bottomRight, topRight, bottomLeft, topLeft)
}

private fun RuntimeShaderUniformProvider.setGlassUniforms(
    w: Float,
    h: Float,
    radii: FloatArray,
    thicknessPx: Float,
    tint: Color,
    pulseFraction: Float,
) {
    val maxThickness = (minOf(w, h) / 5f).coerceAtLeast(1f)
    val clampedThickness = thicknessPx.coerceAtMost(maxThickness)
    setFloatUniform("resolution", w, h)
    setFloatUniform("center", w / 2f, h / 2f)
    setFloatUniform("size", w / 2f, h / 2f)
    setFloatUniform("radius", radii[0], radii[1], radii[2], radii[3])
    setFloatUniform("thickness", clampedThickness)
    setFloatUniform("refract_index", REFRACT_INDEX)
    setFloatUniform("refract_intensity", REFRACT_INTENSITY + PULSE_INTENSITY_BOOST * pulseFraction)
    val a = tint.alpha
    setFloatUniform(
        "foreground_color_premultiplied",
        tint.red * a,
        tint.green * a,
        tint.blue * a,
        a,
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

