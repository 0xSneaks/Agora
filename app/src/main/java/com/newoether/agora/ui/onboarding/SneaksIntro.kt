package com.newoether.agora.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.newoether.agora.R
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 0x Sneaky Alpha loading screen.
 *
 * The portrait glitches in (RGB split, torn bands, scanlines) while a neon bar fills to 100%,
 * locking clean at ~75%. The bar then fades and the baked-in "OxSneakyAlpha" neon sign at the
 * bottom of the art flickers on like a real tube. Reduced motion shows the finished frame.
 */
@Composable
internal fun SneaksIntro(animate: Boolean, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.sneaky_alpha_art)
    val measurer = rememberTextMeasurer()
    var elapsedMs by remember { mutableLongStateOf(if (animate) 0L else LIT_MS) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsedMs = (it - start) / 1_000_000 }
    }
    val t = elapsedMs
    Canvas(modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height)
        val frame = Rect(Offset((size.width - side) / 2f, (size.height - side) / 2f), Size(side, side))
        val p = loadProgress(t)
        drawPortrait(art, frame, glitchIntensity(t, p), t)
        val uiAlpha = loadingUiAlpha(t)
        if (uiAlpha > 0f) drawLoadingUi(measurer, frame, p, uiAlpha)
        val sign = tubeAlpha(t - SIGN_START_MS)
        if (sign > 0f) drawSign(art, frame, sign)
    }
}

/** 0..1 bar fill: quick start, a stall near 70% like a real loader, then a finish. */
internal fun loadProgress(t: Long): Float {
    val x = (t.toFloat() / LOAD_MS).coerceIn(0f, 1f)
    val eased = when {
        x < 0.55f -> 0.70f * (1f - (1f - x / 0.55f) * (1f - x / 0.55f))
        x < 0.70f -> 0.70f + 0.04f * ((x - 0.55f) / 0.15f)
        else -> 0.74f + 0.26f * ((x - 0.70f) / 0.30f)
    }
    return eased.coerceIn(0f, 1f)
}

/** Heavy glitch early, clean by 75% load; afterwards a rare small twitch keeps it alive. */
internal fun glitchIntensity(t: Long, progress: Float): Float {
    if (progress < 0.75f) {
        val base = 1f - progress / 0.75f
        val spike = if (Random(t / 90).nextFloat() < 0.25f) 0.3f else 0f
        return (base * 0.85f + spike * base).coerceIn(0f, 1f)
    }
    return if (t > LIT_MS && (t - LIT_MS) % 4_500 < 140) 0.15f else 0f
}

internal fun loadingUiAlpha(t: Long): Float = when {
    t < LOAD_MS -> 1f
    t < LOAD_MS + 300 -> 1f - (t - LOAD_MS) / 300f
    else -> 0f
}

/** Neon tube strike: two misfires, warm-up, then lit with a faint hum. `s` is ms since strike. */
internal fun tubeAlpha(s: Long): Float = when {
    s < 0 -> 0f
    s < 70 -> 0.9f
    s < 210 -> 0.05f
    s < 280 -> 0.75f
    s < 430 -> 0.1f
    s < 900 -> 0.35f + 0.65f * ((s - 430) / 470f)
    (s % 5_200) in 0..60 -> 0.7f
    else -> 0.94f + 0.06f * ((s / 40) % 2)
}

private fun DrawScope.drawPortrait(art: ImageBitmap, frame: Rect, glitch: Float, t: Long) {
    val splitY = art.height * SIGN_TOP
    val bands = 28
    val rnd = Random(t / 55)
    val shift = glitch * frame.width * 0.04f
    drawContext.canvas.saveLayer(frame, Paint())
    for (b in 0 until bands) {
        val sy0 = (splitY * b / bands).toInt()
        val sy1 = (splitY * (b + 1) / bands).toInt()
        val dy0 = frame.top + frame.height * sy0 / art.height
        val dh = frame.height * (sy1 - sy0) / art.height + 1f
        val tear = if (glitch > 0f && rnd.nextFloat() < glitch * 0.6f) {
            (rnd.nextFloat() * 2f - 1f) * glitch * frame.width * 0.18f
        } else 0f
        val dropout = glitch > 0.5f && rnd.nextFloat() < 0.07f
        for ((matrix, dx) in listOf(RED to -shift, GREEN to 0f, BLUE to shift)) {
            drawImage(
                image = art,
                srcOffset = IntOffset(0, sy0),
                srcSize = IntSize(art.width, sy1 - sy0),
                dstOffset = IntOffset((frame.left + tear + dx).roundToInt(), dy0.roundToInt()),
                dstSize = IntSize(frame.width.roundToInt(), dh.roundToInt()),
                colorFilter = ColorFilter.colorMatrix(matrix),
                blendMode = BlendMode.Plus,
                alpha = if (dropout) 0.2f else 1f,
            )
        }
    }
    drawContext.canvas.restore()
    if (glitch > 0.05f) {
        repeat(5 + (glitch * 12).toInt()) {
            val y = frame.top + rnd.nextFloat() * frame.height * SIGN_TOP
            drawLine(
                color = if (rnd.nextBoolean()) NEON_PINK else NEON_CYAN,
                start = Offset(frame.left, y),
                end = Offset(frame.right, y),
                strokeWidth = 1f + rnd.nextFloat() * 3f,
                alpha = glitch * 0.6f,
            )
        }
    }
}

/** The baked "OxSneakyAlpha" sign from the art, revealed with tube flicker. */
private fun DrawScope.drawSign(art: ImageBitmap, frame: Rect, alpha: Float) {
    val sy = (art.height * SIGN_TOP).toInt()
    val dy = frame.top + frame.height * SIGN_TOP
    clipRect(frame.left, dy, frame.right, frame.bottom) {
        drawImage(
            image = art,
            srcOffset = IntOffset(0, sy),
            srcSize = IntSize(art.width, art.height - sy),
            dstOffset = IntOffset(frame.left.roundToInt(), dy.roundToInt()),
            dstSize = IntSize(frame.width.roundToInt(), (frame.bottom - dy).roundToInt()),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawLoadingUi(measurer: TextMeasurer, frame: Rect, p: Float, alpha: Float) {
    val u = frame.width / 1024f // art pixels -> screen
    // "Loading... 73%" line.
    val title = measurer.measure(
        "Loading... ${(p * 100).toInt()}%",
        TextStyle(
            fontFamily = FontFamily.Cursive, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
            fontSize = (54f * u).toSp(), color = PINK_CORE, shadow = Shadow(NEON_PINK, Offset.Zero, 22f * u),
        ),
    )
    drawText(title, topLeft = Offset(frame.center.x - title.size.width / 2f, frame.top + 755f * u), alpha = alpha)
    // Bar frame with a soft cyan glow.
    val bar = Rect(frame.left + 82f * u, frame.top + 838f * u, frame.right - 82f * u, frame.top + 906f * u)
    val radius = CornerRadius(bar.height / 2f)
    drawRoundRect(NEON_CYAN, bar.topLeft, bar.size, radius, style = Stroke(14f * u), alpha = 0.25f * alpha)
    drawRoundRect(NEON_CYAN, bar.topLeft, bar.size, radius, style = Stroke(4f * u), alpha = alpha)
    // Segments: pink body, the leading few turn cyan like the head of the stream.
    val segments = 24
    val pad = 16f * u
    val gap = 7f * u
    val segW = (bar.width - pad * 2 - gap * (segments - 1)) / segments
    val filled = (p * segments).toInt()
    for (i in 0 until filled) {
        val x = bar.left + pad + i * (segW + gap)
        val color = if (i >= filled - 4 && p < 1f) NEON_CYAN else NEON_PINK
        drawRoundRect(
            color, Offset(x, bar.top + 14f * u), Size(segW, bar.height - 28f * u),
            CornerRadius(4f * u), alpha = alpha,
        )
    }
    val sub = measurer.measure(
        "Initializing data stream...",
        TextStyle(
            fontFamily = FontFamily.Cursive, fontStyle = FontStyle.Italic,
            fontSize = (34f * u).toSp(), color = PINK_CORE, shadow = Shadow(NEON_PINK, Offset.Zero, 12f * u),
        ),
    )
    drawText(sub, topLeft = Offset(frame.center.x - sub.size.width / 2f, frame.top + 925f * u), alpha = alpha)
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
private val PINK_CORE = Color(0xFFFFE3F1)
private val NEON_CYAN = Color(0xFF2DF6FF)

/** Fraction of the art's height where the baked neon sign begins. */
private const val SIGN_TOP = 785f / 1024f
private const val LOAD_MS = 2_400L
private const val SIGN_START_MS = LOAD_MS + 150L
/** The sign is fully lit by this point. */
internal const val LIT_MS = SIGN_START_MS + 900L
