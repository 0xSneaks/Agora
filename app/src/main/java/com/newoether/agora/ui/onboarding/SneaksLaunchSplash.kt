package com.newoether.agora.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.newoether.agora.ui.motion.LocalAgoraMotionPolicy
import kotlinx.coroutines.delay

/**
 * Plays the animated 0x Sneaky Alpha loading sequence ([SneaksIntro]) over the app when it starts:
 * the portrait glitches in while the neon bar fills, then the neon sign lights up. Holds briefly,
 * then fades into the app. Tap to skip. Skipped under reduced motion and screenshot runs. Survives
 * rotation without replaying.
 */
@Composable
internal fun SneaksLaunchSplash(enabled: Boolean = true) {
    val allowMotion = LocalAgoraMotionPolicy.current.allowContinuousMotion
    var showing by rememberSaveable { mutableStateOf(enabled && allowMotion) }
    AnimatedVisibility(
        visible = showing,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(450)),
    ) {
        LaunchedEffect(Unit) { delay(SPLASH_MS); showing = false }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showing = false },
            contentAlignment = Alignment.Center,
        ) {
            SneaksIntro(animate = true)
        }
    }
}

/** The sequence is fully lit at [LIT_MS]; hold the finished sign for a beat before fading out. */
private const val SPLASH_MS = LIT_MS + 700L
