package com.kaii.photos.compose.immich

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.immich.PasswordChangeDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.UpdatableProfileImage
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.bytesToGB
import com.kaii.photos.models.OperationStatus
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModel
import com.kaii.photos.repositories.LoginState
import com.kaii.photos.repositories.ServerInfo
import io.github.kaii_lb.lavender.immichintegration.serialization.FullUserResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.UsageByUserDto
import io.github.kaii_lb.lavender.immichintegration.serialization.UserAvatarColor
import io.github.kaii_lb.lavender.immichintegration.serialization.UserStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first

@Composable
fun ImmichAccountPage(
    viewModel: ImmichInfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val immichInfo by viewModel.info.collectAsStateWithLifecycle()

    LifecycleStartEffect(Unit) {
        viewModel.setCanRefresh(true)

        onStopOrDispose {
            viewModel.setCanRefresh(false)
        }
    }

    ImmichAccountPageImpl(
        userInfo = { userInfo },
        serverInfo = { serverInfo },
        immichInfo = { immichInfo },
        operationStatus = viewModel.operationStatus,
        refreshStatus = viewModel.refreshStatus,
        navController = LocalNavController.current,
        modifier = modifier,
        setUsername = {
            viewModel.updateInfo(
                context = context,
                username = it
            )
        },
        setEmail = {
            viewModel.updateInfo(
                context = context,
                email = it
            )
        },
        setProfilePicture = {
            viewModel.changeProfilePicture(
                uri = it,
                context = context
            )
        },
        setPassword = { current, new ->
            viewModel.changePassword(
                context = context,
                currentPassword = current,
                newPassword = new
            )
        },
        refresh = viewModel::refresh,
        logout = viewModel::logout,
    )
}

@Preview
@Composable
private fun ImmichAccountPagePreview() {
    ImmichAccountPageImpl(
        userInfo = {
            LoginState.LoggedIn(
                user = FullUserResponse(
                    avatarColor = UserAvatarColor.Blue,
                    email = "example@test.dev",
                    id = "",
                    name = "example",
                    profileChangedAt = "",
                    profileImagePath = "",
                    createdAt = "",
                    updatedAt = "",
                    deletedAt = null,
                    isAdmin = true,
                    license = null,
                    oauthId = "",
                    quotaSizeInBytes = null,
                    quotaUsageInBytes = 42 * 1024 * 1024 * 1024,
                    shouldChangePassword = false,
                    status = UserStatus.Active,
                    storageLabel = null
                )
            )
        },
        serverInfo = {
            ServerInfo(
                version = "v2.7.5",
                build = "24350167851",
                online = true,
                diskSize = "100 GiB",
                diskUsed = "25 GiB",
                diskUsedPercentage = 0.25f,
                perUserStorage = listOf(
                    UsageByUserDto(
                        photos = 9812,
                        videos = 5125,
                        quotaSizeInBytes = 5L * 1024 * 1024 * 1024,
                        usage = (2.65 * 1024 * 1024 * 1024).toLong(),
                        usagePhotos = (0.75 * 1024 * 1024 * 1024).toLong(),
                        userId = "",
                        userName = "example"
                    )
                ),
                newVersion = "v2.7.5"
            )
        },
        immichInfo = { ImmichBasicInfo.Empty },
        operationStatus = emptyFlow(),
        refreshStatus = emptyFlow(),
        navController = rememberNavController(),
        modifier = Modifier,
        setUsername = {},
        setEmail = {},
        setProfilePicture = {},
        setPassword = { _, _ -> },
        refresh = {},
        logout = {}
    )
}

@Composable
private fun ImmichAccountPageImpl(
    userInfo: () -> LoginState,
    serverInfo: () -> ServerInfo?,
    immichInfo: () -> ImmichBasicInfo,
    operationStatus: Flow<OperationStatus>,
    refreshStatus: Flow<OperationStatus>,
    navController: NavController,
    modifier: Modifier,
    setUsername: (String) -> Unit,
    setEmail: (String) -> Unit,
    setProfilePicture: (Uri?) -> Unit,
    setPassword: (String, String) -> Unit,
    refresh: () -> Unit,
    logout: () -> Unit
) {
    val perUserStorage by remember {
        derivedStateOf {
            val info = serverInfo()
            val userInfo = userInfo()

            if (info != null && userInfo is LoginState.LoggedIn) {
                info.perUserStorage.first { it.userId == userInfo.user.id }
            } else null
        }
    }

    var refreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(refreshStatus) {
        refreshStatus.collect { status ->
            refreshing = status == OperationStatus.Loading
        }
    }

    Scaffold(
        topBar = {
            ImmichAccountPageTopBar(
                navController = navController,
                pullToRefreshState = pullToRefreshState,
                refreshing = { refreshing }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .pullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = {
                        refresh()
                    },
                    state = pullToRefreshState
                )
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val pfpPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        setProfilePicture(uri)
                    }

                    UpdatableProfileImage(
                        immichInfo = immichInfo,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .clickable(enabled = userInfo() is LoginState.LoggedIn) {
                                pfpPicker.launch(
                                    input = PickVisualMediaRequest(
                                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                    )
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.media_information)
                )
            }

            item {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_account_username),
                        placeholder = (userInfo() as? LoginState.LoggedIn)?.user?.name ?: "",
                        startValue = "",
                        type = KeyboardType.Text,
                        onConfirm = { username ->
                            setUsername(username)

                            // wait for set username result
                            operationStatus.first {
                                it != OperationStatus.Loading
                            } == OperationStatus.Successful
                        },
                        onValueChange = {
                            true
                        },
                        onDismiss = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_account_username),
                    iconResID = R.drawable.name,
                    position = RowPosition.Top,
                    summary = (userInfo() as? LoginState.LoggedIn)?.user?.name ?: stringResource(id = R.string.immich_account_username),
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    enabled = userInfo() is LoginState.LoggedIn,
                    action = {
                        showDialog = true
                    }
                )
            }

            item {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_auth_email),
                        placeholder = (userInfo() as? LoginState.LoggedIn)?.user?.email ?: "",
                        startValue = "",
                        errorMessage = stringResource(id = R.string.immich_account_change_email_invalid),
                        type = KeyboardType.Email,
                        onConfirm = { email ->
                            setEmail(email)

                            // wait for set username result
                            operationStatus.first {
                                it != OperationStatus.Loading
                            } == OperationStatus.Successful
                        },
                        onValueChange = { email ->
                            Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        },
                        onDismiss = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_auth_email),
                    iconResID = R.drawable.mail,
                    position = RowPosition.Middle,
                    summary = (userInfo() as? LoginState.LoggedIn)?.user?.email ?: stringResource(id = R.string.immich_auth_email),
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    enabled = userInfo() is LoginState.LoggedIn,
                    action = {
                        showDialog = true
                    }
                )
            }

            item {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    PasswordChangeDialog(
                        changePassword = setPassword,
                        operationStatus = operationStatus,
                        onDismiss = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_auth_password),
                    iconResID = R.drawable.password,
                    position = RowPosition.Middle,
                    summary = "*".repeat(8),
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    enabled = userInfo() is LoginState.LoggedIn,
                    action = {
                        showDialog = true
                    }
                )
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.immich_account_role),
                    iconResID = R.drawable.supervisor_account,
                    position = RowPosition.Bottom,
                    summary = (userInfo() as? LoginState.LoggedIn)?.user?.isAdmin?.let {
                        stringResource(
                            id =
                                if (it) R.string.immich_account_is_admin
                                else R.string.immich_account_is_user
                        )
                    } ?: "...",
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    action = null
                )
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.immich_account_usage)
                )
            }

            item {
                PreferenceRowWithCustomBody(
                    icon = R.drawable.storage,
                    title = stringResource(id = R.string.immich_account_storage),
                    position = RowPosition.Top,
                    enabled = userInfo() is LoginState.LoggedIn,
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val animated by animateFloatAsState(
                        targetValue =
                            perUserStorage?.quotaSizeInBytes?.let {
                                perUserStorage!!.usage.toFloat() / it
                            } ?: 1f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )

                    LinearProgressIndicator(
                        progress = {
                            animated
                        },
                        color =
                            if (perUserStorage != null) {
                                ProgressIndicatorDefaults.linearColor
                            } else {
                                MaterialTheme.colorScheme.error
                            }.copy(
                                alpha =
                                    if (userInfo() is LoginState.LoggedIn && perUserStorage?.quotaSizeInBytes != null) 1f
                                    else 0.6f
                            ),
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            when {
                                perUserStorage != null && perUserStorage?.quotaSizeInBytes == null -> {
                                    stringResource(
                                        id = R.string.immich_account_unlimited,
                                        perUserStorage!!.usage.bytesToGB()
                                    )
                                }

                                perUserStorage != null -> {
                                    stringResource(
                                        id = R.string.immich_server_storage_desc_detailed,
                                        perUserStorage!!.usage.bytesToGB(),
                                        perUserStorage!!.quotaSizeInBytes?.bytesToGB() ?: 0f
                                    )
                                }

                                else -> {
                                    stringResource(id = R.string.immich_server_info_failed)
                                }
                            },
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (serverInfo() != null) 1f else 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.immich_account_usage_images),
                    iconResID = R.drawable.photogrid,
                    position = RowPosition.Middle,
                    summary = perUserStorage?.photos?.toString() ?: stringResource(id = R.string.immich_state_unknown),
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    action = null
                )
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.immich_account_usage_videos),
                    iconResID = R.drawable.videocam,
                    position = RowPosition.Bottom,
                    summary = perUserStorage?.videos?.toString() ?: stringResource(id = R.string.immich_state_unknown),
                    showBackground = true,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    action = null
                )
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.management)
                )
            }

            item {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    ConfirmationDialog(
                        title = stringResource(id = R.string.immich_logout_question_desc),
                        confirmButtonLabel = stringResource(id = R.string.immich_logout),
                        action = {
                            logout()
                        },
                        onDismiss = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_logout),
                    iconResID = R.drawable.logout,
                    position = RowPosition.Single,
                    summary = stringResource(id = R.string.immich_logout_desc),
                    showBackground = true,
                    backgroundColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    cornerRadius = 32.dp,
                    innerCornerRadius = 8.dp,
                    action = {
                        showDialog = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImmichAccountPageTopBar(
    navController: NavController,
    pullToRefreshState: PullToRefreshState,
    refreshing: () -> Boolean
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
        ),
        actions = {
            val animatedScale by animateFloatAsState(
                targetValue = if (refreshing()) 1f else pullToRefreshState.distanceFraction.coerceAtMost(1.25f)
            )

            AnimatedVisibility(
                visible = refreshing() || pullToRefreshState.distanceFraction > 0f,
                enter = fadeIn() + scaleIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = fadeOut() + scaleOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                modifier = Modifier
                    .scale(animatedScale)
            ) {
                ContainedLoadingIndicator(
                    modifier = Modifier
                        .size(32.dp)
                )
            }
        }
    )
}