package com.kaii.photos.compose.widgets

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.signature.ObjectKey
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.DialogClickableItem
import com.kaii.photos.compose.dialogs.DialogExpandableItem
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.immich.ImmichUserLoginState
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

/** [extraAction] used to reset a [DialogExpandableItem]'s on click*/
@Composable
fun AnimatableTextField(
    state: MutableState<Boolean>,
    string: MutableState<String>,
    doAction: MutableState<Boolean>,
    rowPosition: RowPosition,
    modifier: Modifier = Modifier,
    extraAction: MutableState<Boolean>? = null,
    enabled: Boolean = true,
    resetAction: () -> Unit
) {
    var waitForKB by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AnimatedContent(
        targetState = state.value && enabled,
        label = string.value,
        modifier = Modifier
            .then(modifier),
        transitionSpec = {
            (expandHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                ),
                expandFrom = Alignment.Start
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 350,
                )
            )).togetherWith(
                shrinkHorizontally(
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
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
            Column(
                modifier = Modifier
                    .wrapContentHeight()
            ) {
                DialogClickableItem(
                    text = "Rename",
                    iconResId = R.drawable.edit,
                    position = rowPosition,
                    enabled = enabled
                ) {
                    state.value = true
                    extraAction?.value = false
                }
            }
        }
    }
}

@Composable
fun MainDialogUserInfo() {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        LaunchedEffect(Unit) {
            immichViewModel.refreshUserInfo()
        }

        val context = LocalContext.current
        val resources = LocalResources.current
        val userInfo by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()

        var originalName by remember(userInfo) {
            mutableStateOf(
                if (userInfo is ImmichUserLoginState.IsNotLoggedIn) resources.getString(R.string.immich_login_unavailable)
                else (userInfo as ImmichUserLoginState.IsLoggedIn).info.name
            )
        }
        val pfpPath by remember {
            derivedStateOf {
                if (userInfo is ImmichUserLoginState.IsLoggedIn) {
                    val file = File((userInfo as ImmichUserLoginState.IsLoggedIn).info.profileImagePath)
                    if (file.exists()) file.absolutePath
                    else R.drawable.cat_picture
                } else R.drawable.cat_picture
            }
        }
        val pfpSignature by remember {
            derivedStateOf {
                if (pfpPath is String) {
                    ObjectKey(File(pfpPath as String).lastModified())
                } else ObjectKey(0)
            }
        }

        var username by remember {
            mutableStateOf(
                originalName
            )
        }

        val coroutineScope = rememberCoroutineScope()

        val pfpPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
            var success = false
            if (uri != null) {
                val media = context.contentResolver.getMediaStoreDataFromUri(context = context, uri = uri)

                if (media != null) {
                    success = true
                    immichViewModel.setProfilePic(media.immichFile)
                }
            }

            if (success) {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.immich_set_pfp_success),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.face
                        )
                    )
                }
            } else {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.immich_set_pfp_fail),
                            duration = SnackbarDuration.Short,
                            icon = R.drawable.error_2
                        )
                    )
                }
            }
        }

        InvalidatableGlideImage(
            path = pfpPath,
            signature = pfpSignature,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable {
                    pfpPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }
        )

        Spacer(modifier = Modifier.width(8.dp))

        val focus = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        var changeName by remember { mutableStateOf(false) }
        var backPressedCallbackEnabled by remember { mutableStateOf(false) }

        BackHandler(
            enabled = backPressedCallbackEnabled
        ) {
            focusManager.clearFocus()
        }

        LaunchedEffect(changeName, originalName) {
            focusManager.clearFocus()

            if (!changeName && username != originalName) {
                username = originalName
                return@LaunchedEffect
            } else if (changeName) {
                originalName = username
                immichViewModel.setUsername(username)
                changeName = false
            }
        }

        TextField(
            value = username,
            onValueChange = { newVal ->
                username = newVal
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = TextUnit(16f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
            colors = TextFieldDefaults.colors().copy(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTrailingIconColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                disabledIndicatorColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
                showKeyboardOnFocus = true
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    changeName = true
                },
            ),
            trailingIcon = {
                if (userInfo is ImmichUserLoginState.IsLoggedIn) {
                    Icon(
                        painter = painterResource(id = R.drawable.checkmark_thin),
                        contentDescription = "Confirm filename change button",
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                focusManager.clearFocus()
                                changeName = true
                            }
                    )
                }
            },
            shape = RoundedCornerShape(1000.dp),
            enabled = userInfo is ImmichUserLoginState.IsLoggedIn,
            modifier = Modifier
                .focusRequester(focus)
                .onFocusChanged {
                    backPressedCallbackEnabled = it.isFocused
                }
        )
    }
}