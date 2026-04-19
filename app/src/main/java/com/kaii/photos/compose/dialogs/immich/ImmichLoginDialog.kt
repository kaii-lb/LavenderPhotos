package com.kaii.photos.compose.dialogs.immich

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.helpers.RowPosition
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ImmichLoginDialog(
    login: (email: String, password: String, userAgent: String, onDone: suspend (success: Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp, 0.dp, 8.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val email = remember { mutableStateOf("") }
            val password = remember { mutableStateOf("") }

            val focusManager = LocalFocusManager.current

            TitleCloseRow(
                title = stringResource(id = R.string.immich_login),
                closeOffset = 16.dp,
                onClose = onDismiss
            )

            ClearableTextField(
                text = email,
                placeholder = stringResource(id = R.string.immich_auth_email),
                modifier = Modifier.Companion,
                icon = R.drawable.mail,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentType = ContentType.EmailAddress,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                onConfirm = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onClear = {
                    email.value = ""
                }
            )

            ClearableTextField(
                text = password,
                placeholder = stringResource(id = R.string.immich_auth_passowrd),
                modifier = Modifier.Companion,
                icon = R.drawable.password,
                onConfirm = {
                    login(
                        email.value,
                        password.value,
                        System.getProperty("http.agent") ?: ""
                    ) { success ->
                        password.value = ""

                        if (success) onDismiss()
                    }
                },
                visualTransformation = PasswordVisualTransformation(mask = '\u2B24'),
                contentType = ContentType.Password,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClear = {
                    password.value = ""
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            FullWidthDialogButton(
                text = stringResource(id = R.string.immich_login_confirm),
                color = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                position = RowPosition.Single
            ) {
                login(
                    email.value,
                    password.value,
                    System.getProperty("http.agent") ?: ""
                ) { success ->
                    password.value = ""

                    if (success) onDismiss()
                }
            }
        }
    }
}