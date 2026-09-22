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
fun LibraryMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Library",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LibraryMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val path = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawLibraryMorph(
            progress = progress,
            tint = tint,
            path = path
        )
    }
}

private fun DrawScope.drawLibraryMorph(
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

    path.moveTo(toCanvasX(500f), toCanvasY(-360f))
    path.quadraticTo(
        toCanvasX(542f), toCanvasY(-360f),
        toCanvasX(571f), toCanvasY(-389f)
    )
    path.quadraticTo(
        toCanvasX(600f), toCanvasY(-418f),
        toCanvasX(600f), toCanvasY(-460f)
    )
    path.lineTo(toCanvasX(600f), toCanvasY(-680f))
    path.lineTo(toCanvasX(680f), toCanvasY(-680f))
    path.quadraticTo(
        toCanvasX(697f), toCanvasY(-680f),
        toCanvasX(708.5f), toCanvasY(-691.5f)
    )
    path.quadraticTo(
        toCanvasX(720f), toCanvasY(-703f),
        toCanvasX(720f), toCanvasY(-720f)
    )
    path.quadraticTo(
        toCanvasX(720f), toCanvasY(-737f),
        toCanvasX(708.5f), toCanvasY(-748.5f)
    )
    path.quadraticTo(
        toCanvasX(697f), toCanvasY(-760f),
        toCanvasX(680f), toCanvasY(-760f)
    )
    path.lineTo(toCanvasX(600f), toCanvasY(-760f))
    path.quadraticTo(
        toCanvasX(583f), toCanvasY(-760f),
        toCanvasX(571.5f), toCanvasY(-748.5f)
    )
    path.quadraticTo(
        toCanvasX(560f), toCanvasY(-737f),
        toCanvasX(560f), toCanvasY(-720f)
    )
    path.lineTo(toCanvasX(560f), toCanvasY(-540f))
    path.quadraticTo(
        toCanvasX(547f), toCanvasY(-550f),
        toCanvasX(532f), toCanvasY(-555f)
    )
    path.quadraticTo(
        toCanvasX(517f), toCanvasY(-560f),
        toCanvasX(500f), toCanvasY(-560f)
    )
    path.quadraticTo(
        toCanvasX(458f), toCanvasY(-560f),
        toCanvasX(429f), toCanvasY(-531f)
    )
    path.quadraticTo(
        toCanvasX(400f), toCanvasY(-502f),
        toCanvasX(400f), toCanvasY(-460f)
    )
    path.quadraticTo(
        toCanvasX(400f), toCanvasY(-418f),
        toCanvasX(429f), toCanvasY(-389f)
    )
    path.quadraticTo(
        toCanvasX(458f), toCanvasY(-360f),
        toCanvasX(500f), toCanvasY(-360f)
    )
    path.close()

    path.moveTo(toCanvasX(320f), toCanvasY(-240f))
    path.quadraticTo(
        toCanvasX(287f), toCanvasY(-240f),
        toCanvasX(263.5f), toCanvasY(-263.5f)
    )
    path.quadraticTo(
        toCanvasX(240f), toCanvasY(-287f),
        toCanvasX(240f), toCanvasY(-320f)
    )
    path.lineTo(toCanvasX(240f), toCanvasY(-800f))
    path.quadraticTo(
        toCanvasX(240f), toCanvasY(-833f),
        toCanvasX(263.5f), toCanvasY(-856.5f)
    )
    path.quadraticTo(
        toCanvasX(287f), toCanvasY(-880f),
        toCanvasX(320f), toCanvasY(-880f)
    )
    path.lineTo(toCanvasX(800f), toCanvasY(-880f))
    path.quadraticTo(
        toCanvasX(833f), toCanvasY(-880f),
        toCanvasX(856.5f), toCanvasY(-856.5f)
    )
    path.quadraticTo(
        toCanvasX(880f), toCanvasY(-833f),
        toCanvasX(880f), toCanvasY(-800f)
    )
    path.lineTo(toCanvasX(880f), toCanvasY(-320f))
    path.quadraticTo(
        toCanvasX(880f), toCanvasY(-287f),
        toCanvasX(856.5f), toCanvasY(-263.5f)
    )
    path.quadraticTo(
        toCanvasX(833f), toCanvasY(-240f),
        toCanvasX(800f), toCanvasY(-240f)
    )
    path.lineTo(toCanvasX(320f), toCanvasY(-240f))
    path.close()

    path.moveTo(toCanvasX(160f), toCanvasY(-80f))
    path.quadraticTo(
        toCanvasX(127f), toCanvasY(-80f),
        toCanvasX(103.5f), toCanvasY(-103.5f)
    )
    path.quadraticTo(
        toCanvasX(80f), toCanvasY(-127f),
        toCanvasX(80f), toCanvasY(-160f)
    )
    path.lineTo(toCanvasX(80f), toCanvasY(-680f))
    path.quadraticTo(
        toCanvasX(80f), toCanvasY(-697f),
        toCanvasX(91.5f), toCanvasY(-708.5f)
    )
    path.quadraticTo(
        toCanvasX(103f), toCanvasY(-720f),
        toCanvasX(120f), toCanvasY(-720f)
    )
    path.quadraticTo(
        toCanvasX(137f), toCanvasY(-720f),
        toCanvasX(148.5f), toCanvasY(-708.5f)
    )
    path.quadraticTo(
        toCanvasX(160f), toCanvasY(-697f),
        toCanvasX(160f), toCanvasY(-680f)
    )
    path.lineTo(toCanvasX(160f), toCanvasY(-160f))
    path.lineTo(toCanvasX(680f), toCanvasY(-160f))
    path.quadraticTo(
        toCanvasX(697f), toCanvasY(-160f),
        toCanvasX(708.5f), toCanvasY(-148.5f)
    )
    path.quadraticTo(
        toCanvasX(720f), toCanvasY(-137f),
        toCanvasX(720f), toCanvasY(-120f)
    )
    path.quadraticTo(
        toCanvasX(720f), toCanvasY(-103f),
        toCanvasX(708.5f), toCanvasY(-91.5f)
    )
    path.quadraticTo(
        toCanvasX(697f), toCanvasY(-80f),
        toCanvasX(680f), toCanvasY(-80f)
    )
    path.lineTo(toCanvasX(160f), toCanvasY(-80f))
    path.close()

    val holeScale = (1f - p).coerceIn(0f, 1f)
    if (holeScale > 0.001f) {
        fun holeX(x: Float): Float = toCanvasX(560f + (x - 560f) * holeScale)
        fun holeY(y: Float): Float = toCanvasY(-560f + (y + 560f) * holeScale)

        path.moveTo(holeX(320f), holeY(-320f))
        path.lineTo(holeX(800f), holeY(-320f))
        path.lineTo(holeX(800f), holeY(-800f))
        path.lineTo(holeX(320f), holeY(-800f))
        path.close()
    }

    drawPath(path = path, color = tint)
}
