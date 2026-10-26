package com.newoether.agora.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.newoether.agora.data.rememberUserName
import java.time.LocalTime
import kotlin.random.Random

/**
 * Text for the empty new-chat screen: a time-of-day greeting from [WelcomeGreetings],
 * with the saved display name slotted in when there is one.
 *
 * Both the hour and the variant are picked once per new chat (keyed on
 * [newChatEntryId]) so the greeting doesn't change while the user is typing,
 * but the next new chat can land on a different variant.
 */
@Composable
internal fun rememberWelcomeText(newChatEntryId: Long): String {
    val name = rememberUserName()
    val hour = remember(newChatEntryId) { LocalTime.now().hour }
    val variantIndex = remember(newChatEntryId) { Random.nextInt() }
    return remember(newChatEntryId, name) {
        WelcomeGreetings.greeting(hour = hour, name = name, variantIndex = variantIndex)
    }
}
