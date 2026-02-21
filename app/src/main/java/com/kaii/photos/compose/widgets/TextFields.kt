package com.kaii.photos.compose.widgets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R

@Composable
fun ClearableTextField(
    text: MutableState<String>,
    placeholder: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentType: ContentType? = null,
    onConfirm: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        TextField(
            value = text.value,
            onValueChange = {
                text.value = it
            },
            maxLines = 1,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            prefix = {
                Row {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = "Search Icon",
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            suffix = {
                if (text.value.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Clear search query",
                        tint = contentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                onClear()
                            }
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                focusedPlaceholderColor = contentColor.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = contentColor.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onConfirm()
                    keyboardController?.hide()
                }
            ),
            visualTransformation = visualTransformation,
            shape = CircleShape,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    if (contentType != null) this.contentType = contentType
                }
        )
    }
}