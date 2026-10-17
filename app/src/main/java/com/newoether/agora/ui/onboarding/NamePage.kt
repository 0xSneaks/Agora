package com.newoether.agora.ui.onboarding

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

/** Onboarding step where the user can give a name to be greeted by. Optional: blank is fine. */
@Composable
internal fun NamePage(
    name: String,
    hint: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
    )
}
