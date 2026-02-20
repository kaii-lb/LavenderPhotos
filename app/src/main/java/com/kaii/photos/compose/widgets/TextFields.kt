package com.kaii.photos.compose.widgets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.immichintegration.serialization.ProfilePictureResponse
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import kotlinx.coroutines.launch

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

@Composable
fun MainDialogUserInfo(
    loginState: LoginState,
    uploadPfp: suspend (bytes: ByteArray, filename: String, accessToken: String) -> ProfilePictureResponse?,
    setUsername: (username: String, accessToken: String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val mainViewModel = LocalMainViewModel.current
    val alwaysShowInfo by mainViewModel.settings.immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)
    val immichInfo by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val originalName by remember {
            derivedStateOf {
                when (loginState) {
                    is LoginState.LoggedIn -> {
                        loginState.name
                    }

                    is LoginState.ServerUnreachable -> {
                        immichInfo.username.takeIf { it.isNotBlank() }
                    }

                    else -> {
                        null
                    }
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
        val pfpPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
            coroutineScope.launch {
                val loading = mutableStateOf(true)
                var success = false

                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.LoadingEvent(
                        message = resources.getString(R.string.immich_set_pfp_loading),
                        icon = R.drawable.face,
                        isLoading = loading
                    )
                )

                if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val filename = context.contentResolver.getAbsolutePathFromUri(uri = uri)?.filename() ?: "image.png"
                        success = uploadPfp(
                            inputStream.readBytes(),
                            filename,
                            (loginState as LoginState.LoggedIn).accessToken
                        ) != null

                        loading.value = false
                    }
                }

                if (success) {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.immich_set_pfp_success),
                            icon = R.drawable.face,
                            duration = SnackbarDuration.Short
                        )
                    )
                } else {
                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.immich_set_pfp_fail),
                                icon = R.drawable.error_2,
                                duration = SnackbarDuration.Short
                            )
                        )
                    }
                }
            }
        }

        UpdatableProfileImage(
            loggedIn = alwaysShowInfo || loginState !is LoginState.LoggedOut,
            pfpUrl = if (alwaysShowInfo && loginState is LoginState.LoggedOut) context.profilePicture else (loginState as? LoginState.LoggedIn)?.pfpUrl ?: "",
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(enabled = loginState is LoginState.LoggedIn) {
                    pfpPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }
        )

        Text(
            text = originalName?.let { "Hi, $it!" } ?: "",
            fontSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE.sp
        )
    }
}