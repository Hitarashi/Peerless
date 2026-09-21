package org.shilpo.peerless.ui.components.lyrics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * State and physics simulation for a single floating sparkle particle.
 * Directly translated from XMusic's Sparkle system.
 */
internal class Sparkle(
    var x: Float = 0f,
    var y: Float = 0f,
    var baseX: Float = 0f,
    var baseY: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var alpha: Float = 0f,
    var maxAlpha: Float = 1f,
    var size: Float = 2f,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var phase: Float = 0f,
    var amplitude: Float = 2f,
    var frequency: Float = 8f
)

/**
 * High-performance sparkle particle emitter and simulation system for active singing lines.
 */
internal class SparkleParticleEmitter(
    private val maxParticles: Int = 45
) {
    private val particles = ArrayList<Sparkle>(maxParticles)
    private val random = Random(System.currentTimeMillis())

    fun spawn(headX: Float, topY: Float, bottomY: Float, density: Float) {
        if (particles.size >= maxParticles) return

        val s = Sparkle()
        s.baseX = headX - (random.nextFloat() * 4f * density)
        val h = bottomY - topY
        s.baseY = topY + (h * 0.125f) + (random.nextFloat() * (h * 0.75f))
        s.vx = (random.nextFloat() - 0.5f) * 8f * density
        s.vy = -(random.nextFloat() * 10f * density + 4f * density)
        s.maxLife = 0.4f + random.nextFloat() * 0.6f
        s.life = s.maxLife
        s.maxAlpha = 0.6f + random.nextFloat() * 0.4f
        s.alpha = s.maxAlpha
        s.size = (0.8f + random.nextFloat() * 1.4f) * density
        s.phase = random.nextFloat() * (PI.toFloat() * 2f)
        s.amplitude = (1.5f + random.nextFloat() * 1.5f) * density
        s.frequency = 6f + random.nextFloat() * 8f
        s.x = s.baseX
        s.y = s.baseY

        particles.add(s)
    }

    fun update(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        var i = 0
        while (i < particles.size) {
            val s = particles[i]
            s.life -= dt
            if (s.life <= 0f) {
                particles.removeAt(i)
                continue
            }

            val p = 1.0f - (s.life / s.maxLife)
            s.alpha = (s.maxAlpha * (1.0f - p * p)).coerceIn(0f, 1f)
            s.baseX += s.vx * dt
            s.baseY += s.vy * dt
            s.x = s.baseX + sin(s.phase + p * s.frequency) * s.amplitude
            s.y = s.baseY
            i++
        }
    }

    fun draw(drawScope: DrawScope, color: Color) {
        if (particles.isEmpty()) return

        for (s in particles) {
            val particleAlpha = (s.alpha * color.alpha).coerceIn(0f, 1f)
            if (particleAlpha <= 0.01f) continue

            val particleColor = color.copy(alpha = particleAlpha)
            drawScope.drawCircle(
                color = particleColor,
                radius = s.size,
                center = Offset(s.x, s.y)
            )

            // Draw a subtle diamond cross sparkle glint for larger particles
            if (s.size > 2.2f) {
                val glintLength = s.size * 2f
                val glintAlpha = (particleAlpha * 0.7f).coerceIn(0f, 1f)
                val glintColor = color.copy(alpha = glintAlpha)
                drawScope.drawLine(
                    color = glintColor,
                    start = Offset(s.x - glintLength, s.y),
                    end = Offset(s.x + glintLength, s.y),
                    strokeWidth = 1f
                )
                drawScope.drawLine(
                    color = glintColor,
                    start = Offset(s.x, s.y - glintLength),
                    end = Offset(s.x, s.y + glintLength),
                    strokeWidth = 1f
                )
            }
        }
    }

    fun clear() {
        particles.clear()
    }
}
