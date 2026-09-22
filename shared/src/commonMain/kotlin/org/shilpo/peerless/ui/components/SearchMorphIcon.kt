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

@Composable
fun SearchMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Search",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SearchMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val path = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawSearchMorph(
            progress = progress,
            tint = tint,
            path = path
        )
    }
}

private fun DrawScope.drawSearchMorph(
    progress: Float,
    tint: Color,
    path: Path,
) {
    val p = progress.coerceIn(0f, 1f)

    val elasticScale = 1f - 0.04f * sin(p * PI.toFloat())

    val baseSize = min(this.size.width, this.size.height)
    val s = (baseSize / 960f) * elasticScale
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f

    fun toCanvasX(x: Float): Float = cx + (x - 480f) * s
    fun toCanvasY(y: Float): Float = cy + (y + 480f) * s

    path.reset()
    path.fillType = PathFillType.EvenOdd

    path.moveTo(toCanvasX(380f), toCanvasY(-320f))
    path.quadraticTo(
        toCanvasX(271f), toCanvasY(-320f),
        toCanvasX(195.5f), toCanvasY(-395.5f)
    )
    path.quadraticTo(
        toCanvasX(120f), toCanvasY(-471f),
        toCanvasX(120f), toCanvasY(-580f)
    )
    path.quadraticTo(
        toCanvasX(120f), toCanvasY(-689f),
        toCanvasX(195.5f), toCanvasY(-764.5f)
    )
    path.quadraticTo(
        toCanvasX(271f), toCanvasY(-840f),
        toCanvasX(380f), toCanvasY(-840f)
    )
    path.quadraticTo(
        toCanvasX(489f), toCanvasY(-840f),
        toCanvasX(564.5f), toCanvasY(-764.5f)
    )
    path.quadraticTo(
        toCanvasX(640f), toCanvasY(-689f),
        toCanvasX(640f), toCanvasY(-580f)
    )
    path.quadraticTo(
        toCanvasX(640f), toCanvasY(-536f),
        toCanvasX(626f), toCanvasY(-497f)
    )
    path.quadraticTo(
        toCanvasX(612f), toCanvasY(-458f),
        toCanvasX(588f), toCanvasY(-428f)
    )
    path.lineTo(toCanvasX(812f), toCanvasY(-204f))
    path.quadraticTo(
        toCanvasX(823f), toCanvasY(-193f),
        toCanvasX(823f), toCanvasY(-176f)
    )
    path.quadraticTo(
        toCanvasX(823f), toCanvasY(-159f),
        toCanvasX(812f), toCanvasY(-148f)
    )
    path.quadraticTo(
        toCanvasX(801f), toCanvasY(-137f),
        toCanvasX(784f), toCanvasY(-137f)
    )
    path.quadraticTo(
        toCanvasX(767f), toCanvasY(-137f),
        toCanvasX(756f), toCanvasY(-148f)
    )
    path.lineTo(toCanvasX(532f), toCanvasY(-372f))
    path.quadraticTo(
        toCanvasX(502f), toCanvasY(-348f),
        toCanvasX(463f), toCanvasY(-334f)
    )
    path.quadraticTo(
        toCanvasX(424f), toCanvasY(-320f),
        toCanvasX(380f), toCanvasY(-320f)
    )
    path.close()

    val holeScale = (1f - p).coerceIn(0f, 1f)
    if (holeScale > 0.001f) {
        fun holeX(x: Float): Float = toCanvasX(380f + (x - 380f) * holeScale)
        fun holeY(y: Float): Float = toCanvasY(-580f + (y + 580f) * holeScale)

        path.moveTo(holeX(380f), holeY(-400f))
        path.quadraticTo(
            holeX(455f), holeY(-400f),
            holeX(507.5f), holeY(-452.5f)
        )
        path.quadraticTo(
            holeX(560f), holeY(-505f),
            holeX(560f), holeY(-580f)
        )
        path.quadraticTo(
            holeX(560f), holeY(-655f),
            holeX(507.5f), holeY(-707.5f)
        )
        path.quadraticTo(
            holeX(455f), holeY(-760f),
            holeX(380f), holeY(-760f)
        )
        path.quadraticTo(
            holeX(305f), holeY(-760f),
            holeX(252.5f), holeY(-707.5f)
        )
        path.quadraticTo(
            holeX(200f), holeY(-655f),
            holeX(200f), holeY(-580f)
        )
        path.quadraticTo(
            holeX(200f), holeY(-505f),
            holeX(252.5f), holeY(-452.5f)
        )
        path.quadraticTo(
            holeX(305f), holeY(-400f),
            holeX(380f), holeY(-400f)
        )
        path.close()
    }

    drawPath(path = path, color = tint)
}
