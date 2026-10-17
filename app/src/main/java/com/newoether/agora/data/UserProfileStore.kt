package com.newoether.agora.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")

/** The name the user gives during onboarding, used only to greet them. Blank means not set. */
internal class UserProfileStore(private val context: Context) {
    val name: Flow<String> = context.dataStore.data.map { it[USER_DISPLAY_NAME].orEmpty() }

    suspend fun saveName(value: String) {
        val trimmed = value.trim().take(MAX_NAME_LENGTH)
        context.dataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(USER_DISPLAY_NAME)
            } else {
                prefs[USER_DISPLAY_NAME] = trimmed
            }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

/** The saved display name, or "" when the user hasn't given one. */
@Composable
internal fun rememberUserName(): String {
    val appContext = LocalContext.current.applicationContext
    val store = remember(appContext) { UserProfileStore(appContext) }
    val name by store.name.collectAsState(initial = "")
    return name
}
