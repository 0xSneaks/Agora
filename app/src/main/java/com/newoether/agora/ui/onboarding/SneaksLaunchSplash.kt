package com.newoether.agora.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.newoether.agora.ui.motion.LocalAgoraMotionPolicy
import kotlinx.coroutines.delay

/**
 * Lights the neon sign over the app when it starts, holds it briefly, then fades into the app.
 * Tap to skip. Skipped under reduced motion and screenshot runs. Survives rotation without replaying.
 */
@Composable
internal fun SneaksLaunchSplash(enabled: Boolean = true) {
    val allowMotion = LocalAgoraMotionPolicy.current.allowContinuousMotion
    var showing by rememberSaveable { mutableStateOf(enabled && allowMotion) }
    AnimatedVisibility(visible = showing, exit = fadeOut(tween(450))) {
        LaunchedEffect(Unit) { delay(LIT_MS + HOLD_MS); showing = false }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showing = false },
        ) {
            SneaksIntro(animate = true)
        }
    }
}

private const val HOLD_MS = 900L
