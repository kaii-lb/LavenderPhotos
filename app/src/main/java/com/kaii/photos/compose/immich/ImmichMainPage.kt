package com.kaii.photos.compose.immich

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.ImmichLoginDialog
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition
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
                val immichBasicInfo by mainViewModel.settings.Immich.getImmichBasicInfo()
                    .collectAsStateWithLifecycle(initialValue = ImmichBasicInfo("", ""))

                if (showAddressDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_endpoint_base),
                        placeholder = stringResource(id = R.string.immich_endpoint_base_placeholder),
                        onConfirm = { value ->
                            if ((value.startsWith("https://") || value.startsWith("http://"))
                                && Patterns.WEB_URL.matcher(value).matches()
                            ) {
                                mainViewModel.settings.Immich.setImmichBasicInfo(
                                    ImmichBasicInfo(
                                        endpoint = value.removeSuffix("/"),
                                        bearerToken = immichBasicInfo.bearerToken
                                    )
                                ) // TODO: check if we can ping server address?
                                showAddressDialog = false
                                true
                            } else false
                        },
                        onValueChange = { value ->
                            (value.startsWith("https://") || value.startsWith("http://"))
                                    && Patterns.WEB_URL.matcher(value).matches()
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
                            if (immichBasicInfo.bearerToken == "") return@launch

                            immichViewModel.logoutUser()
                            mainViewModel.settings.Immich.setImmichBasicInfo(
                                ImmichBasicInfo("", "")
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
                    .collectAsStateWithLifecycle(initialValue = ImmichBasicInfo("", ""))
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

                val context = LocalContext.current
                val title by remember {
                    derivedStateOf {
                        if (userInfo is ImmichUserLoginState.IsNotLoggedIn) context.resources.getString(R.string.immich_login_unavailable)
                        else context.resources.getString(R.string.immich_login_found) + " " + (userInfo as ImmichUserLoginState.IsLoggedIn).info.name
                    }
                }
                val summary by remember {
                    derivedStateOf {
                        if (userInfo is ImmichUserLoginState.IsNotLoggedIn) context.resources.getString(R.string.immich_login_unavailable_desc)
                        else context.resources.getString(R.string.immich_email) + " " + (userInfo as ImmichUserLoginState.IsLoggedIn).info.email
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .clickable {
                            if (immichBasicInfo.endpoint != "" && !isLoadingInfo) {
                                // if (userInfo is ImmichUserLoginState.IsNotLoggedIn) showLoginDialog = true
                                // else showLogoutDialog.value = true
                                showLoginDialog = true
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
        }
    }
}
