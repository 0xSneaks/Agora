package com.newoether.agora.ui.chat

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.newoether.agora.R
import com.newoether.agora.data.rememberUserName
import java.time.LocalTime

/** Which greeting fits the local hour (0-23). Late night gets a plain "Hey". */
@StringRes
internal fun greetingResFor(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..16 -> R.string.greeting_afternoon
    in 17..21 -> R.string.greeting_evening
    else -> R.string.greeting_night
}

/**
 * Text for the empty new-chat screen: "Good morning, Name" once a name is saved, otherwise the plain
 * welcome line. The hour is read once per new chat so the greeting doesn't change while typing.
 */
@Composable
internal fun rememberWelcomeText(newChatEntryId: Long): String {
    val name = rememberUserName()
    val hour = remember(newChatEntryId) { LocalTime.now().hour }
    return if (name.isBlank()) {
        stringResource(R.string.welcome_to_agora)
    } else {
        stringResource(greetingResFor(hour), name)
    }
}
