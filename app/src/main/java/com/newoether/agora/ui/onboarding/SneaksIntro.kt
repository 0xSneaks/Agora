package com.newoether.agora.ui.onboarding

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newoether.agora.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * First onboarding beat: the profile picture tears itself apart (channel split, sliced
 * bands, pulsing defocus, pink/cyan scanlines), eases back to a sharp portrait, then a
 * script neon "Sneaks AI" sign strikes like a tired tube. Reduced motion skips the clock
 * and shows that settled frame with the sign fully lit.
 */
@Composable
internal fun SneaksIntro(animate: Boolean, modifier: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(R.drawable.sneaks_pfp)
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame -> elapsedMs = (frame - start) / 1_000_000L }
        }
    }
    // Reduced motion must not sample the timeline: t == 0 is mid-glitch, and the first
    // idle instant is not a lit sign yet.
    val glitch = if (animate) glitchIntensity(elapsedMs) else 0f
    val neon = if (animate) neonAlpha(elapsedMs) else 1f
    val blurDp = if (animate && glitch > 0.02f && supportsHardwareBlur()) {
        val pulse = (sin(elapsedMs / 155.0) * 0.5 + 0.5).toFloat()
        (glitch * (2.2f + 8.4f * pulse)).dp
    } else {
        0.dp
    }
    val portraitMod = if (blurDp > 0.5.dp) {
        Modifier.blur(blurDp, BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val signSp = (maxWidth.value * 0.105f).coerceIn(32f, 50f).sp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().then(portraitMod)) {
                    drawPortrait(image, glitch, if (animate) elapsedMs else 0L)
                }
                // Scanlines stay sharp over the defocused portrait so the tear still reads.
                if (glitch > 0.04f) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawScanlines(glitch, elapsedMs)
                    }
                }
            }
            // Reserved whether or not the tube has struck, so the portrait doesn't jump.
            Box(
                Modifier.fillMaxWidth().height(92.dp).padding(bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (neon > 0.01f) NeonSign(strength = neon, fontSizeSp = signSp.value)
            }
        }
    }
}

/**
 * 1 = fully broken, 0 = clean.
 * Chaotic for 1.8s (breathing in and out of focus), eases out over the next 1.6s,
 * then a short readable twitch a few seconds apart.
 */
internal fun glitchIntensity(t: Long): Float = when {
    t < CHAOS_MS -> {
        val breath = (sin(t / 145.0) * 0.5 + 0.5).toFloat()
        val grit = Random(t / 55).nextFloat()
        val raw = 0.16f + 0.84f * (breath * 0.7f + grit * 0.3f)
        // Land on a known level so the assemble ease doesn't pop.
        val handoff = ((t - (CHAOS_MS - 200)).toFloat() / 200f).coerceIn(0f, 1f)
        raw * (1f - handoff) + ASSEMBLE_FROM * handoff
    }
    t < SETTLED_MS -> {
        val p = (t - CHAOS_MS).toFloat() / (SETTLED_MS - CHAOS_MS)
        val easeOut = 1f - (1f - p) * (1f - p) * (1f - p)
        (1f - easeOut) * ASSEMBLE_FROM
    }
    else -> {
        val phase = (t - SETTLED_MS) % IDLE_CYCLE_MS
        if (phase >= IDLE_TWITCH_MS) {
            0f
        } else {
            val u = phase.toFloat() / IDLE_TWITCH_MS
            IDLE_PEAK * sin(u * PI.toFloat())
        }
    }
}

/**
 * Tube strike after the portrait has assembled: two misfires, a weak catch, then a
 * steady glow. Returns 0 until [SETTLED_MS].
 */
internal fun neonAlpha(t: Long): Float {
    val s = t - SETTLED_MS
    val base = when {
        s < 70 -> 0f
        s < 150 -> 0.95f
        s < 280 -> 0f
        s < 360 -> 0.5f
        s < 490 -> 0.04f
        s < 580 -> 0.9f
        s < 700 -> 0.22f
        s < 1_250 -> {
            val p = (s - 700).toFloat() / 550f
            val eased = 1f - (1f - p) * (1f - p)
            0.22f + 0.78f * eased
        }
        else -> 1f
    }
    // Buzz only while the tube is striking. Once it holds, the glow stays steady.
    if (s in 70 until 1_250 && base > 0.08f && base < 0.99f) {
        val buzz = 0.7f + 0.3f * abs(sin(s / 24.0)).toFloat()
        return base * buzz
    }
    return base
}

private fun DrawScope.drawPortrait(image: ImageBitmap, glitch: Float, t: Long) {
    val frame = portraitFrame() ?: return
    if (glitch < 0.02f) {
        blit(image, frame, 0f, 1f, null, BlendMode.SrcOver)
        return
    }
    val rnd = Random(t / 42)
    val split = glitch * frame.side * 0.042f * if ((t / 90) % 2L == 0L) 1f else -1f
    // Presence dips make the portrait snap in and out while the channels are torn.
    val presence = when {
        glitch > 0.62f && rnd.nextFloat() < 0.09f -> 0.28f
        glitch > 0.45f && rnd.nextFloat() < 0.12f -> 0.62f
        else -> 1f
    }
    // Software stand-in where RenderEffect blur is unavailable (API 26–30).
    if (!supportsHardwareBlur()) {
        val smear = glitch * frame.side * 0.012f * (0.4f + 0.6f * abs(sin(t / 155.0)).toFloat())
        val ghost = 0.16f * glitch * presence
        blit(image, frame, -smear, ghost, null, BlendMode.SrcOver)
        blit(image, frame, smear, ghost, null, BlendMode.SrcOver)
        blit(image, frame, 0f, ghost, null, BlendMode.SrcOver, dy = smear)
        blit(image, frame, 0f, ghost, null, BlendMode.SrcOver, dy = -smear)
    }
    drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
    blit(image, frame, -split, presence, RED_FILTER, BlendMode.Plus)
    blit(image, frame, 0f, presence, GREEN_FILTER, BlendMode.Plus)
    blit(image, frame, split, presence, BLUE_FILTER, BlendMode.Plus)
    drawContext.canvas.restore()

    val tears = if (glitch < 0.28f) 2 else 3 + (glitch * 5f).toInt()
    val bands = 12
    val ih = image.height
    val iw = image.width
    repeat(tears) {
        if (rnd.nextFloat() > glitch.coerceIn(0.15f, 1f)) return@repeat
        val band = rnd.nextInt(bands)
        val sy0 = ih * band / bands
        val sy1 = ih * (band + 1) / bands
        val sh = sy1 - sy0
        if (sh <= 0) return@repeat
        val dy = frame.top + frame.side * band / bands.toFloat()
        val dh = frame.side / bands.toFloat() + 1f
        val shift = (rnd.nextFloat() * 2f - 1f) * glitch * frame.side * 0.13f
        val dropped = rnd.nextFloat() < glitch * 0.18f
        drawImage(
            image = image,
            srcOffset = IntOffset(0, sy0),
            srcSize = IntSize(iw, sh),
            dstOffset = IntOffset((frame.left + shift).roundToInt(), dy.roundToInt()),
            dstSize = IntSize(frame.side.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
            alpha = if (dropped) 0.12f else 0.94f,
        )
    }
}

private fun DrawScope.drawScanlines(glitch: Float, t: Long) {
    val frame = portraitFrame() ?: return
    val rnd = Random(t / 37 + 17)
    val lines = 5 + (glitch * 11f).toInt()
    repeat(lines) {
        val y = frame.top + rnd.nextFloat() * frame.side
        val pink = rnd.nextBoolean()
        drawLine(
            color = if (pink) NEON_PINK else NEON_CYAN,
            start = Offset(frame.left, y),
            end = Offset(frame.left + frame.side, y),
            strokeWidth = 1f + rnd.nextFloat() * (1.5f + glitch * 3.5f),
            alpha = glitch * (0.28f + rnd.nextFloat() * 0.55f),
        )
    }
    if (glitch > 0.35f && rnd.nextFloat() < glitch * 0.65f) {
        val y = frame.top + rnd.nextFloat() * frame.side
        drawLine(
            color = if (rnd.nextBoolean()) NEON_PINK else NEON_CYAN,
            start = Offset(frame.left, y),
            end = Offset(frame.left + frame.side * (0.45f + rnd.nextFloat() * 0.55f), y),
            strokeWidth = 3f + rnd.nextFloat() * 5f,
            alpha = 0.45f * glitch,
        )
    }
}

private fun DrawScope.blit(
    image: ImageBitmap,
    frame: PortraitFrame,
    dx: Float,
    alpha: Float,
    filter: ColorFilter?,
    blend: BlendMode,
    dy: Float = 0f,
) {
    val side = frame.side.roundToInt().coerceAtLeast(1)
    drawImage(
        image = image,
        dstOffset = IntOffset((frame.left + dx).roundToInt(), (frame.top + dy).roundToInt()),
        dstSize = IntSize(side, side),
        alpha = alpha.coerceIn(0f, 1f),
        colorFilter = filter,
        blendMode = blend,
    )
}

private data class PortraitFrame(val left: Float, val top: Float, val side: Float)

private fun DrawScope.portraitFrame(): PortraitFrame? {
    if (size.minDimension < 2f) return null
    val side = size.minDimension * 0.86f
    return PortraitFrame(
        left = (size.width - side) / 2f,
        top = (size.height - side) / 2f,
        side = side,
    )
}

@Composable
private fun NeonSign(strength: Float, fontSizeSp: Float) {
    val lit = strength.coerceIn(0f, 1f)
    val style = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        fontSize = fontSizeSp.sp,
        textAlign = TextAlign.Center,
    )
    // Pink tube slightly larger than the white-pink core, so the glass edge
    // still reads on a light welcome background. Shadows are the gas bloom.
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = SIGN,
            modifier = Modifier.graphicsLayer {
                scaleX = 1.045f
                scaleY = 1.12f
                alpha = lit
            },
            style = style.copy(
                color = NEON_PINK.copy(alpha = 0.95f * lit),
                shadow = Shadow(NEON_PINK.copy(alpha = 0.9f * lit), Offset.Zero, 54f),
            ),
        )
        Text(
            text = SIGN,
            modifier = Modifier.graphicsLayer { alpha = lit },
            style = style.copy(
                color = NEON_PINK,
                shadow = Shadow(NEON_PINK.copy(alpha = lit), Offset.Zero, 18f),
            ),
        )
        Text(
            text = SIGN,
            modifier = Modifier.graphicsLayer {
                scaleX = 0.985f
                scaleY = 0.94f
                alpha = lit
            },
            style = style.copy(
                color = TUBE_CORE.copy(alpha = 0.92f + 0.08f * lit),
                shadow = Shadow(Color.White.copy(alpha = 0.85f * lit), Offset.Zero, 5f),
            ),
        )
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

private val RED_FILTER = ColorFilter.colorMatrix(channel(1f, 0f, 0f))
private val GREEN_FILTER = ColorFilter.colorMatrix(channel(0f, 1f, 0f))
private val BLUE_FILTER = ColorFilter.colorMatrix(channel(0f, 0f, 1f))

private val NEON_PINK = Color(0xFFFF2D95)
private val NEON_CYAN = Color(0xFF2DF6FF)
private val TUBE_CORE = Color(0xFFFFE4F2)

private const val SIGN = "Sneaks AI"
private const val CHAOS_MS = 1_800L
private const val ASSEMBLE_MS = 1_600L
internal const val SETTLED_MS = CHAOS_MS + ASSEMBLE_MS
private const val ASSEMBLE_FROM = 0.82f
private const val IDLE_CYCLE_MS = 4_700L
private const val IDLE_TWITCH_MS = 260L
private const val IDLE_PEAK = 0.22f

private fun supportsHardwareBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
