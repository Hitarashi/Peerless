package org.shilpo.peerless.theme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.rememberAsyncImagePainter
import com.materialkolor.ktx.themeColorOrNull

/**
 * Converts any Compose Painter to a compact ImageBitmap (e.g. 64x64) for rapid color quantization.
 */
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

private val artworkColorCache = mutableMapOf<String, Color>()

/**
 * Extracts a dynamic seed color from an album artwork URL.
 * Falls back smoothly to the default Expressive lilac (PrimaryDark) if loading fails or no artwork is available.
 */
@Composable
fun rememberArtworkSeedColor(
    artworkUrl: String?,
    fallbackColor: Color = PrimaryDark
): Color {
    val cached = remember(artworkUrl) {
        if (!artworkUrl.isNullOrBlank()) artworkColorCache[artworkUrl] else null
    }

    var seedColor by remember(artworkUrl) { mutableStateOf(cached ?: fallbackColor) }

    if (!artworkUrl.isNullOrBlank() && cached == null) {
        val painter = rememberAsyncImagePainter(
            model = artworkUrl,
            onSuccess = { state ->
                try {
                    val bitmap = state.painter.toSampledImageBitmap(sampleWidth = 64, sampleHeight = 64)
                    val extracted = bitmap.themeColorOrNull()
                    if (extracted != null) {
                        artworkColorCache[artworkUrl] = extracted
                        seedColor = extracted
                    }
                } catch (_: Throwable) {
                }
            }
        )

        LaunchedEffect(painter) {
        }
    }

    return seedColor
}
