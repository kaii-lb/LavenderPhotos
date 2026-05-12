package com.kaii.photos.compose.immich

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.compose.widgets.UpdatableProfileImage
import com.kaii.photos.compose.widgets.infiniteLoadingIndicator
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.OperationStatus
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow

@Preview
@Composable
private fun ImmichLoginPagePreview() {
    ImmichLoginPageImpl(
        immichInfo = { ImmichBasicInfo.Empty },
        operationStatus = emptyFlow(),
        navController = rememberNavController(),
        login = { _, _, _ -> }
    )
}

@Composable
fun ImmichLoginPage(
    viewModel: ImmichInfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val immichInfo by viewModel.info.collectAsStateWithLifecycle()

    LifecycleStartEffect(Unit) {
        viewModel.setCanRefresh(true)

        onStopOrDispose {
            viewModel.setCanRefresh(false)
        }
    }

    ImmichLoginPageImpl(
        immichInfo = { immichInfo },
        operationStatus = viewModel.operationStatus,
        navController = LocalNavController.current,
        modifier = modifier,
        login = { email, password, isApiKey ->
            if (isApiKey) {
                viewModel.authenticate(
                    apiKey = password,
                    context = context
                )
            } else {
                viewModel.login(
                    email = email,
                    password = password,
                    context = context
                )
            }
        }
    )
}

@Composable
private fun ImmichLoginPageImpl(
    immichInfo: () -> ImmichBasicInfo,
    operationStatus: Flow<OperationStatus>,
    navController: NavController,
    modifier: Modifier = Modifier,
    login: (email: String, password: String, isApiKey: Boolean) -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(operationStatus) {
        operationStatus.collectLatest { status ->
            loading = status == OperationStatus.Loading || status == OperationStatus.Successful

            // for dramatic effect
            delay(5000)

            if (status is OperationStatus.Successful) {
                loading = false
                navController.navigate(Screens.Immich.Account) {
                    popUpTo(route = Screens.Immich.Dashboard::class) {
                        inclusive = false
                    }
                }
            }
        }
    }

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isApiKey by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            ImmichLoginPageTopbar(
                navController = navController
            )
        },
        modifier = modifier
            .imePadding()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .infiniteLoadingIndicator(
                            strokeWidth = 8.dp,
                            padding = 4.dp
                        ) {
                            loading
                        }
                ) {
                    UpdatableProfileImage(
                        immichInfo = immichInfo,
                        modifier = Modifier
                            .size(128.dp),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AnimatedContent(
                    targetState = isApiKey,
                    transitionSpec = {
                        val enter = fadeIn(
                            animationSpec = AnimationConstants.expressiveTween()
                        ) + expandVertically(clip = false)

                        val exit = fadeOut(
                            animationSpec = AnimationConstants.expressiveTween(),
                        ) + shrinkVertically(clip = false) { it / 4 }

                        enter.togetherWith(exit)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { visible ->
                    if (!visible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(
                                space = 4.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ClearableTextField(
                                text = email,
                                placeholder = stringResource(id = R.string.immich_auth_email),
                                icon = R.drawable.mail,
                                enabled = !loading,
                                contentType = ContentType.EmailAddress,
                                shape = RoundedCornerShape(
                                    topStart = 32.dp, topEnd = 32.dp,
                                    bottomStart = 8.dp, bottomEnd = 8.dp
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
                                placeholder = stringResource(id = R.string.immich_auth_password),
                                icon = R.drawable.password,
                                enabled = !loading,
                                contentType = ContentType.Password,
                                visualTransformation =
                                    if (showPassword) VisualTransformation.None
                                    else PasswordVisualTransformation(mask = '\u2B24'),
                                shape = RoundedCornerShape(
                                    topStart = 8.dp, topEnd = 8.dp,
                                    bottomStart = 32.dp, bottomEnd = 32.dp
                                ),
                                onConfirm = {
                                    focusManager.moveFocus(FocusDirection.Down)
                                    focusManager.moveFocus(FocusDirection.Down)
                                    keyboardController?.hide()
                                },
                                onClear = {
                                    password.value = ""
                                }
                            )
                        }
                    } else {
                        ClearableTextField(
                            text = password,
                            placeholder = stringResource(id = R.string.immich_auth_api_key),
                            icon = R.drawable.key,
                            enabled = !loading,
                            contentType = ContentType.Password,
                            visualTransformation =
                                if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(mask = '\u2B24'),
                            shape = RoundedCornerShape(size = 32.dp),
                            onConfirm = {
                                focusManager.moveFocus(FocusDirection.Down)
                                focusManager.moveFocus(FocusDirection.Down)
                                keyboardController?.hide()
                            },
                            onClear = {
                                password.value = ""
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val context = LocalContext.current
                    Text(
                        text = stringResource(id = R.string.immich_account_create),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                val intent = Intent().apply {
                                    data = "${immichInfo().endpoint}/admin/users/new".toUri()
                                    action = Intent.ACTION_VIEW
                                }

                                context.startActivity(intent)
                            }
                    )

                    IconToggleButton(
                        checked = isApiKey,
                        onCheckedChange = {
                            isApiKey = it
                        },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContentColor = LocalContentColor.current
                        )
                    ) {
                        Icon(
                            painter = painterResource(
                                id =
                                    if (isApiKey) R.drawable.mail
                                    else R.drawable.key
                            ),
                            contentDescription = stringResource(
                                id =
                                    if (isApiKey) R.string.immich_auth_login_with_api_key
                                    else R.string.immich_auth_login_with_account
                            )
                        )
                    }

                    IconToggleButton(
                        checked = showPassword,
                        onCheckedChange = {
                            showPassword = it
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id =
                                    if (showPassword) R.drawable.visibility_off
                                    else R.drawable.visibility
                            ),
                            contentDescription = stringResource(id = R.string.immich_auth_hide_password)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        login(email.value, password.value, isApiKey)
                    },
                    enabled = !loading
                ) {
                    Text(
                        text = stringResource(id = R.string.immich_login),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImmichLoginPageTopbar(
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.immich_account),
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}