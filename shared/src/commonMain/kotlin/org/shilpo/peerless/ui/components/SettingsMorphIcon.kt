package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val SETTINGS_MORPH_SAMPLES = 512
private const val SETTINGS_VIEWBOX_SIZE = 24f
private const val SETTINGS_NORMAL_CENTER_PATH =
    "M12.012 14.83c-1.604 0-2.902-1.25-2.902-2.82s1.298-2.83 2.902-2.83 2.872 1.26 2.872 2.83-1.267 2.82-2.872 2.82Z"
private const val SETTINGS_NORMAL_GEAR_PATH =
    "M21.23 14.37c-.194-.3-.47-.6-.828-.79-.286-.14-.47-.37-.634-.64-.521-.86-.215-1.99.654-2.5a2.027 2.027 0 0 0 .756-2.83l-.685-1.18a2.11 2.11 0 0 0-2.872-.76c-.899.48-2.054.16-2.575-.69-.164-.28-.255-.58-.235-.88.031-.39-.092-.76-.276-1.06C14.158 2.42 13.473 2 12.717 2h-1.441a2.2 2.2 0 0 0-1.809 1.04c-.194.3-.307.67-.286 1.06.02.3-.072.6-.235.88-.521.85-1.676 1.17-2.565.69a2.124 2.124 0 0 0-2.882.76l-.685 1.18c-.583.99-.255 2.26.756 2.83.869.51 1.175 1.64.664 2.5-.174.27-.358.5-.644.64-.347.19-.654.49-.818.79-.378.62-.358 1.4.02 2.05l.705 1.2a2.13 2.13 0 0 0 1.819 1.04c.347 0 .756-.1 1.083-.3.255-.17.562-.23.899-.23 1.012 0 1.86.83 1.88 1.82 0 1.15.94 2.05 2.126 2.05h1.39c1.175 0 2.115-.9 2.115-2.05.031-.99.879-1.82 1.891-1.82.327 0 .634.06.899.23.327.2.726.3 1.083.3.726 0 1.431-.4 1.809-1.04l.715-1.2c.368-.67.399-1.43.02-2.05Z"
private const val SETTINGS_FILLED_PATH =
    "M12.717 2c.756 0 1.441.42 1.819 1.04.184.3.307.67.276 1.06-.02.3.072.6.235.88.521.85 1.676 1.17 2.575.69a2.11 2.11 0 0 1 2.872.76l.685 1.18c.593.99.266 2.26-.756 2.83-.869.51-1.175 1.64-.654 2.5.164.27.347.5.634.64.358.19.634.49.828.79.378.62.347 1.38-.02 2.05l-.715 1.2a2.13 2.13 0 0 1-1.809 1.04c-.358 0-.756-.1-1.083-.3-.266-.17-.572-.23-.899-.23-1.012 0-1.86.83-1.891 1.82 0 1.15-.94 2.05-2.115 2.05h-1.39c-1.185 0-2.126-.9-2.126-2.05-.02-.99-.869-1.82-1.88-1.82-.337 0-.644.06-.899.23-.327.2-.736.3-1.083.3a2.13 2.13 0 0 1-1.819-1.04l-.705-1.2c-.378-.65-.399-1.43-.02-2.05.164-.3.47-.6.818-.79.286-.14.47-.37.644-.64.511-.86.204-1.99-.664-2.5a2.044 2.044 0 0 1-.756-2.83l.685-1.18a2.124 2.124 0 0 1 2.882-.76c.889.48 2.044.16 2.565-.69.164-.28.255-.58.235-.88-.02-.39.092-.76.286-1.06A2.2 2.2 0 0 1 11.276 2ZM12.012 9.18c-1.604 0-2.902 1.26-2.902 2.83s1.298 2.82 2.902 2.82 2.872-1.25 2.872-2.82-1.267-2.83-2.872-2.83Z"

@Composable
fun SettingsMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Settings",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SettingsMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val shapeMorph = remember {
        val filledContours = settingsSampleContours(SETTINGS_FILLED_PATH)
        check(filledContours.size == 2) {
            "Filled settings SVG must contain gear and center contours."
        }
        val normalGear = settingsSampleContour(SETTINGS_NORMAL_GEAR_PATH)
        val normalCenter = settingsSampleContour(SETTINGS_NORMAL_CENTER_PATH)
        val selectedGear = settingsAlignContour(normalGear, filledContours[0])
        val selectedCenter = settingsAlignContour(normalCenter, filledContours[1])
        val fullGear = settingsAlignContour(normalCenter, filledContours[0])
        SettingsShapeMorph(
            gear = SettingsContourMorph(
                start = normalGear,
                end = selectedGear,
            ),
            center = SettingsContourMorph(
                start = normalCenter,
                end = selectedCenter,
            ),
            centerFill = SettingsCenterFillMorph(
                initialCenter = normalCenter,
                fullGear = fullGear,
                selectedCenter = settingsAlignContour(fullGear, filledContours[1]),
            ),
        )
    }
    val gearPath = remember { Path() }
    val centerPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawSettingsMorph(
            progress = progress,
            tint = tint,
            shapeMorph = shapeMorph,
            gearPath = gearPath,
            centerPath = centerPath,
        )
    }
}

private data class SettingsContourMorph(
    val start: List<Offset>,
    val end: List<Offset>,
)

private data class SettingsShapeMorph(
    val gear: SettingsContourMorph,
    val center: SettingsContourMorph,
    val centerFill: SettingsCenterFillMorph,
)

private data class SettingsCenterFillMorph(
    val initialCenter: List<Offset>,
    val fullGear: List<Offset>,
    val selectedCenter: List<Offset>,
)

private fun settingsSampleContours(pathData: String): List<List<Offset>> =
    settingsSplitContours(pathData).map(::settingsSampleContour)

private fun settingsSampleContour(pathData: String): List<Offset> {
    val sourcePath = PathParser().parsePathString(pathData).toNodes().toPath()
    val measure = PathMeasure().apply {
        setPath(sourcePath, forceClosed = true)
    }
    val contourLength = measure.length

    return List(SETTINGS_MORPH_SAMPLES) { index ->
        measure.getPosition(contourLength * index / SETTINGS_MORPH_SAMPLES)
    }
}

private fun settingsSplitContours(pathData: String): List<String> {
    val contours = mutableListOf<String>()
    var contourStart = 0
    var previousNonWhitespace: Char? = null

    pathData.forEachIndexed { index, command ->
        if (command == 'M' && index > contourStart && previousNonWhitespace == 'Z') {
            contours += pathData.substring(contourStart, index)
            contourStart = index
        }
        if (!command.isWhitespace()) previousNonWhitespace = command
    }
    contours += pathData.substring(contourStart)
    return contours
}

private fun settingsAlignContour(start: List<Offset>, end: List<Offset>): List<Offset> {
    val count = start.size
    var bestOffset = 0
    var bestReversed = false
    var bestScore = Float.POSITIVE_INFINITY

    for (reversed in listOf(false, true)) {
        for (offset in 0 until count) {
            var score = 0f
            for (index in 0 until count) {
                val endIndex = if (reversed) {
                    (offset - index + count) % count
                } else {
                    (offset + index) % count
                }
                val dx = start[index].x - end[endIndex].x
                val dy = start[index].y - end[endIndex].y
                score += dx * dx + dy * dy
            }
            if (score < bestScore) {
                bestScore = score
                bestOffset = offset
                bestReversed = reversed
            }
        }
    }

    return List(count) { index ->
        val endIndex = if (bestReversed) {
            (bestOffset - index + count) % count
        } else {
            (bestOffset + index) % count
        }
        end[endIndex]
    }
}

private fun DrawScope.drawSettingsMorph(
    progress: Float,
    tint: Color,
    shapeMorph: SettingsShapeMorph,
    gearPath: Path,
    centerPath: Path,
) {
    val p = progress.coerceIn(0f, 1f)
    val bodyProgress = (p * 2f).coerceIn(0f, 1f)
    settingsUpdatePath(gearPath, shapeMorph.gear, bodyProgress)
    settingsAppendContour(gearPath, shapeMorph.center, bodyProgress)
    settingsUpdateCenterFill(centerPath, shapeMorph.centerFill, p)
    gearPath.fillType = PathFillType.EvenOdd

    val iconSize = min(size.width, size.height)
    val iconScale = iconSize / SETTINGS_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f
    val rotationDegrees = 180f * p

    withTransform({
        rotate(rotationDegrees, pivot = Offset(size.width / 2f, size.height / 2f))
        translate(left = left, top = top)
        scale(scaleX = iconScale, scaleY = iconScale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = gearPath,
            color = tint.copy(alpha = tint.alpha * settingsLerp(0.4f, 1f, bodyProgress)),
        )
        if (p < 1f) {
            drawPath(
                path = centerPath,
                color = tint.copy(alpha = tint.alpha * settingsCenterFillOpacity(p)),
            )
        }
    }
}

private fun settingsUpdateCenterFill(
    path: Path,
    morph: SettingsCenterFillMorph,
    progress: Float,
) {
    val phaseProgress = if (progress <= 0.5f) {
        progress * 2f
    } else {
        (progress - 0.5f) * 2f
    }
    val start = if (progress <= 0.5f) morph.initialCenter else morph.fullGear
    val end = if (progress <= 0.5f) morph.fullGear else morph.selectedCenter

    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        settingsLerp(start[0].x, end[0].x, phaseProgress),
        settingsLerp(start[0].y, end[0].y, phaseProgress),
    )
    for (pointIndex in 1 until SETTINGS_MORPH_SAMPLES) {
        path.lineTo(
            settingsLerp(start[pointIndex].x, end[pointIndex].x, phaseProgress),
            settingsLerp(start[pointIndex].y, end[pointIndex].y, phaseProgress),
        )
    }
    path.close()
}

private fun settingsCenterFillOpacity(progress: Float): Float =
    if (progress <= 0.5f) 1f else 1f - (progress - 0.5f) * 2f

private fun settingsUpdatePath(
    path: Path,
    morph: SettingsContourMorph,
    progress: Float,
) {
    path.reset()
    path.fillType = PathFillType.NonZero
    path.moveTo(
        settingsLerp(morph.start[0].x, morph.end[0].x, progress),
        settingsLerp(morph.start[0].y, morph.end[0].y, progress),
    )
    for (pointIndex in 1 until SETTINGS_MORPH_SAMPLES) {
        path.lineTo(
            settingsLerp(
                morph.start[pointIndex].x,
                morph.end[pointIndex].x,
                progress,
            ),
            settingsLerp(
                morph.start[pointIndex].y,
                morph.end[pointIndex].y,
                progress,
            ),
        )
    }
    path.close()
}

private fun settingsAppendContour(
    path: Path,
    morph: SettingsContourMorph,
    progress: Float,
) {
    path.moveTo(
        settingsLerp(morph.start[0].x, morph.end[0].x, progress),
        settingsLerp(morph.start[0].y, morph.end[0].y, progress),
    )
    for (pointIndex in 1 until SETTINGS_MORPH_SAMPLES) {
        path.lineTo(
            settingsLerp(
                morph.start[pointIndex].x,
                morph.end[pointIndex].x,
                progress,
            ),
            settingsLerp(
                morph.start[pointIndex].y,
                morph.end[pointIndex].y,
                progress,
            ),
        )
    }
    path.close()
}

private fun settingsLerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
