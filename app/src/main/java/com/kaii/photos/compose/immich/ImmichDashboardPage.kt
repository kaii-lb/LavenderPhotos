package com.kaii.photos.compose.immich

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.AnnotatedExplanationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.UpdatableProfileImage
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.OperationStatus
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModel
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.ServerInfoState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImmichDashboardPage(
    viewModel: ImmichInfoViewModel
) {
    LifecycleStartEffect(Unit) {
        viewModel.setCanRefresh(true)

        onStopOrDispose {
            viewModel.setCanRefresh(false)
        }
    }

    val loginInfo by viewModel.info.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.immich_title),
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
        var isLoadingInfo by remember { mutableStateOf(false) }
        val pullToRefreshState = rememberPullToRefreshState()

        LaunchedEffect(viewModel.refreshStatus) {
            viewModel.refreshStatus.collect { status ->
                isLoadingInfo = status == OperationStatus.Loading
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pullToRefresh(
                    isRefreshing = isLoadingInfo,
                    state = pullToRefreshState,
                    onRefresh = {
                        viewModel.refresh()
                    }
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.immich_account)
                )
            }

            item {
                var showAddressDialog by remember { mutableStateOf(false) }
                val resources = LocalResources.current
                val coroutineScope = rememberCoroutineScope()

                if (showAddressDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_endpoint_base),
                        placeholder = stringResource(id = R.string.immich_endpoint_base_placeholder),
                        errorMessage = resources.getString(R.string.immich_server_url_invalid),
                        onConfirm = { address ->
                            val address = address.trim()

                            when {
                                viewModel.validateServerAddress(address = address) && viewModel.ping(address = address) -> {
                                    viewModel.setInfo(
                                        ImmichBasicInfo(
                                            endpoint = address.removeSuffix("/"),
                                            auth = loginInfo.auth,
                                            username = loginInfo.username,
                                            userId = loginInfo.userId,
                                            updatedAt = loginInfo.updatedAt
                                        )
                                    )
                                    showAddressDialog = false
                                    true
                                }

                                viewModel.validateServerAddress(address = address) -> {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            LavenderSnackbarEvent.MessageEvent(
                                                message = resources.getString(R.string.immich_server_unreachable),
                                                icon = R.drawable.globe_2_cancel,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                    false
                                }

                                else -> {
                                    false
                                }
                            }
                        },
                        onValueChange = { value ->
                            viewModel.validateServerAddress(address = value)
                        },
                        onDismiss = {
                            showAddressDialog = false
                        }
                    )
                }

                val showClearEndpointDialog = remember { mutableStateOf(false) }
                if (showClearEndpointDialog.value) {
                    ConfirmationDialogWithBody(
                        showDialog = showClearEndpointDialog,
                        dialogTitle = stringResource(id = R.string.immich_clear_endpoint),
                        dialogBody = stringResource(id = R.string.immich_clear_endpoint_desc),
                        confirmButtonLabel = stringResource(id = R.string.media_confirm),
                    ) {
                        coroutineScope.launch {
                            viewModel.removeServer()
                        }

                        showClearEndpointDialog.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_endpoint_base),
                    summary =
                        if (loginInfo.endpoint == "") stringResource(id = R.string.immich_endpoint_base_desc)
                        else loginInfo.endpoint,
                    iconResID = R.drawable.data,
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    if (loginInfo.endpoint.isEmpty()) showAddressDialog = true
                    else showClearEndpointDialog.value = true
                }
            }

            item {
                val resources = LocalResources.current
                val title by remember {
                    derivedStateOf {
                        when (userInfo) {
                            is LoginState.LoggedIn -> {
                                resources.getString(R.string.immich_login_found) + " " + (userInfo as LoginState.LoggedIn).user.name
                            }

                            is LoginState.LoggedOut -> {
                                resources.getString(R.string.immich_login_unavailable)
                            }

                            else -> {
                                resources.getString(R.string.immich_login_unreachable)
                            }
                        }
                    }
                }

                val summary by remember {
                    derivedStateOf {
                        when (userInfo) {
                            is LoginState.LoggedIn -> {
                                resources.getString(R.string.immich_email) + " " + (userInfo as LoginState.LoggedIn).user.email
                            }

                            is LoginState.LoggedOut -> {
                                resources.getString(R.string.immich_login_unavailable_desc)
                            }

                            else -> {
                                resources.getString(R.string.immich_login_unreachable_desc)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .clickable(enabled = userInfo !is LoginState.ServerUnreachable && !isLoadingInfo) {
                            navController.navigate(
                                if (userInfo is LoginState.LoggedIn) Screens.Immich.Account
                                else Screens.Immich.Login
                            )
                        }
                        .padding(16.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpdatableProfileImage(
                        immichInfo = { loginInfo },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = title,
                            fontSize = TextUnit(18f, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            color =
                                if (loginInfo.endpoint != "" && !isLoadingInfo) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Text(
                            text = summary,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            color =
                                if (loginInfo.endpoint != "" && !isLoadingInfo) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isLoadingInfo) 1f else pullToRefreshState.distanceFraction
                    )

                    AnimatedVisibility(
                        visible = isLoadingInfo || pullToRefreshState.distanceFraction > 0f,
                        enter = fadeIn() + scaleIn(animationSpec = AnimationConstants.expressiveTween()),
                        exit = fadeOut() + scaleOut(animationSpec = AnimationConstants.expressiveTween()),
                        modifier = Modifier
                            .scale(animatedScale)
                    ) {
                        ContainedLoadingIndicator(
                            modifier = Modifier
                                .size(32.dp)
                        )
                    }
                }
            }
            
            item {
                PreferencesRow(
                    title = stringResource(id = R.string.immich_backup_options),
                    summary = stringResource(id = R.string.immich_backup_options_desc),
                    iconResID = R.drawable.add_photo_alternate,
                    position = RowPosition.Single,
                    showBackground = false,
                    enabled = userInfo is LoginState.LoggedIn,
                    action = {
                        navController.navigate(Screens.Immich.BackupOptions)
                    }
                )
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.immich_server)
                )
            }

            item {
                val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
                val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
                val resources = LocalResources.current

                val info by remember {
                    derivedStateOf {
                        if (serverInfo is ServerInfoState.Available) {
                            Pair(
                                (serverInfo as ServerInfoState.Available).version,
                                (serverInfo as ServerInfoState.Available).build ?: resources.getString(R.string.immich_state_unknown)
                            )
                        } else {
                            Pair(
                                resources.getString(R.string.immich_state_unknown),
                                resources.getString(R.string.immich_state_unknown)
                            )
                        }
                    }
                }

                val storage by remember {
                    derivedStateOf {
                        if (serverInfo is ServerInfoState.Available) {
                            Pair(
                                (serverInfo as ServerInfoState.Available).diskUsed,
                                (serverInfo as ServerInfoState.Available).diskSize
                            )
                        } else {
                            Pair(
                                resources.getString(R.string.immich_state_unknown),
                                resources.getString(R.string.immich_state_unknown)
                            )
                        }
                    }
                }

                PreferencesRow(
                    title = buildAnnotatedString {
                        append(resources.getString(R.string.immich_server_info_with_status))
                        append(" ")

                        val online = (serverInfo as? ServerInfoState.Available)?.online == true

                        withStyle(
                            style = SpanStyle(
                                color =
                                    if (online) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        ) {
                            if (online) append(resources.getString(R.string.immich_server_online))
                            else append(resources.getString(R.string.immich_server_offline))
                        }
                    },
                    summary = stringResource(id = R.string.immich_server_info_desc, info.first, info.second),
                    iconResID = R.drawable.handyman,
                    position = RowPosition.Middle,
                    showBackground = false,
                    enabled = userInfo is LoginState.LoggedIn
                )

                PreferenceRowWithCustomBody(
                    icon = R.drawable.storage,
                    title = stringResource(id = R.string.immich_server_storage),
                    enabled = userInfo is LoginState.LoggedIn
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val animated by animateFloatAsState(
                        targetValue = if (serverInfo is ServerInfoState.Available) {
                            (serverInfo as ServerInfoState.Available).diskUsedPercentage
                        } else {
                            1f
                        }
                    )

                    LinearProgressIndicator(
                        progress = {
                            animated
                        },
                        color =
                            if (serverInfo is ServerInfoState.Available) {
                                ProgressIndicatorDefaults.linearColor
                            } else {
                                MaterialTheme.colorScheme.error
                            }.copy(alpha = if (userInfo is LoginState.LoggedIn) 1f else 0.6f),
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            if (serverInfo is ServerInfoState.Available) {
                                stringResource(id = R.string.immich_server_storage_desc, storage.first, storage.second)
                            } else {
                                stringResource(id = R.string.immich_server_info_failed)
                            },
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (serverInfo is ServerInfoState.Available) 1f else 0.6f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth(1f)
                    )
                }
            }

            item {
                PreferencesSeparatorText(text = stringResource(id = R.string.immich_misc))
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.help)
                )
            }

            item {
                var showExplanationDialog by remember { mutableStateOf(false) }
                if (showExplanationDialog) {
                    AnnotatedExplanationDialog(
                        title = stringResource(id = R.string.immich_help_general),
                        annotatedExplanation = AnnotatedString.fromHtml(
                            htmlString = stringResource(id = R.string.immich_help_detailed_desc)
                        )
                    ) {
                        showExplanationDialog = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_help_general),
                    summary = stringResource(id = R.string.immich_help_general_desc),
                    iconResID = R.drawable.help,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showExplanationDialog = true
                }
            }
        }
    }
}