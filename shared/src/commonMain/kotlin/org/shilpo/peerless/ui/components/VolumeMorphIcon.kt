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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

private const val VOLUME_ICON_VIEWBOX_SIZE = 96f
private const val VOLUME_MUTE_SLASH_WIDTH = 6f

private const val VOLUME_SPEAKER_PATH =
    "M21 38v20h1v5h1v2h1v1h1v1h1v1h2v1h1v1h1v1h1v1h2v1h1v1h1v1h1v1h2v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h1v1h4v1h2v-1h4v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-3h1v-8h1V34h-1v-9h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-4v-1h-1v1h-4v1h-2v1h-2v1h-1v1h-2v1h-2v1h-1v1h-2v1h-1v1h-2v1h-8v1h-6v1h-2v1h-1v1h-1v1h-1v2h-1v5Z"

private val VOLUME_SPEAKER_PATH_LAYERS = listOf(
    0.024f to "M20 38v1h1v-1ZM20 57v1h1v-1ZM21 62v1h1v-1ZM23 66v1h1v-1Z",
    0.027f to "M56 15v1h1v-1ZM54 16v1h1v-1ZM49 19v1h1v-1ZM46 21v1h1v-1ZM73 22v1h1v-1ZM41 24v1h1v-1ZM36 25v1h1v-1ZM75 35v1h1v-1ZM36 70v1h1v-1Z",
    0.051f to "M69 17v1h1v-1ZM51 18v1h1v-1ZM70 18v1h1v-1ZM71 19v1h1v-1ZM43 23v1h1v-1ZM37 25v1h1v-1ZM22 31v1h1v-1ZM21 33v1h1v-1ZM75 36v3h1v-3ZM20 56v1h1v-1ZM75 57v3h1v-3ZM37 70v1h1v-1ZM43 72v1h1v-1ZM73 73v1h1v-1ZM46 74v1h1v-1ZM71 76v1h1v-1ZM51 77v1h1v-1ZM70 77v1h1v-1ZM69 78v1h1v-1ZM54 79v1h1v-1ZM56 80v1h1v-1Z",
    0.075f to "M38 70v1h1v-1Z",
    0.078f to "M38 25v1h1v-1ZM74 25v1h1v-1ZM20 39v1h1v-1Z",
    0.102f to "M65 15v1h1v-1ZM48 20v1h1v-1ZM28 26v1h1v-1ZM26 27v1h1v-1ZM75 39v5h1v-5ZM20 40v2h1v-2ZM75 52v5h1v-5ZM20 54v2h1v-2ZM22 64v1h1v-1ZM28 69v1h1v-1ZM39 70v1h1v-1ZM74 70v1h1v-1ZM48 75v1h1v-1ZM67 79v1h1v-1ZM65 80v1h1v-1Z",
    0.125f to "M39 25v1h1v-1ZM20 42v1h1v-1ZM75 51v1h1v-1ZM20 53v1h1v-1ZM26 68v1h1v-1Z",
    0.149f to "M67 16v1h1v-1ZM21 34v1h1v-1ZM20 43v10h1V43ZM75 44v7h1v-7ZM21 61v1h1v-1Z",
    0.153f to "M45 73v1h1v-1ZM53 78v1h1v-1ZM57 80v1h1v-1Z",
    0.173f to "M57 15v1h1v-1Z",
    0.176f to "M53 17v1h1v-1ZM45 22v1h1v-1ZM74 26v1h1v-1ZM74 69v1h1v-1Z",
    0.2f to "M55 16v1h1v-1ZM50 19v1h1v-1ZM72 21v1h1v-1ZM73 23v1h1v-1ZM42 24v1h1v-1ZM29 26v1h1v-1ZM29 69v1h1v-1ZM42 71v1h1v-1ZM72 74v1h1v-1ZM50 76v1h1v-1Z",
    0.224f to "M47 21v1h1v-1ZM25 28v1h1v-1ZM23 30v1h1v-1ZM21 60v1h1v-1ZM23 65v1h1v-1ZM25 67v1h1v-1Z",
    0.227f to "M64 15v1h1v-1ZM40 25v1h1v-1ZM74 27v1h1v-1ZM74 68v1h1v-1ZM40 70v1h1v-1ZM64 80v1h1v-1Z",
    0.251f to "M58 15v1h1v-1ZM68 17v1h1v-1ZM74 28v1h1v-1ZM22 32v1h1v-1ZM21 35v1h1v-1ZM74 67v1h1v-1ZM73 72v1h1v-1ZM47 74v1h1v-1ZM55 79v1h1v-1Z",
    0.275f to "M30 69v1h1v-1ZM58 80v1h1v-1Z",
    0.278f to "M30 26v1h1v-1ZM74 29v1h1v-1Z",
    0.302f to "M63 15v1h1v-1ZM52 18v1h1v-1ZM44 23v1h1v-1ZM27 27v1h1v-1ZM74 30v1h1v-1ZM21 36v1h1v-1ZM22 63v1h1v-1ZM74 65v2h1v-2ZM44 72v1h1v-1ZM52 77v1h1v-1ZM68 78v1h1v-1ZM66 79v1h1v-1Z",
    0.325f to "M71 20v1h1v-1ZM31 26v1h1v-1ZM24 29v1h1v-1ZM21 59v1h1v-1ZM74 64v1h1v-1ZM24 66v1h1v-1ZM31 69v1h1v-1ZM59 80v1h1v-1ZM63 80v1h1v-1Z",
    0.349f to "M59 15v1h1v-1ZM66 16v1h1v-1ZM69 18v1h1v-1ZM70 19v1h1v-1ZM49 20v1h1v-1ZM32 26v1h2v-1ZM74 31v2h1v-2ZM74 63v1h1v-1ZM32 69v1h1v-1Z",
    0.353f to "M27 68v1h1v-1ZM41 70v1h1v-1ZM73 71v1h1v-1ZM46 73v1h1v-1ZM49 75v1h1v-1ZM71 75v1h1v-1ZM70 76v1h1v-1ZM69 77v1h1v-1ZM54 78v1h1v-1Z",
    0.373f to "M60 15v1h1v-1ZM62 15v1h1v-1ZM56 16v1h1v-1ZM54 17v1h1v-1ZM51 19v1h1v-1ZM46 22v1h1v-1ZM43 24v1h1v-1ZM41 25v1h1v-1Z",
    0.376f to "M72 22v1h1v-1ZM73 24v1h1v-1ZM74 33v1h1v-1ZM21 37v1h1v-1ZM21 58v1h1v-1ZM33 69v1h1v-1ZM62 80v1h1v-1Z",
    0.4f to "M21 38v20h1v5h1v2h1v1h1v1h1v1h2v1h6v1h8v1h1v1h2v1h2v1h1v1h2v1h1v1h2v1h2v1h1v1h4v1h2v-1h4v-1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-3h1v-8h1V34h-1v-9h-1v-2h-1v-2h-1v-1h-1v-1h-1v-1h-1v-1h-2v-1h-4v-1h-1v1h-4v1h-2v1h-2v1h-1v1h-2v1h-2v1h-1v1h-2v1h-1v1h-2v1h-8v1h-6v1h-2v1h-1v1h-1v1h-1v2h-1v5Z",
)

private const val VOLUME_TICKS_PATH =
    "M44 17v1h1v2h2v1h2v-1h2v-2h1v-1h-1v-2h-1v-1h-4v1h-1v2Z " +
            "M19 26v3h1v2h2v1h1v-1h2v-1h1v-4h-1v-1h-2v-1h-1v1h-2v1Z " +
            "M70 26v4h1v1h2v1h1v-1h2v-1h1v-4h-1v-1h-2v-1h-1v1h-2v1Z " +
            "M8 53v1h1v2h1v1h4v-1h1v-2h1v-1h-1v-2h-1v-1h-4v1H9v2Z " +
            "M80 53v1h1v2h1v1h4v-1h1v-2h1v-1h-1v-2h-1v-1h-4v1h-1v2Z " +
            "M19 77v3h1v1h1v1h4v-1h1v-4h-1v-1h-1v-1h-3v1h-1v1Z " +
            "M70 77v4h1v1h5v-2h1v-3h-1v-1h-1v-1h-3v1h-1v1Z"

private const val VOLUME_KNOB_PATH =
    "M48 52v1h1v1h1v1h2v-1h1v-1h1v-1h1v-1h1v-1h1v-2h1v-1h1v-3h-1v-1h-2v1h-2v2h-1v1h-1v1h-1v1h-1v1h-1v2h-1v1h-1v-4Z"

@Composable
internal fun VolumeMorphIcon(
    volume: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isMuted || volume <= 0.001f) "Muted" else "Volume ${(volume * 100).roundToInt()}%",
) {
    val targetVolume = if (isMuted) 0f else volume.coerceIn(0f, 1f)
    val animatedVolume by animateFloatAsState(
        targetValue = targetVolume,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "VolumeKnobRotation",
    )
    val muteProgress by animateFloatAsState(
        targetValue = if (isMuted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "VolumeMuteSlash",
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    val speakerPath = remember { parseVolumeIconPath(VOLUME_SPEAKER_PATH) }
    val ticksPath = remember { parseVolumeIconPath(VOLUME_TICKS_PATH) }
    val knobPath = remember { parseVolumeIconPath(VOLUME_KNOB_PATH) }

    Canvas(modifier = descModifier.size(size)) {
        drawVolumeMorph(
            volume = animatedVolume,
            muteProgress = muteProgress,
            tint = tint,
            speakerPath = speakerPath,
            ticksPath = ticksPath,
            knobPath = knobPath,
        )
    }
}

@Composable
internal fun VolumeMuteIcon(
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isMuted) "Unmute" else "Mute",
) {
    val muteProgress by animateFloatAsState(
        targetValue = if (isMuted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "VolumeMuteSlash",
    )
    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    val speakerLayers = remember {
        VOLUME_SPEAKER_PATH_LAYERS.map { (opacity, pathData) ->
            opacity to parseVolumeIconPath(pathData)
        }
    }
    val slicedSpeakerRegionPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawVolumeMuteIcon(
            muteProgress = muteProgress,
            tint = tint,
            speakerLayers = speakerLayers,
            slicedSpeakerRegionPath = slicedSpeakerRegionPath,
        )
    }
}

private fun parseVolumeIconPath(pathData: String): Path =
    PathParser().parsePathString(pathData).toNodes().toPath()

private fun DrawScope.drawVolumeMorph(
    volume: Float,
    muteProgress: Float,
    tint: Color,
    speakerPath: Path,
    ticksPath: Path,
    knobPath: Path,
) {
    val iconSize = min(size.width, size.height)
    val scale = iconSize / VOLUME_ICON_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f
    val knobRotation = -135f + 270f * volume.coerceIn(0f, 1f)

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
    }) {
        drawPath(path = speakerPath, color = tint)
        drawPath(path = ticksPath, color = tint)
        withTransform({ rotate(degrees = knobRotation, pivot = Offset(54f, 49f)) }) {
            drawPath(path = knobPath, color = tint)
        }

        val muteAmount = muteProgress.coerceIn(0f, 1f)
        if (muteAmount > 0.001f) {
            val slashStart = Offset(84f, 12f)
            val slashEnd = Offset(
                x = 84f + (13f - 84f) * muteAmount,
                y = 12f + (83f - 12f) * muteAmount,
            )
            drawLine(
                color = tint,
                start = slashStart,
                end = slashEnd,
                strokeWidth = VOLUME_MUTE_SLASH_WIDTH,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawVolumeMuteIcon(
    muteProgress: Float,
    tint: Color,
    speakerLayers: List<Pair<Float, Path>>,
    slicedSpeakerRegionPath: Path,
) {
    val iconSize = min(size.width, size.height)
    val scale = iconSize / VOLUME_ICON_VIEWBOX_SIZE
    val left = (size.width - iconSize) / 2f
    val top = (size.height - iconSize) / 2f

    withTransform({
        translate(left = left, top = top)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
    }) {
        val muteAmount = muteProgress.coerceIn(0f, 1f)
        speakerLayers.forEach { (opacity, path) ->
            drawPath(path = path, color = tint.copy(alpha = tint.alpha * opacity))
        }
        val speakerPath = speakerLayers.last().second

        if (muteAmount > 0.001f) {
            val slashStart = Offset(84f, 12f)
            val slashEnd = Offset(
                x = 84f + (13f - 84f) * muteAmount,
                y = 12f + (83f - 12f) * muteAmount,
            )
            val topSliceX = slashStart.x * (1f - muteAmount)
            slicedSpeakerRegionPath.reset()
            slicedSpeakerRegionPath.moveTo(slashStart.x, slashStart.y)
            slicedSpeakerRegionPath.lineTo(topSliceX, 0f)
            slicedSpeakerRegionPath.lineTo(0f, slashEnd.y)
            slicedSpeakerRegionPath.lineTo(slashEnd.x, slashEnd.y)
            slicedSpeakerRegionPath.close()

            clipPath(path = slicedSpeakerRegionPath) {
                drawPath(
                    path = speakerPath,
                    color = tint.copy(alpha = tint.alpha * muteAmount),
                )
            }
            drawLine(
                color = tint,
                start = slashStart,
                end = slashEnd,
                strokeWidth = VOLUME_MUTE_SLASH_WIDTH,
                cap = StrokeCap.Round,
            )
        }
    }
}
