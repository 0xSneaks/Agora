package com.newoether.agora.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.newoether.agora.R
import com.newoether.agora.ui.motion.LocalAgoraMotionPolicy
import kotlinx.coroutines.delay

/**
 * Plays the Sneaks AI load sequence once over the app when it starts, then fades into the app.
 * Tap to skip. Skipped entirely under reduced motion. Survives rotation without replaying.
 */
@Composable
@androidx.annotation.OptIn(UnstableApi::class)
internal fun SneaksLaunchSplash(enabled: Boolean = true) {
    val allowMotion = LocalAgoraMotionPolicy.current.allowContinuousMotion
    // Off for screenshot captures, where it would cover the screen being captured.
    var showing by rememberSaveable { mutableStateOf(enabled && allowMotion) }
    AnimatedVisibility(visible = showing, exit = fadeOut(tween(450))) {
        val context = LocalContext.current
        val player = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.sneaks_splash}"))
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                prepare()
            }
        }
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) showing = false
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    showing = false
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener); player.release() }
        }
        // Never trap the user behind the splash if playback stalls.
        LaunchedEffect(Unit) { delay(MAX_SPLASH_MS); showing = false }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showing = false },
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val MAX_SPLASH_MS = 5_000L
