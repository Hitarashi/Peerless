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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * An expressive morphing Home icon that smoothly transitions between
 * an outlined house and a filled house with physics-based spring dynamics.
 *
 * Modeled using exact Material Symbols vector path data, continuous
 * outer contour interpolation, and centered geometric aperture morphing.
 */
@Composable
fun HomeMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Home",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "HomeMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val path = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawHomeMorph(
            progress = progress,
            tint = tint,
            path = path
        )
    }
}

private fun DrawScope.drawHomeMorph(
    progress: Float,
    tint: Color,
    path: Path,
) {
    val p = progress.coerceIn(0f, 1f)

    // Subtle elastic squash-and-stretch during state transition
    val elasticScale = 1f - 0.04f * sin(p * PI.toFloat())

    val baseSize = min(this.size.width, this.size.height)
    val s = (baseSize / 960f) * elasticScale
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f

    fun toCanvasX(x: Float): Float = cx + (x - 480f) * s
    fun toCanvasY(y: Float): Float = cy + (y + 480f) * s

    path.reset()
    path.fillType = PathFillType.EvenOdd

    // 1. Outer House Shell & Roof (shared geometry, with door morphing between p=0 and p=1)
    path.moveTo(toCanvasX(160f), toCanvasY(-200f))
    path.lineTo(toCanvasX(160f), toCanvasY(-560f))
    path.quadraticTo(
        toCanvasX(160f), toCanvasY(-579f),
        toCanvasX(168.5f), toCanvasY(-596f)
    )
    path.quadraticTo(
        toCanvasX(177f), toCanvasY(-613f),
        toCanvasX(192f), toCanvasY(-624f)
    )
    path.lineTo(toCanvasX(432f), toCanvasY(-804f))
    path.quadraticTo(
        toCanvasX(453f), toCanvasY(-820f),
        toCanvasX(480f), toCanvasY(-820f)
    )
    path.quadraticTo(
        toCanvasX(507f), toCanvasY(-820f),
        toCanvasX(528f), toCanvasY(-804f)
    )
    path.lineTo(toCanvasX(768f), toCanvasY(-624f))
    path.quadraticTo(
        toCanvasX(783f), toCanvasY(-613f),
        toCanvasX(791.5f), toCanvasY(-596f)
    )
    path.quadraticTo(
        toCanvasX(800f), toCanvasY(-579f),
        toCanvasX(800f), toCanvasY(-560f)
    )
    path.lineTo(toCanvasX(800f), toCanvasY(-200f))
    path.quadraticTo(
        toCanvasX(800f), toCanvasY(-167f),
        toCanvasX(776.5f), toCanvasY(-143.5f)
    )
    path.quadraticTo(
        toCanvasX(753f), toCanvasY(-120f),
        toCanvasX(720f), toCanvasY(-120f)
    )

    // Door transition (smoothly interpolating between outlined door p=0 and filled arched door p=1)
    path.lineTo(toCanvasX(lerp(560f, 600f, p)), toCanvasY(-120f))
    path.quadraticTo(
        toCanvasX(lerp(543f, 583f, p)), toCanvasY(-120f),
        toCanvasX(lerp(531.5f, 571.5f, p)), toCanvasY(-131.5f)
    )
    path.quadraticTo(
        toCanvasX(lerp(520f, 560f, p)), toCanvasY(-143f),
        toCanvasX(lerp(520f, 560f, p)), toCanvasY(-160f)
    )
    path.lineTo(toCanvasX(lerp(520f, 560f, p)), toCanvasY(-360f))
    path.quadraticTo(
        toCanvasX(lerp(520f, 560f, p)), toCanvasY(lerp(-360f, -377f, p)),
        toCanvasX(lerp(520f, 548.5f, p)), toCanvasY(lerp(-360f, -388.5f, p))
    )
    path.quadraticTo(
        toCanvasX(lerp(520f, 537f, p)), toCanvasY(lerp(-360f, -400f, p)),
        toCanvasX(520f), toCanvasY(lerp(-360f, -400f, p))
    )
    path.lineTo(toCanvasX(440f), toCanvasY(lerp(-360f, -400f, p)))
    path.quadraticTo(
        toCanvasX(lerp(440f, 423f, p)), toCanvasY(lerp(-360f, -400f, p)),
        toCanvasX(lerp(440f, 411.5f, p)), toCanvasY(lerp(-360f, -388.5f, p))
    )
    path.quadraticTo(
        toCanvasX(lerp(440f, 400f, p)), toCanvasY(lerp(-360f, -377f, p)),
        toCanvasX(lerp(440f, 400f, p)), toCanvasY(-360f)
    )
    path.lineTo(toCanvasX(lerp(440f, 400f, p)), toCanvasY(-160f))
    path.quadraticTo(
        toCanvasX(lerp(440f, 400f, p)), toCanvasY(-143f),
        toCanvasX(lerp(428.5f, 388.5f, p)), toCanvasY(-131.5f)
    )
    path.quadraticTo(
        toCanvasX(lerp(417f, 377f, p)), toCanvasY(-120f),
        toCanvasX(lerp(400f, 360f, p)), toCanvasY(-120f)
    )
    path.lineTo(toCanvasX(240f), toCanvasY(-120f))
    path.quadraticTo(
        toCanvasX(207f), toCanvasY(-120f),
        toCanvasX(183.5f), toCanvasY(-143.5f)
    )
    path.quadraticTo(
        toCanvasX(160f), toCanvasY(-167f),
        toCanvasX(160f), toCanvasY(-200f)
    )
    path.close()

    // 2. Inner Window Cutout: contracts smoothly toward center (480, -470) as p goes 0 -> 1
    val holeScale = (1f - p).coerceIn(0f, 1f)
    if (holeScale > 0.001f) {
        fun holeX(x: Float): Float = toCanvasX(480f + (x - 480f) * holeScale)
        fun holeY(y: Float): Float = toCanvasY(-470f + (y + 470f) * holeScale)

        path.moveTo(holeX(240f), holeY(-200f))
        path.lineTo(holeX(360f), holeY(-200f))
        path.lineTo(holeX(360f), holeY(-400f))
        path.quadraticTo(
            holeX(360f), holeY(-417f),
            holeX(371.5f), holeY(-428.5f)
        )
        path.quadraticTo(
            holeX(383f), holeY(-440f),
            holeX(400f), holeY(-440f)
        )
        path.lineTo(holeX(560f), holeY(-440f))
        path.quadraticTo(
            holeX(577f), holeY(-440f),
            holeX(588.5f), holeY(-428.5f)
        )
        path.quadraticTo(
            holeX(600f), holeY(-417f),
            holeX(600f), holeY(-400f)
        )
        path.lineTo(holeX(600f), holeY(-200f))
        path.lineTo(holeX(720f), holeY(-200f))
        path.lineTo(holeX(720f), holeY(-560f))
        path.lineTo(holeX(480f), holeY(-740f))
        path.lineTo(holeX(240f), holeY(-560f))
        path.lineTo(holeX(240f), holeY(-200f))
        path.close()
    }

    drawPath(path = path, color = tint)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
