package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginStateManager
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ImmichLoginDialog(
    loginState: LoginStateManager,
    endpoint: String,
    setImmichBasicInfo: (info: ImmichBasicInfo) -> Unit,
    onDismiss: () -> Unit,
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

            val coroutineScope = rememberCoroutineScope()
            val resources = LocalResources.current

            suspend fun login() {
                val eventTitle =
                    mutableStateOf(resources.getString(R.string.immich_login_ongoing))
                val isLoading = mutableStateOf(true)

                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.LoadingEvent(
                        message = eventTitle.value,
                        icon = R.drawable.account_circle,
                        isLoading = isLoading
                    )
                )

                loginState.login(
                    email = email.value.trim(),
                    password = password.value.trim(),
                    userAgent = System.getProperty("http.agent") ?: ""
                ).let { state ->
                    setImmichBasicInfo(
                        if (state is LoginState.LoggedIn) {
                            ImmichBasicInfo(
                                endpoint = endpoint,
                                accessToken = state.accessToken,
                                username = state.name
                            )
                        } else {
                            ImmichBasicInfo(
                                endpoint = endpoint,
                                accessToken = "",
                                username = ""
                            )
                        }
                    )

                    if (state is LoginState.LoggedIn) {
                        eventTitle.value = resources.getString(R.string.immich_login_successful)
                        isLoading.value = false

                        onDismiss()
                    } else {
                        password.value = ""

                        eventTitle.value = resources.getString(R.string.immich_login_failed)
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.immich_login_failed),
                                duration = SnackbarDuration.Short,
                                icon = R.drawable.error_2
                            )
                        )
                    }
                }
            }

            ClearableTextField(
                text = password,
                placeholder = stringResource(id = R.string.immich_auth_passowrd),
                modifier = Modifier.Companion,
                icon = R.drawable.password,
                onConfirm = {
                    coroutineScope.launch {
                        login()
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
                coroutineScope.launch {
                    login()
                }
            }
        }
    }
}