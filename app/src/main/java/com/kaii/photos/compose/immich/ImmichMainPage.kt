package com.kaii.photos.compose.immich

import android.util.Patterns
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.AnnotatedExplanationDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.ImmichLoginDialog
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.compose.widgets.PreferenceRowWithCustomBody
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.immich.ImmichServerState
import com.kaii.photos.immich.ImmichUserLoginState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ImmichMainPage() {
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
        val mainViewModel = LocalMainViewModel.current
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
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
                val immichBasicInfo by mainViewModel.settings.Immich.getImmichBasicInfo()
                    .collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

                if (showAddressDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_endpoint_base),
                        placeholder = stringResource(id = R.string.immich_endpoint_base_placeholder),
                        errorMessage = resources.getString(R.string.immich_server_url_invalid),
                        onConfirm = { value ->
                            if ((value.startsWith("https://") || value.startsWith("http://"))
                                && ((Regex("(?<=:)[0-9]+$").containsMatchIn(value) && value.count { it == ':' } <= 2)
                                        || Patterns.WEB_URL.matcher(value).matches()
                                        )
                            ) {
                                // TODO: check if we can ping server address?
                                mainViewModel.settings.Immich.setImmichBasicInfo(
                                    ImmichBasicInfo(
                                        endpoint = value.removeSuffix("/"),
                                        bearerToken = immichBasicInfo.bearerToken,
                                        username = immichBasicInfo.username,
                                        pfpPath = immichBasicInfo.pfpPath
                                    )
                                )
                                showAddressDialog = false
                                true
                            } else false
                        },
                        onValueChange = { value ->
                            (value.startsWith("https://") || value.startsWith("http://"))
                                    && ((Regex("(?<=:)[0-9]+$").containsMatchIn(value) && value.count { it == ':' } <= 2)
                                    || Patterns.WEB_URL.matcher(value).matches()
                                    )
                            // TODO: check if we can ping server address?
                        },
                        onDismiss = {
                            showAddressDialog = false
                        }
                    )
                }


                val showClearEndpointDialog = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                if (showClearEndpointDialog.value) {
                    ConfirmationDialogWithBody(
                        showDialog = showClearEndpointDialog,
                        dialogTitle = stringResource(id = R.string.immich_clear_endpoint),
                        dialogBody = stringResource(id = R.string.immich_clear_endpoint_desc),
                        confirmButtonLabel = stringResource(id = R.string.media_confirm),
                    ) {
                        coroutineScope.launch {
                            if (immichBasicInfo.bearerToken != "") {
                                immichViewModel.logoutUser()
                            }

                            mainViewModel.settings.Immich.setImmichBasicInfo(
                                ImmichBasicInfo.Empty
                            )
                        }
                        showClearEndpointDialog.value = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.immich_endpoint_base),
                    summary =
                        if (immichBasicInfo.endpoint == "") stringResource(id = R.string.immich_endpoint_base_desc)
                        else immichBasicInfo.endpoint,
                    iconResID = R.drawable.data,
                    position = RowPosition.Middle,
                    showBackground = false
                ) {
                    if (immichBasicInfo.endpoint.isEmpty()) showAddressDialog = true
                    else showClearEndpointDialog.value = true
                }
            }

            item {
                val mainViewModel = LocalMainViewModel.current
                val userInfo by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()
                val immichBasicInfo by mainViewModel.settings.Immich.getImmichBasicInfo()
                    .collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
                var showLoginDialog by remember { mutableStateOf(false) }
                var isLoadingInfo by remember { mutableStateOf(true) }

                if (showLoginDialog) {
                    ImmichLoginDialog(
                        endpointBase = immichBasicInfo.endpoint
                    ) {
                        showLoginDialog = false
                    }
                }

                LaunchedEffect(Unit) {
                    immichViewModel.refreshUserInfo {
                        isLoadingInfo = false
                    }
                }

                val showLogoutDialog = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                if (showLogoutDialog.value) {
                    ConfirmationDialogWithBody(
                        showDialog = showLogoutDialog,
                        dialogTitle = stringResource(id = R.string.immich_logout),
                        dialogBody = stringResource(id = R.string.immich_logout_desc),
                        confirmButtonLabel = stringResource(id = R.string.media_confirm),
                    ) {
                        coroutineScope.launch {
                            if (immichBasicInfo.bearerToken == "") return@launch

                            immichViewModel.logoutUser()
                        }
                        showLogoutDialog.value = false
                    }
                }

                val resources = LocalResources.current
                val title by remember {
                    derivedStateOf {
                        if (userInfo is ImmichUserLoginState.IsNotLoggedIn) resources.getString(R.string.immich_login_unavailable)
                        else resources.getString(R.string.immich_login_found) + " " + (userInfo as ImmichUserLoginState.IsLoggedIn).info.name
                    }
                }
                val summary by remember {
                    derivedStateOf {
                        if (userInfo is ImmichUserLoginState.IsNotLoggedIn) resources.getString(R.string.immich_login_unavailable_desc)
                        else resources.getString(R.string.immich_email) + " " + (userInfo as ImmichUserLoginState.IsLoggedIn).info.email
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .clickable {
                            if (immichBasicInfo.endpoint != "" && !isLoadingInfo) {
                                if (userInfo is ImmichUserLoginState.IsNotLoggedIn) showLoginDialog = true
                                else showLogoutDialog.value = true
                            }
                        }
                        .padding(16.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pfpPath by remember {
                        derivedStateOf {
                            val file = File((userInfo as ImmichUserLoginState.IsLoggedIn).info.profileImagePath)
                            if (file.exists()) file.absolutePath
                            else null
                        }
                    }

                    if (userInfo is ImmichUserLoginState.IsNotLoggedIn || pfpPath == null) {
                        Icon(
                            painter = painterResource(id = R.drawable.account_circle),
                            contentDescription = "an icon describing: $title",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(28.dp)
                        )
                    } else {
                        GlideImage(
                            model = pfpPath,
                            contentDescription = "User profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        ) {
                            it.diskCacheStrategy(DiskCacheStrategy.NONE)
                        }
                    }

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
                            color = if (immichBasicInfo.endpoint != "" && !isLoadingInfo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.5f
                            )
                        )

                        Text(
                            text = summary,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            color = if (immichBasicInfo.endpoint != "" && !isLoadingInfo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.5f
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isLoadingInfo) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.immich_server)
                )
            }

            item {
                val serverInfo by immichViewModel.immichServerState.collectAsStateWithLifecycle()
                val userInfo by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()
                val resources = LocalResources.current

                LaunchedEffect(userInfo) {
                    if (userInfo is ImmichUserLoginState.IsLoggedIn) immichViewModel.refreshServerInfo()
                }

                val info by remember {
                    derivedStateOf {
                        if (serverInfo is ImmichServerState.HasInfo) {
                            Pair(
                                (serverInfo as ImmichServerState.HasInfo).info.version.toString(),
                                (serverInfo as ImmichServerState.HasInfo).info.build.toString()
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
                        if (serverInfo is ImmichServerState.HasInfo) {
                            Pair(
                                (serverInfo as ImmichServerState.HasInfo).storage.diskUse,
                                (serverInfo as ImmichServerState.HasInfo).storage.diskSize
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
                    title = stringResource(id = R.string.immich_server_info),
                    summary = stringResource(id = R.string.immich_server_info_desc, info.first, info.second),
                    iconResID = R.drawable.handyman,
                    position = RowPosition.Middle,
                    showBackground = false,
                    enabled = userInfo is ImmichUserLoginState.IsLoggedIn
                )

                PreferenceRowWithCustomBody(
                    icon = R.drawable.storage,
                    title = stringResource(id = R.string.immich_server_storage),
                    enabled = userInfo is ImmichUserLoginState.IsLoggedIn
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val animated by animateFloatAsState(
                        targetValue = if (serverInfo is ImmichServerState.HasInfo) {
                            (serverInfo as ImmichServerState.HasInfo).storage.diskUsagePercentage.toFloat() / 100f
                        } else {
                            1f
                        }
                    )

                    LinearProgressIndicator(
                        progress = {
                            animated
                        },
                        color = if (serverInfo is ImmichServerState.HasInfo) {
                            ProgressIndicatorDefaults.linearColor
                        } else {
                            MaterialTheme.colorScheme.error
                        }.copy(alpha = if (userInfo is ImmichUserLoginState.IsLoggedIn) 1f else 0.6f),
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text =
                            if (serverInfo is ImmichServerState.HasInfo) {
                                stringResource(id = R.string.immich_server_storage_desc, storage.first, storage.second)
                            } else {
                                stringResource(id = R.string.immich_server_info_failed)
                            },
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (serverInfo is ImmichServerState.HasInfo) 1f else 0.6f),
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
                val alwaysShowInfo by mainViewModel.settings.Immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.immich_always_show_user_info),
                    summary = stringResource(id = R.string.immich_always_show_user_info_desc),
                    iconResID = R.drawable.id_card,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = alwaysShowInfo,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Immich.setAlwaysShowUserInfo(checked)
                    }
                )
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