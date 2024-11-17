package com.kaii.photos.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.delay

@Composable
fun TextFieldWithConfirm(
    text: MutableState<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
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
                        painter = painterResource(id = R.drawable.edit),
                        contentDescription = "Edit Icon",
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                cursorColor = CustomMaterialTheme.colorScheme.primary,
                focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onConfirm()
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(1000.dp, 0.dp, 0.dp, 1000.dp),
            modifier = Modifier
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .height(56.dp)
                .width(32.dp)
                .clip(RoundedCornerShape(0.dp, 1000.dp, 1000.dp, 0.dp))
                .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                .weight(0.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .clickable {
                        onConfirm()
                        keyboardController?.hide()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
                    contentDescription = "Confirm text change",
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SearchTextField(
    searchedForText: MutableState<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        TextField(
            value = searchedForText.value,
            onValueChange = {
                searchedForText.value = it
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
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search Icon",
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                cursorColor = CustomMaterialTheme.colorScheme.primary,
                focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(1000.dp, 0.dp, 0.dp, 1000.dp),
            modifier = Modifier
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .height(56.dp)
                .width(32.dp)
                .clip(RoundedCornerShape(0.dp, 1000.dp, 1000.dp, 0.dp))
                .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                .weight(0.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .clickable {
                        onClear()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Clear search query",
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }
}

/** [extraAction] used to reset a [DialogExpandableItem]'s on click*/
@Composable
fun AnimatableTextField(
    state: MutableState<Boolean>,
    string: MutableState<String>,
    doAction: MutableState<Boolean>,
    rowPosition: RowPosition,
    modifier: Modifier = Modifier,
    extraAction: MutableState<Boolean>? = null,
    resetAction: () -> Unit
) {
    var waitForKB by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AnimatedContent (
        targetState = state.value,
        label = string.value,
        modifier = Modifier
            .then(modifier),
        transitionSpec = {
            (expandHorizontally (
                animationSpec = tween(
                    durationMillis = 350
                ),
                expandFrom = Alignment.Start
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 350,
                )
            )).togetherWith(
                shrinkHorizontally (
                    animationSpec = tween(
                        durationMillis = 350
                    ),
                    shrinkTowards = Alignment.End
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 350,
                    )
                )
            )
        }
    ) { showFirst ->
        if (showFirst) {
            TextField(
                value = string.value,
                onValueChange = {
                    string.value = it
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        doAction.value = true
                        waitForKB = true
                    }
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = CustomMaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                    showKeyboardOnFocus = true
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            doAction.value = false
                            waitForKB = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Cancel filename change button"
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors().copy(
                    unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .focusRequester(focus)
                    .fillMaxWidth(1f)
            )

            LaunchedEffect(Unit) {
                delay(500)
                focus.requestFocus()

            }

            LaunchedEffect(waitForKB) {
                if (!waitForKB) return@LaunchedEffect

                delay(200)

                resetAction()
                state.value = false
                waitForKB = false
            }
        } else {
            Column (
                modifier = Modifier
                    .wrapContentHeight()
            ) {
                DialogClickableItem(
                    text = "Rename",
                    iconResId = R.drawable.edit,
                    position = rowPosition,
                ) {
                    state.value = true
                    extraAction?.value = false
                }
            }
        }
    }
}