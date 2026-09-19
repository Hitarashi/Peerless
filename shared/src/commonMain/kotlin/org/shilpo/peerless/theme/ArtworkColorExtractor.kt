package org.shilpo.peerless.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.materialkolor.ktx.themeColorOrNull
import kotlin.math.abs
import kotlin.math.max

internal expect fun createSoftwareArtworkRequest(
    context: PlatformContext,
    artworkUrl: String,
    size: Int
): ImageRequest

fun Painter.toSampledImageBitmap(sampleWidth: Int = 64, sampleHeight: Int = 64): ImageBitmap {
    val bitmap = ImageBitmap(sampleWidth, sampleHeight)
    val canvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(sampleWidth.toFloat(), sampleHeight.toFloat())
    ) {
        draw(this.size)
    }
    return bitmap
}

fun extractDominantArtworkColor(bitmap: ImageBitmap, fallback: Color): Color {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    try {
        bitmap.readPixels(pixels)
    } catch (_: Throwable) {
        return fallback
    }

    var bestColor: Color? = null
    var bestScore = -1f

    for (argb in pixels) {
        val a = (argb shr 24) and 0xFF
        if (a < 128) continue
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f

        val hsv = rgbToHsv(r, g, b)
        val s = hsv[1]
        val v = hsv[2]

        if (v < 0.10f || v > 0.96f) continue

        val score = s * 2.2f + (1f - abs(v - 0.55f))
        if (score > bestScore) {
            bestScore = score
            bestColor = Color(r, g, b, 1f)
        }
    }

    return bestColor ?: fallback
}

fun extractArtworkSeedColor(painter: Painter, fallback: Color): Color {
    val bitmap = runCatching {
        painter.toSampledImageBitmap(sampleWidth = 64, sampleHeight = 64)
    }.getOrNull() ?: return fallback

    // Prefer pixels sampled from the rendered cover. MaterialKolor can return a
    // plausible but unrelated theme seed for poster-style artwork, which makes
    // the mini-player appear to keep the app's default color.
    val sampledColor = extractDominantArtworkColor(bitmap, fallback)
    if (sampledColor != fallback) return sampledColor

    return runCatching { bitmap.themeColorOrNull(filter = false) }.getOrNull() ?: fallback
}

private val artworkColorCache = mutableMapOf<String, Color>()

@Composable
fun rememberArtworkSeedColor(
    artworkUrl: String?,
    fallbackColor: Color = MaterialTheme.colorScheme.primary
): Color {
    val cached = remember(artworkUrl) {
        if (!artworkUrl.isNullOrBlank()) artworkColorCache[artworkUrl] else null
    }

    var seedColor by remember(artworkUrl) { mutableStateOf(cached ?: fallbackColor) }

    if (!artworkUrl.isNullOrBlank() && cached == null) {
        val context = LocalPlatformContext.current
        val request = remember(artworkUrl, context) {
            createSoftwareArtworkRequest(context, artworkUrl, size = 64)
        }
        val painter = rememberAsyncImagePainter(
            model = request,
            onSuccess = { state ->
                val extracted = extractArtworkSeedColor(state.painter, fallbackColor)
                artworkColorCache[artworkUrl] = extracted
                seedColor = extracted
            }
        )

        // Attach the probe painter so Coil starts loading the image for color sampling.
        Image(
            painter = painter,
            contentDescription = null,
            modifier = androidx.compose.ui.Modifier.size(1.dp).alpha(0f)
        )
    }

    return seedColor
}

@Immutable
data class MiniPlayerGlowPalette(
    val first: Color,
    val second: Color,
    val third: Color,
    val fourth: Color
)

fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    var h = 0f
    val s = if (max == 0f) 0f else delta / max
    val v = max

    if (delta != 0f) {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        } * 60f
        if (h < 0f) h += 360f
    }
    return floatArrayOf(h, s, v)
}

fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m, alpha)
}

fun hueShift(color: Color, degrees: Float): Color {
    val hsv = rgbToHsv(color.red, color.green, color.blue)
    val newHue = ((hsv[0] + degrees) % 360f + 360f) % 360f
    return hsvToColor(newHue, hsv[1], hsv[2], color.alpha)
}

fun tuneColorForGlow(
    color: Color,
    saturationMin: Float = 0.60f,
    saturationBoost: Float = 1.15f,
    valueTarget: Float = 0.78f
): Color {
    val hsv = rgbToHsv(color.red, color.green, color.blue)
    val newSat = (max(hsv[1], saturationMin) * saturationBoost).coerceIn(0f, 1f)
    val newVal = (hsv[2] * 0.85f + valueTarget * 0.15f).coerceIn(0.38f, 0.92f)
    return hsvToColor(hsv[0], newSat, newVal, color.alpha)
}

@Composable
fun rememberMiniPlayerGlowPalette(seedColor: Color): MiniPlayerGlowPalette {
    val spec = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val targetFirst = remember(seedColor) { tuneColorForGlow(seedColor, saturationMin = 0.55f, valueTarget = 0.80f) }
    val targetSecond =
        remember(seedColor) { tuneColorForGlow(hueShift(seedColor, 18f), saturationMin = 0.65f, valueTarget = 0.72f) }
    val targetThird =
        remember(seedColor) { tuneColorForGlow(hueShift(seedColor, -15f), saturationMin = 0.45f, valueTarget = 0.88f) }
    val targetFourth =
        remember(seedColor) { tuneColorForGlow(hueShift(seedColor, 8f), saturationMin = 0.60f, valueTarget = 0.60f) }

    val first by animateColorAsState(targetFirst, animationSpec = spec, label = "glowFirst")
    val second by animateColorAsState(targetSecond, animationSpec = spec, label = "glowSecond")
    val third by animateColorAsState(targetThird, animationSpec = spec, label = "glowThird")
    val fourth by animateColorAsState(targetFourth, animationSpec = spec, label = "glowFourth")

    return remember(first, second, third, fourth) {
        MiniPlayerGlowPalette(
            first = first,
            second = second,
            third = third,
            fourth = fourth
        )
    }
}
