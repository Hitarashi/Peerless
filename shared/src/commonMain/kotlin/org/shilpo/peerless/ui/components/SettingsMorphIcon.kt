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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * An expressive morphing Settings icon that smoothly transitions between
 * an outlined mechanical gear and a solid filled gear with spring dynamics and rotation.
 *
 * Modeled using exact Material Symbols vector path data and centered geometric aperture morphing.
 */
@Composable
fun SettingsMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Settings",
    animateRotation: Boolean = true,
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

    val solidGearPath = remember {
        PathParser().parsePathString(SETTINGS_SOLID_GEAR_PATH).toNodes().toPath()
    }
    val path = remember { Path() }
    val moatPath = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawSettingsMorph(
            progress = progress,
            tint = tint,
            solidGearPath = solidGearPath,
            path = path,
            moatPath = moatPath,
            animateRotation = animateRotation
        )
    }
}

private fun DrawScope.drawSettingsMorph(
    progress: Float,
    tint: Color,
    solidGearPath: Path,
    path: Path,
    moatPath: Path,
    animateRotation: Boolean,
) {
    val p = progress.coerceIn(0f, 1f)

    // Subtle elastic squash-and-stretch during state transition
    val elasticScale = 1f - 0.04f * sin(p * PI.toFloat())

    val baseSize = min(this.size.width, this.size.height)
    val s = baseSize / 960f
    val offsetX = (this.size.width - baseSize) / 2f
    val offsetY = (this.size.height - baseSize) / 2f

    path.reset()
    path.fillType = PathFillType.EvenOdd
    path.addPath(solidGearPath)

    // Moat cutout: hollows out the gear body when unselected (p=0), contracts shut when selected (p=1)
    val holeScale = (1f - p).coerceIn(0f, 1f)
    if (holeScale > 0.001f) {
        fun moatX(x: Float) = 482f + (x - 482f) * holeScale
        fun moatY(y: Float) = -480f + (y + 480f) * holeScale

        moatPath.reset()
        moatPath.moveTo(moatX(440f), moatY(-160f))
        moatPath.lineTo(moatX(519f), moatY(-160f))
        moatPath.lineTo(moatX(533f), moatY(-266f))
        moatPath.quadraticTo(
            moatX(564f), moatY(-274f),
            moatX(590.5f), moatY(-289.5f)
        )
        moatPath.quadraticTo(
            moatX(617f), moatY(-305f),
            moatX(639f), moatY(-327f)
        )
        moatPath.lineTo(moatX(738f), moatY(-286f))
        moatPath.lineTo(moatX(777f), moatY(-354f))
        moatPath.lineTo(moatX(691f), moatY(-419f))
        moatPath.quadraticTo(
            moatX(696f), moatY(-433f),
            moatX(698f), moatY(-448.5f)
        )
        moatPath.quadraticTo(
            moatX(700f), moatY(-464f),
            moatX(700f), moatY(-480f)
        )
        moatPath.quadraticTo(
            moatX(700f), moatY(-496f),
            moatX(698f), moatY(-511.5f)
        )
        moatPath.quadraticTo(
            moatX(696f), moatY(-527f),
            moatX(691f), moatY(-541f)
        )
        moatPath.lineTo(moatX(777f), moatY(-606f))
        moatPath.lineTo(moatX(738f), moatY(-674f))
        moatPath.lineTo(moatX(639f), moatY(-632f))
        moatPath.quadraticTo(
            moatX(617f), moatY(-655f),
            moatX(590.5f), moatY(-670.5f)
        )
        moatPath.quadraticTo(
            moatX(564f), moatY(-686f),
            moatX(533f), moatY(-694f)
        )
        moatPath.lineTo(moatX(520f), moatY(-800f))
        moatPath.lineTo(moatX(441f), moatY(-800f))
        moatPath.lineTo(moatX(427f), moatY(-694f))
        moatPath.quadraticTo(
            moatX(396f), moatY(-686f),
            moatX(369.5f), moatY(-670.5f)
        )
        moatPath.quadraticTo(
            moatX(343f), moatY(-655f),
            moatX(321f), moatY(-633f)
        )
        moatPath.lineTo(moatX(222f), moatY(-674f))
        moatPath.lineTo(moatX(183f), moatY(-606f))
        moatPath.lineTo(moatX(269f), moatY(-542f))
        moatPath.quadraticTo(
            moatX(264f), moatY(-527f),
            moatX(262f), moatY(-512f)
        )
        moatPath.quadraticTo(
            moatX(260f), moatY(-497f),
            moatX(260f), moatY(-480f)
        )
        moatPath.quadraticTo(
            moatX(260f), moatY(-464f),
            moatX(262f), moatY(-449f)
        )
        moatPath.quadraticTo(
            moatX(264f), moatY(-434f),
            moatX(269f), moatY(-419f)
        )
        moatPath.lineTo(moatX(183f), moatY(-354f))
        moatPath.lineTo(moatX(222f), moatY(-286f))
        moatPath.lineTo(moatX(321f), moatY(-328f))
        moatPath.quadraticTo(
            moatX(343f), moatY(-305f),
            moatX(369.5f), moatY(-289.5f)
        )
        moatPath.quadraticTo(
            moatX(396f), moatY(-274f),
            moatX(427f), moatY(-266f)
        )
        moatPath.lineTo(moatX(440f), moatY(-160f))
        moatPath.close()

        path.addPath(moatPath)
    }

    val rotation = if (animateRotation) 45f * p else 0f

    scale(scale = elasticScale, pivot = center) {
        rotate(degrees = rotation, pivot = center) {
            translate(left = offsetX, top = offsetY + 960f * s) {
                scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
                    drawPath(path = path, color = tint)
                }
            }
        }
    }
}

private const val SETTINGS_SOLID_GEAR_PATH =
    "M433-80q-27 0-46.5-18T363-142l-9-66q-13-5-24.5-12T307-235l-62 26q-25 11-50 2t-39-32l-47-82q-14-23-8-49t27-43l53-40q-1-7-1-13.5v-27q0-6.5 1-13.5l-53-40q-21-17-27-43t8-49l47-82q14-23 39-32t50 2l62 26q11-8 23-15t24-12l9-66q4-26 23.5-44t46.5-18h94q27 0 46.5 18t23.5 44l9 66q13 5 24.5 12t22.5 15l62-26q25-11 50-2t39 32l47 82q14 23 8 49t-27 43l-53 40q1 7 1 13.5v27q0 6.5-2 13.5l53 40q21 17 27 43t-8 49l-48 82q-14 23-39 32t-50-2l-60-26q-11 8-23 15t-24 12l-9 66q-4 26-23.5 44T527-80h-94Zm49-260q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Z"
