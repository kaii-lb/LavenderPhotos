package com.kaii.photos.compose.immich

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.User
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.ImmichLoginDialog
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.datastore.Immich
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmichMainPage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.immich_title),
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                }
            )
        }
    ) { innerPadding ->
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
                val immichEndpointBase by mainViewModel.settings.Immich.getEndpointBase()
                    .collectAsStateWithLifecycle(initialValue = "")
                val bearerToken by mainViewModel.settings.Immich.getBearerToken().collectAsStateWithLifecycle(initialValue = "")

                if (showAddressDialog) {
                    TextEntryDialog(
                        title = stringResource(id = R.string.immich_endpoint_base),
                        placeholder = stringResource(id = R.string.immich_endpoint_base_placeholder),
                        onConfirm = { value ->
                            if (value.startsWith("https://") && Patterns.WEB_URL.matcher(value).matches()) {
                                mainViewModel.settings.Immich.setEndpointBase(endpointBase = value) // TODO: check if we can ping server address?
                                showAddressDialog = false
                                true
                            } else false
                        },
                        onValueChange = { value ->
                            value.startsWith("https://") && Patterns.WEB_URL.matcher(value).matches()
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
                            if (bearerToken == "") return@launch

                            mainViewModel.settings.Immich.setEndpointBase("")
                            User(
                                apiClient = ApiClient(),
                                endpointBase = immichEndpointBase
                            ).logout(bearerToken = bearerToken)
                            mainViewModel.settings.Immich.setBearerToken("")
                            mainViewModel.settings.Immich.setUser(null)
                        }
                        showClearEndpointDialog.value = false
                    }
                }

                Box (
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                showClearEndpointDialog.value = true
                            },
                            onClick = {
                                showAddressDialog = true
                            }
                        )
                ) {
                    PreferencesRow(
                        title = stringResource(id = R.string.immich_endpoint_base),
                        summary =
                            if (immichEndpointBase == "") stringResource(id = R.string.immich_endpoint_base_desc)
                            else immichEndpointBase,
                        iconResID = R.drawable.data,
                        position = RowPosition.Middle,
                        showBackground = false
                    )
                }
            }

            item {
                val user by mainViewModel.settings.Immich.getUser()
                    .collectAsStateWithLifecycle(initialValue = null)
                val endpointBase by mainViewModel.settings.Immich.getEndpointBase()
                    .collectAsStateWithLifecycle(initialValue = "")
                var showLoginDialog by remember { mutableStateOf(false) }
                val immichEndpointBase by mainViewModel.settings.Immich.getEndpointBase()
                    .collectAsStateWithLifecycle(initialValue = "")
                val bearerToken by mainViewModel.settings.Immich.getBearerToken()
                    .collectAsStateWithLifecycle(initialValue = "")

                if (showLoginDialog) {
                    ImmichLoginDialog(
                        endpointBase = endpointBase
                    ) {
                        showLoginDialog = false
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
                            if (bearerToken == "") return@launch

                            User(
                                apiClient = ApiClient(),
                                endpointBase = endpointBase
                            ).logout(bearerToken = bearerToken)
                            mainViewModel.settings.Immich.setBearerToken("")
                            mainViewModel.settings.Immich.setUser(null)
                        }
                        showLogoutDialog.value = false
                    }
                }

                Box (
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                showLogoutDialog.value = true
                            },
                            onClick = {
                                showLoginDialog = true
                            }
                        )
                ) {
                    PreferencesRow(
                        title =
                            if (user == null) stringResource(id = R.string.immich_login_unavailable)
                            else stringResource(id = R.string.immich_login_found) + " " + user!!.name,
                        summary =
                            if (user == null) stringResource(id = R.string.immich_login_unavailable_desc)
                            else stringResource(id = R.string.immich_login_found_desc),
                        iconResID = R.drawable.account_circle,
                        position = RowPosition.Middle,
                        showBackground = false,
                        enabled = immichEndpointBase != ""
                    )
                }
            }

            // item {
            //     val backupAlbums by mainViewModel.settings.Immich.getServerAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
            //
            //     val showConfirmationDialog = remember { mutableStateOf(false) }
            //     if (showConfirmationDialog.value) {
            //         ConfirmationDialogWithBody(
            //             showDialog = showConfirmationDialog,
            //             dialogTitle = stringResource(id = R.string.immich_albums_clear),
            //             dialogBody = stringResource(id = R.string.immich_albums_clear_confirm),
            //             confirmButtonLabel = stringResource(id = R.string.media_confirm)
            //         ) {
            //             mainViewModel.settings.Immich.setBackupsAlbums(emptyList())
            //         }
            //     }
            //
            //     PreferencesRow(
            //         title = stringResource(id = R.string.immich_albums_clear),
            //         summary = stringResource(id = R.string.immich_albums_clear_desc),
            //         iconResID = R.drawable.albums_clear,
            //         position = RowPosition.Middle,
            //         showBackground = false,
            //         enabled = backupAlbums.isNotEmpty()
            //     ) {
            //         showConfirmationDialog.value = true
            //     }
            // }
        }
    }
}
