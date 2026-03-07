package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TextEntryDialog(
    title: String,
    placeholder: String? = null,
    startValue: String = "",
    errorMessage: String? = null,
    onConfirm: suspend (text: String) -> Boolean,
    onValueChange: (text: String) -> Boolean,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val keyboardController = LocalSoftwareKeyboardController.current
        var text by remember { mutableStateOf(startValue) }
        var showError by remember { mutableStateOf(!onValueChange(text)) }

        TextField(
            value = text,
            onValueChange = {
                text = it
                showError = !onValueChange(it.trim())
            },
            maxLines = 1,
            singleLine = true,
            placeholder = {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        fontSize = TextUnit(16f, TextUnitType.Sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            suffix = {
                if (showError) {
                    val coroutineScope = rememberCoroutineScope()
                    val resources = LocalResources.current

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.MessageEvent(
                                            message = errorMessage ?: resources.getString(R.string.paths_should_be_relative),
                                            icon = R.drawable.error_2,
                                            duration = SnackbarDuration.Short
                                        )
                                    )
                                }
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.error_2),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            ),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        val coroutineScope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        FullWidthDialogButton(
            text = stringResource(id = R.string.media_confirm),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single,
            enabled = !showError && !isLoading
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                isLoading = true
                showError = !onConfirm(text.trim())
                isLoading = false
            }
        }
    }
}