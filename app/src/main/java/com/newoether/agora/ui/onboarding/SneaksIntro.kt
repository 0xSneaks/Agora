package com.newoether.agora.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newoether.agora.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * First onboarding page: the profile picture glitches in and out of focus, then slowly pulls
 * together; a 60s-style neon "Sneaks AI" sign flickers on once it settles. With reduced motion
 * the settled frame is shown immediately.
 */
@Composable
internal fun SneaksIntro(animate: Boolean, modifier: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(R.drawable.sneaks_pfp)
    var elapsedMs by remember { mutableLongStateOf(if (animate) 0L else SETTLED_MS) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsedMs = (it - start) / 1_000_000 }
    }
    val t = elapsedMs
    val glitch = glitchIntensity(t)
    // In and out of focus while glitching; sharp once assembled.
    val blurDp = (glitch * (6f + 6f * abs(sin(t / 140.0)).toFloat())).dp

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().blur(blurDp)) { drawGlitched(image, glitch, t) }
        NeonSign(
            alpha = neonAlpha(t),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
        )
    }
}

/** 1 = fully broken, 0 = clean. Chaotic for 1.8s, converges by 3.4s, then rare small twitches. */
internal fun glitchIntensity(t: Long): Float = when {
    t < CHAOS_MS -> 0.75f + 0.25f * Random(t / 70).nextFloat()
    t < SETTLED_MS -> {
        val p = (t - CHAOS_MS).toFloat() / (SETTLED_MS - CHAOS_MS)
        val ease = 1f - (1f - p) * (1f - p) * (1f - p)
        (1f - ease) * 0.75f
    }
    // Idle: a 160ms twitch roughly every 4s keeps it alive without hurting readability.
    (t - SETTLED_MS) % 4_000 < 160 -> 0.18f
    else -> 0f
}

/** Neon tubes buzz on with two misfires after the image settles, then stay lit. */
internal fun neonAlpha(t: Long): Float {
    val s = t - SETTLED_MS + 300
    return when {
        s < 0 -> 0f
        s < 90 -> 0.9f
        s < 220 -> 0.1f
        s < 300 -> 0.7f
        s < 420 -> 0.05f
        s < 900 -> 0.35f + 0.65f * ((s - 420) / 480f)
        else -> 1f
    }
}

private fun DrawScope.drawGlitched(image: ImageBitmap, glitch: Float, t: Long) {
    val side = minOf(size.width, size.height) * 0.78f
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    val iw = image.width
    val ih = image.height
    val bands = 24
    val rnd = Random(t / 55)
    val split = glitch * side * 0.045f
    drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
    for (b in 0 until bands) {
        val sy0 = ih * b / bands
        val sy1 = ih * (b + 1) / bands
        val dy0 = top + side * b / bands
        val dh = side / bands + 1f
        // Only some bands tear, and only while glitching.
        val tear = if (glitch > 0f && rnd.nextFloat() < glitch * 0.55f) {
            (rnd.nextFloat() * 2f - 1f) * glitch * side * 0.16f
        } else 0f
        for ((matrix, dx) in listOf(RED to -split, GREEN to 0f, BLUE to split)) {
            drawImage(
                image = image,
                srcOffset = IntOffset(0, sy0),
                srcSize = IntSize(iw, sy1 - sy0),
                dstOffset = IntOffset((left + tear + dx).roundToInt(), dy0.roundToInt()),
                dstSize = IntSize(side.roundToInt(), dh.roundToInt()),
                colorFilter = ColorFilter.colorMatrix(matrix),
                blendMode = BlendMode.Plus,
                alpha = if (glitch > 0.6f && rnd.nextFloat() < 0.08f) 0.25f else 1f,
            )
        }
    }
    drawContext.canvas.restore()
    // Scanline flicker while broken.
    if (glitch > 0.05f) {
        val lines = 6 + (glitch * 10).toInt()
        repeat(lines) {
            val y = top + rnd.nextFloat() * side
            drawLine(
                color = if (rnd.nextBoolean()) NEON_PINK else NEON_CYAN,
                start = Offset(left, y),
                end = Offset(left + side, y),
                strokeWidth = 1f + rnd.nextFloat() * 3f,
                alpha = glitch * 0.7f,
            )
        }
    }
}

@Composable
private fun NeonSign(alpha: Float, modifier: Modifier = Modifier) {
    if (alpha <= 0f) return
    val base = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
    )
    Box(modifier.alpha(alpha), contentAlignment = Alignment.Center) {
        // Wide outer glow, tight inner glow, then the hot white-pink tube core.
        Text("Sneaks AI", style = base.copy(color = NEON_PINK, shadow = Shadow(NEON_PINK, Offset.Zero, 48f)))
        Text("Sneaks AI", style = base.copy(color = NEON_PINK, shadow = Shadow(NEON_PINK, Offset.Zero, 18f)))
        Text("Sneaks AI", style = base.copy(color = TUBE_CORE, shadow = Shadow(Color.White, Offset.Zero, 4f)))
    }
}

private fun channel(r: Float, g: Float, b: Float) = ColorMatrix(
    floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

private val RED = channel(1f, 0f, 0f)
private val GREEN = channel(0f, 1f, 0f)
private val BLUE = channel(0f, 0f, 1f)
private val NEON_PINK = Color(0xFFFF2D95)
private val NEON_CYAN = Color(0xFF2DF6FF)
private val TUBE_CORE = Color(0xFFFFE3F1)
private const val CHAOS_MS = 1_800L
internal const val SETTLED_MS = 3_400L
