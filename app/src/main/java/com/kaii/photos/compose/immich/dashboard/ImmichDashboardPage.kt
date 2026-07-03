package com.kaii.photos.compose.immich.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.AnnotatedExplanationDialog
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.OperationStatus
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModel
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.launch
import java.util.UUID

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
                val resources = LocalResources.current
                val coroutineScope = rememberCoroutineScope()

                ServerAddressRow(
                    loginInfo = { loginInfo },
                    validateAddress = viewModel::validateServerAddress,
                    removeAddress = viewModel::removeServer,
                    setServerAddress = { address ->
                        val validated = viewModel.validateServerAddress(address)
                        val pinged = viewModel.ping(address)

                        if (validated && pinged) {
                            viewModel.setInfo(
                                ImmichBasicInfo(
                                    endpoint = address.removeSuffix("/"),
                                    auth = loginInfo.auth,
                                    username = loginInfo.username,
                                    userId = loginInfo.userId,
                                    updatedAt = loginInfo.updatedAt
                                )
                            )
                        } else {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvent.MessageEvent(
                                        message = resources.getString(R.string.immich_server_unreachable),
                                        icon = R.drawable.globe_2_cancel,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }

                        validated && pinged
                    }
                )
            }

            item {
                AccountRow(
                    immichInfo = { loginInfo },
                    userInfo = { userInfo },
                    isLoadingInfo = { isLoadingInfo },
                    pullToRefreshState = pullToRefreshState
                )
            }

            item {
                PreferencesRow(
                    title = stringResource(id = R.string.immich_backup_options),
                    summary = stringResource(id = R.string.immich_backup_options_desc),
                    iconResID = R.drawable.add_photo_alternate,
                    position = RowPosition.Single,
                    showBackground = false,
                    enabled = userInfo is ImmichLoginState.LoggedIn && !isLoadingInfo,
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

                ServerInfoRow(
                    serverInfo = { serverInfo },
                    enabled = { userInfo is ImmichLoginState.LoggedIn }
                )
            }

            item {
                val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
                val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()

                ServerStorageRow(
                    serverInfo = { serverInfo },
                    enabled = { userInfo is ImmichLoginState.LoggedIn }
                )
            }

            item {
                PreferencesSeparatorText(text = stringResource(id = R.string.immich_misc))
            }

            item {
                val context = LocalContext.current
                val resources = LocalResources.current
                val coroutineScope = rememberCoroutineScope()

                var id by retain { mutableStateOf<UUID?>(null) }
                var loading by retain { mutableStateOf(false) }

                LaunchedEffect(id) {
                    if (id != null) {
                        WorkManager.getInstance(context)
                            .getWorkInfoByIdFlow(id!!)
                            .collect {
                                if (it?.state != WorkInfo.State.RUNNING
                                    && it?.state != WorkInfo.State.ENQUEUED
                                ) {
                                    loading = false
                                }
                            }
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_backup_sync),
                    summary = stringResource(id = R.string.immich_backup_sync_desc),
                    iconResID = R.drawable.cloud_sync,
                    position = RowPosition.Single,
                    showBackground = false,
                    enabled = !loading && userInfo is ImmichLoginState.LoggedIn
                ) {
                    loading = true
                    id = CloudSyncWorker.immediateEnqueue(
                        context = context,
                        albumId = null
                    )

                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            event = LavenderSnackbarEvent.MessageEvent(
                                message = resources.getString(R.string.immich_backup_sync_running),
                                icon = R.drawable.cloud_sync,
                                duration = SnackbarDuration.Short
                            )
                        )
                    }
                }
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