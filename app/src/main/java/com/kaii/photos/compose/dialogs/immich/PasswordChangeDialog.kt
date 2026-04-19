package com.kaii.photos.compose.dialogs.immich

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.compose.widgets.infiniteLoadingIndicator
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.models.OperationStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Preview
@Composable
private fun PasswordChangeDialogPreview() {
    val channel = Channel<OperationStatus>()
    val coroutineScope = rememberCoroutineScope()

    PasswordChangeDialog(
        operationStatus = channel.receiveAsFlow(),
        modifier = Modifier,
        onDismiss = {},
        changePassword = { _, _ ->
            coroutineScope.launch {
                channel.send(OperationStatus.Loading)
                delay(3000)
                channel.send(OperationStatus.Failed)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordChangeDialog(
    operationStatus: Flow<OperationStatus>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    changePassword: (String, String) -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp, 0.dp, 8.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentPassword = remember { mutableStateOf("") }
            val newPassword1 = remember { mutableStateOf("") }
            val newPassword2 = remember { mutableStateOf("") }

            val hiddenTransformation = remember<VisualTransformation> { PasswordVisualTransformation(mask = '\u2B24') }
            var visualTransformation by remember { mutableStateOf(hiddenTransformation) }

            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            TitleCloseRow(
                title = stringResource(id = R.string.immich_account_change_password),
                closeOffset = 16.dp,
                onClose = onDismiss
            )

            ClearableTextField(
                text = currentPassword,
                placeholder = stringResource(id = R.string.immich_auth_current_password),
                icon = R.drawable.password,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentType = ContentType.Password,
                visualTransformation = visualTransformation,
                shape = RoundedCornerShape(
                    topStart = 32.dp, topEnd = 32.dp,
                    bottomStart = 8.dp, bottomEnd = 8.dp,
                ),
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                onConfirm = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onClear = {
                    currentPassword.value = ""
                }
            )

            ClearableTextField(
                text = newPassword1,
                placeholder = stringResource(id = R.string.immich_auth_new_password),
                icon = R.drawable.password,
                onConfirm = {
                    focusManager.moveFocus(FocusDirection.Down)
                },
                visualTransformation = visualTransformation,
                contentType = ContentType.NewPassword,
                shape = RoundedCornerShape(size = 8.dp),
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClear = {
                    newPassword1.value = ""
                }
            )

            ClearableTextField(
                text = newPassword2,
                placeholder = stringResource(id = R.string.immich_auth_repeat_new_password),
                icon = R.drawable.password,
                onConfirm = {
                    focusManager.moveFocus(FocusDirection.Down)
                    focusManager.moveFocus(FocusDirection.Down)
                },
                visualTransformation = visualTransformation,
                contentType = ContentType.NewPassword,
                shape = RoundedCornerShape(
                    topStart = 8.dp, topEnd = 8.dp,
                    bottomStart = 32.dp, bottomEnd = 32.dp
                ),
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClear = {
                    newPassword2.value = ""
                }
            )

            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf(false) }
            val successColor = MaterialTheme.colorScheme.primary
            val errorColor = MaterialTheme.colorScheme.error

            LaunchedEffect(currentPassword.value, newPassword1.value, newPassword2.value) {
                error = false
            }

            val passwordHint by remember {
                derivedStateOf {
                    val textId = when {
                        error -> R.string.immich_account_change_password_failed
                        newPassword1.value != newPassword2.value -> R.string.immich_auth_password_do_not_match
                        newPassword1.value.length < 8 -> R.string.immich_auth_password_too_short
                        newPassword1.value == currentPassword.value -> R.string.immich_auth_password_same_as_old
                        currentPassword.value.isBlank() -> R.string.immich_auth_password_current_blank
                        else -> R.string.immich_auth_password_good_to_go
                    }

                    Pair(
                        textId,
                        if (textId == R.string.immich_auth_password_good_to_go) successColor
                        else errorColor
                    )
                }
            }

            Text(
                text = stringResource(id = passwordHint.first),
                color = passwordHint.second,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) {
                        visualTransformation =
                            if (visualTransformation == hiddenTransformation) VisualTransformation.None
                            else hiddenTransformation
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(id = R.string.immich_auth_show_password),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                )

                Switch(
                    checked = visualTransformation != hiddenTransformation,
                    onCheckedChange = {
                        visualTransformation =
                            if (visualTransformation == hiddenTransformation) VisualTransformation.None
                            else hiddenTransformation
                    }
                )
            }

            Box(
                modifier = Modifier
                    .infiniteLoadingIndicator {
                        loading
                    }
            ) {
                LaunchedEffect(operationStatus) {
                    operationStatus.collect { status ->
                        error = status == OperationStatus.Failed
                        loading = status == OperationStatus.Loading

                        if (status == OperationStatus.Successful) {
                            delay(AnimationConstants.DURATION.toLong())
                            onDismiss()
                        }
                    }
                }

                FullWidthDialogButton(
                    text = stringResource(id = R.string.immich_account_change_password_confirm),
                    color =
                        if (error) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.contentColorFor(
                        if (error) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                    position = RowPosition.Single,
                    enabled =
                        newPassword1.value == newPassword2.value &&
                                currentPassword.value.isNotBlank() &&
                                newPassword1.value.isNotBlank() &&
                                newPassword1.value != currentPassword.value &&
                                newPassword1.value.length >= 8 &&
                                !loading
                ) {
                    keyboardController?.hide()
                    visualTransformation = hiddenTransformation

                    changePassword(
                        currentPassword.value,
                        newPassword1.value
                    )
                }
            }
        }
    }
}