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
fun LyricsMorphIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = "Lyrics",
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LyricsMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    val path = remember { Path() }

    Canvas(modifier = descModifier.size(size)) {
        drawLyricsMorph(
            progress = progress,
            tint = tint,
            path = path
        )
    }
}

private fun DrawScope.drawLyricsMorph(
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

    fun tx(x: Float): Float = cx + (x - 480f) * s
    fun ty(y: Float): Float = cy + (y + 480f) * s

    path.reset()
    path.fillType = PathFillType.EvenOdd

    addRoundPill(path, 240f, -480f, 400f, -400f, 28.5f, ::tx, ::ty)

    addRoundPill(path, 240f, -600f, 520f, -520f, 28.5f, ::tx, ::ty)

    addRoundPill(path, 240f, -720f, 520f, -640f, 28.5f, ::tx, ::ty)

    path.moveTo(tx(760f), ty(-480f))
    path.quadraticTo(tx(710f), ty(-480f), tx(675f), ty(-515f))
    path.quadraticTo(tx(640f), ty(-550f), tx(640f), ty(-600f))
    path.quadraticTo(tx(640f), ty(-650f), tx(675f), ty(-685f))
    path.quadraticTo(tx(710f), ty(-720f), tx(760f), ty(-720f))
    path.quadraticTo(tx(771f), ty(-720f), tx(781f), ty(-718f))
    path.quadraticTo(tx(791f), ty(-716f), tx(800f), ty(-713f))
    path.lineTo(tx(800f), ty(-880f))
    path.quadraticTo(tx(800f), ty(-897f), tx(811.5f), ty(-908.5f))
    path.quadraticTo(tx(823f), ty(-920f), tx(840f), ty(-920f))
    path.lineTo(tx(920f), ty(-920f))
    path.quadraticTo(tx(937f), ty(-920f), tx(948.5f), ty(-908.5f))
    path.quadraticTo(tx(960f), ty(-897f), tx(960f), ty(-880f))
    path.quadraticTo(tx(960f), ty(-863f), tx(948.5f), ty(-851.5f))
    path.quadraticTo(tx(937f), ty(-840f), tx(920f), ty(-840f))
    path.lineTo(tx(880f), ty(-840f))
    path.lineTo(tx(880f), ty(-600f))
    path.quadraticTo(tx(880f), ty(-550f), tx(845f), ty(-515f))
    path.quadraticTo(tx(810f), ty(-480f), tx(760f), ty(-480f))
    path.close()

    path.moveTo(tx(240f), ty(-240f))
    path.lineTo(tx(148f), ty(-148f))
    path.quadraticTo(tx(142f), ty(-142f), tx(135f), ty(-139f))
    path.quadraticTo(tx(128f), ty(-136f), tx(120f), ty(-136f))
    path.quadraticTo(tx(104f), ty(-136f), tx(92f), ty(-147.5f))
    path.quadraticTo(tx(80f), ty(-159f), tx(80f), ty(-177f))
    path.lineTo(tx(80f), ty(-800f))
    path.quadraticTo(tx(80f), ty(-833f), tx(103.5f), ty(-856.5f))
    path.quadraticTo(tx(127f), ty(-880f), tx(160f), ty(-880f))
    path.lineTo(tx(600f), ty(-880f))
    path.quadraticTo(tx(631f), ty(-880f), tx(654.5f), ty(-861f))
    path.quadraticTo(tx(678f), ty(-842f), tx(678f), ty(-812f))
    path.quadraticTo(tx(678f), ty(-798f), tx(671.5f), ty(-787f))
    path.quadraticTo(tx(665f), ty(-776f), tx(654f), ty(-769f))
    path.quadraticTo(tx(610f), ty(-742f), tx(585f), ty(-697f))
    path.quadraticTo(tx(560f), ty(-652f), tx(560f), ty(-600f))
    path.quadraticTo(tx(560f), ty(-546f), tx(586.5f), ty(-500f))
    path.quadraticTo(tx(613f), ty(-454f), tx(660f), ty(-427f))
    path.quadraticTo(tx(682f), ty(-414f), tx(695f), ty(-392.5f))
    path.quadraticTo(tx(708f), ty(-371f), tx(708f), ty(-345f))
    path.quadraticTo(tx(708f), ty(-300f), tx(676.5f), ty(-270f))
    path.quadraticTo(tx(645f), ty(-240f), tx(600f), ty(-240f))
    path.lineTo(tx(240f), ty(-240f))
    path.close()

    val ap = (1f - p).coerceIn(0f, 1f)
    if (ap > 0.001f) {
        val holeCx = 360f
        val holeCy = -555f

        fun hx(x: Float): Float = tx(holeCx + (x - holeCx) * ap)
        fun hy(y: Float): Float = ty(holeCy + (y - holeCy) * ap)

        val cornerR = 20f * ap

        path.moveTo(hx(160f + cornerR), hy(-795f))
        path.lineTo(hx(550f - cornerR), hy(-795f))
        path.quadraticTo(hx(550f), hy(-795f), hx(550f), hy(-795f + cornerR))
        path.lineTo(hx(550f), hy(-320f - cornerR))
        path.quadraticTo(hx(550f), hy(-320f), hx(550f - cornerR), hy(-320f))
        path.lineTo(hx(205f), hy(-320f))
        path.lineTo(hx(160f), hy(-275f))
        path.lineTo(hx(160f), hy(-795f + cornerR))
        path.quadraticTo(hx(160f), hy(-795f), hx(160f + cornerR), hy(-795f))
        path.close()
    }

    drawPath(path = path, color = tint)
}

private fun addRoundPill(
    path: Path,
    left: Float, top: Float, right: Float, bottom: Float,
    r: Float,
    tx: (Float) -> Float,
    ty: (Float) -> Float,
) {
    path.moveTo(tx(left + r), ty(top))
    path.lineTo(tx(right - r), ty(top))
    path.quadraticTo(tx(right), ty(top), tx(right), ty(top + r))
    path.lineTo(tx(right), ty(bottom - r))
    path.quadraticTo(tx(right), ty(bottom), tx(right - r), ty(bottom))
    path.lineTo(tx(left + r), ty(bottom))
    path.quadraticTo(tx(left), ty(bottom), tx(left), ty(bottom - r))
    path.lineTo(tx(left), ty(top + r))
    path.quadraticTo(tx(left), ty(top), tx(left + r), ty(top))
    path.close()
}
