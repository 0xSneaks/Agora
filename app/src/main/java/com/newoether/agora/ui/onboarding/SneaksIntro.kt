package com.newoether.agora.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A 1960s neon sign reading "0x Sneaky Alpha". The small cyan "0x" tube strikes first, then the
 * pink script tube buzzes on with a couple of misfires and holds with a faint hum. Reduced motion
 * shows the sign fully lit.
 */
@Composable
internal fun SneaksIntro(animate: Boolean, modifier: Modifier = Modifier) {
    var elapsedMs by remember { mutableLongStateOf(if (animate) 0L else LIT_MS) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { elapsedMs = (it - start) / 1_000_000 }
    }
    val t = elapsedMs
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            NeonTube("0x", NEON_CYAN, CYAN_CORE, 34.sp, tubeAlpha(t, delayMs = 250, animate))
            NeonTube("Sneaky Alpha", NEON_PINK, PINK_CORE, 46.sp, tubeAlpha(t, delayMs = 700, animate))
        }
    }
}

/** Brightness of one tube: dark, two misfires, warm-up, then lit with a faint mains hum. */
internal fun tubeAlpha(t: Long, delayMs: Long, animate: Boolean): Float {
    if (!animate) return 1f
    val s = t - delayMs
    return when {
        s < 0 -> 0f
        s < 70 -> 0.9f
        s < 210 -> 0.05f
        s < 280 -> 0.75f
        s < 430 -> 0.1f
        s < 900 -> 0.35f + 0.65f * ((s - 430) / 470f)
        // Rare brief dip so the sign feels like real glass, not a static image.
        (s % 5_200) in 0..60 -> 0.7f
        else -> 0.94f + 0.06f * ((s / 40) % 2)
    }
}

@Composable
private fun NeonTube(text: String, glow: Color, core: Color, size: TextUnit, alpha: Float) {
    val base = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Bold,
        fontSize = size,
    )
    // Unlit glass stays faintly visible, like a real sign that is switched off.
    Box(contentAlignment = Alignment.Center) {
        Text(text, style = base.copy(color = glow.copy(alpha = 0.12f)), maxLines = 1, overflow = TextOverflow.Visible)
        Box(Modifier.alpha(alpha), contentAlignment = Alignment.Center) {
            Text(text, style = base.copy(color = glow, shadow = Shadow(glow, Offset.Zero, 46f)), maxLines = 1)
            Text(text, style = base.copy(color = glow, shadow = Shadow(glow, Offset.Zero, 16f)), maxLines = 1)
            Text(text, style = base.copy(color = core, shadow = Shadow(Color.White, Offset.Zero, 4f)), maxLines = 1)
        }
    }
}

private val NEON_PINK = Color(0xFFFF2D95)
private val PINK_CORE = Color(0xFFFFE3F1)
private val NEON_CYAN = Color(0xFF2DF6FF)
private val CYAN_CORE = Color(0xFFE6FEFF)
/** Both tubes are fully lit by this point. */
internal const val LIT_MS = 1_700L
