package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun PlayPauseMorphIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = if (isPlaying) "Pause" else "Play",
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PlayPauseMorphProgress"
    )

    val descModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = descModifier.size(size)) {
        drawPlayPauseMorph(
            progress = progress,
            tint = tint
        )
    }
}

private fun DrawScope.drawPlayPauseMorph(
    progress: Float,
    tint: Color,
) {
    val p = progress.coerceIn(0f, 1f)

    val scale = 1f - 0.07f * sin(p * PI.toFloat())

    val baseSize = min(size.width, size.height)
    val s = (baseSize / 24f) * scale
    val cx = size.width / 2f
    val cy = size.height / 2f

    fun toCanvas(x: Float, y: Float): Offset {
        return Offset(
            cx + (x - 12f) * s,
            cy + (y - 12f) * s
        )
    }

    val cornerRadius = lerp(3.4f * s, 2.0f * s, p)
    val tipRadius = lerp(3.6f * s, 2.0f * s, p)
    val seamRadius = lerp(0f, 2.0f * s, p)

    val bar1Vertices = listOf(
        Vertex(toCanvas(lerp(8.0f, 6.0f, p), lerp(5.0f, 5.0f, p)), cornerRadius),
        Vertex(toCanvas(lerp(13.0f, 10.0f, p), lerp(8.182f, 5.0f, p)), seamRadius),
        Vertex(toCanvas(lerp(13.0f, 10.0f, p), lerp(15.818f, 19.0f, p)), seamRadius),
        Vertex(toCanvas(lerp(8.0f, 6.0f, p), lerp(19.0f, 19.0f, p)), cornerRadius)
    )

    val bar2Vertices = listOf(
        Vertex(toCanvas(lerp(13.0f, 14.0f, p), lerp(8.182f, 5.0f, p)), seamRadius),
        Vertex(toCanvas(lerp(19.0f, 18.0f, p), lerp(12.0f, 5.0f, p)), tipRadius),
        Vertex(toCanvas(lerp(19.0f, 18.0f, p), lerp(12.0f, 19.0f, p)), tipRadius),
        Vertex(toCanvas(lerp(13.0f, 14.0f, p), lerp(15.818f, 19.0f, p)), seamRadius)
    )

    val path1 = Path().apply { addRoundedPolygon(bar1Vertices) }
    val path2 = Path().apply { addRoundedPolygon(bar2Vertices) }

    drawPath(path1, color = tint)
    drawPath(path2, color = tint)
}

private data class Vertex(val pos: Offset, val radius: Float)

private fun Path.addRoundedPolygon(vertices: List<Vertex>) {
    val filtered = mutableListOf<Vertex>()
    for (i in vertices.indices) {
        val curr = vertices[i]
        val next = vertices[(i + 1) % vertices.size]
        val dist = distance(curr.pos, next.pos)
        if (dist > 0.08f) {
            filtered.add(curr)
        }
    }
    if (filtered.size < 3) return

    val n = filtered.size
    for (i in 0 until n) {
        val prev = filtered[(i - 1 + n) % n]
        val curr = filtered[i]
        val next = filtered[(i + 1) % n]

        val dPrev = distance(prev.pos, curr.pos)
        val dNext = distance(next.pos, curr.pos)
        if (dPrev < 0.08f || dNext < 0.08f) continue

        val uPrevX = (prev.pos.x - curr.pos.x) / dPrev
        val uPrevY = (prev.pos.y - curr.pos.y) / dPrev
        val uNextX = (next.pos.x - curr.pos.x) / dNext
        val uNextY = (next.pos.y - curr.pos.y) / dNext

        val maxRPrev = if (prev.radius <= 0.01f) dPrev * 0.85f else dPrev * 0.48f
        val maxRNext = if (next.radius <= 0.01f) dNext * 0.85f else dNext * 0.48f
        val maxR = minOf(maxRPrev, maxRNext, curr.radius)

        val startX = curr.pos.x + uPrevX * maxR
        val startY = curr.pos.y + uPrevY * maxR
        val endX = curr.pos.x + uNextX * maxR
        val endY = curr.pos.y + uNextY * maxR

        if (i == 0) {
            moveTo(startX, startY)
        } else {
            lineTo(startX, startY)
        }

        if (maxR > 0.08f) {
            quadraticTo(curr.pos.x, curr.pos.y, endX, endY)
        } else {
            lineTo(curr.pos.x, curr.pos.y)
        }
    }
    close()
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
