package com.newoether.agora.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.newoether.agora.R
import com.newoether.agora.ui.motion.LocalAgoraMotionPolicy
import kotlinx.coroutines.delay

/**
 * Shows the 0x Sneaky Alpha CRT art over the app when it starts, holds it briefly, then fades into
 * the app. Tap to skip. Skipped under reduced motion and screenshot runs. Survives rotation without
 * replaying.
 */
@Composable
internal fun SneaksLaunchSplash(enabled: Boolean = true) {
    val allowMotion = LocalAgoraMotionPolicy.current.allowContinuousMotion
    var showing by rememberSaveable { mutableStateOf(enabled && allowMotion) }
    AnimatedVisibility(
        visible = showing,
        enter = fadeIn(tween(250)),
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
            Image(
                painter = painterResource(R.drawable.sneaky_alpha_crt),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val SPLASH_MS = 1_800L
