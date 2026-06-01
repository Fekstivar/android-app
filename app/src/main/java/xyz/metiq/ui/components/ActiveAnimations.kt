package xyz.metiq.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WaveRings(
    color: Color,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = diameter.toPx() / 2f
        val ringCount = 3
        repeat(ringCount) { i ->
            val p = (phase + i.toFloat() / ringCount) % 1f
            val scale = 1f + p * 0.6f
            val alpha = (1f - p) * 0.9f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = baseRadius * scale,
                center = center,
            )
        }
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    val radiusDp: Float,
    val alpha: Float,
    val speed: Float,
)

private fun newParticle(rng: Random): Particle = Particle(
    x = rng.nextFloat(),
    y = rng.nextFloat(),
    radiusDp = 1.2f + rng.nextFloat() * 1.2f,
    alpha = 0.2f + rng.nextFloat() * 0.4f,
    speed = 0.012f + rng.nextFloat() * 0.018f,
)

private fun flowAngle(x: Float, y: Float, t: Float): Float {
    val a = sin(x * 1.7f + t * 0.14f) + cos(y * 1.3f - t * 0.09f)
    val b = cos(x * 1.0f - t * 0.07f) + sin(y * 2.1f + t * 0.16f)
    return (a + b) * PI.toFloat() * 0.5f
}

private fun Particle.update(dt: Float, t: Float) {
    val angle = flowAngle(x, y, t)
    x = ((x + cos(angle) * speed * dt) % 1f + 1f) % 1f
    y = ((y + sin(angle) * speed * dt) % 1f + 1f) % 1f
}

@Composable
fun ParticleField(
    color: Color,
    modifier: Modifier = Modifier,
    count: Int = 90,
) {
    val rng = remember { Random(0xC0FFEE) }
    val particles = remember(count) { List(count) { newParticle(rng) } }
    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        var elapsed = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                elapsed += dt
                particles.forEach { it.update(dt, elapsed) }
                frameTime = now
            }
        }
    }
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION") frameTime
        particles.forEach { p ->
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.radiusDp.dp.toPx(),
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
    }
}
