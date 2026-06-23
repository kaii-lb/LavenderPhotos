package com.kaii.photos.compose.immich.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.RowPosition

@Composable
fun ServerAddressRow(
    loginInfo: () -> ImmichBasicInfo,
    validateAddress: (address: String) -> Boolean,
    removeAddress: () -> Unit,
    setServerAddress: suspend (address: String) -> Boolean
) {
    var showAddressDialog by remember { mutableStateOf(false) }
    val resources = LocalResources.current

    if (showAddressDialog) {
        TextEntryDialog(
            title = stringResource(id = R.string.immich_endpoint_base),
            placeholder = stringResource(id = R.string.immich_endpoint_base_placeholder),
            errorMessage = resources.getString(R.string.immich_server_url_invalid),
            onConfirm = { address ->
                setServerAddress(address.trim())
            },
            onValueChange = { value ->
                validateAddress(value.trim())
            },
            onDismiss = {
                showAddressDialog = false
            }
        )
    }

    var showClearEndpointDialog by remember { mutableStateOf(false) }
    if (showClearEndpointDialog) {
        ConfirmationDialogWithBody(
            title = stringResource(id = R.string.immich_clear_endpoint),
            body = stringResource(id = R.string.immich_clear_endpoint_desc),
            confirmButtonLabel = stringResource(id = R.string.media_confirm),
            action = removeAddress,
            onDismiss = {
                showClearEndpointDialog = false
            }
        )
    }

    PreferencesRow(
        title = stringResource(id = R.string.immich_endpoint_base),
        summary =
            if (loginInfo().endpoint == "") stringResource(id = R.string.immich_endpoint_base_desc)
            else loginInfo().endpoint,
        iconResID = R.drawable.data,
        position = RowPosition.Middle,
        showBackground = false
    ) {
        if (loginInfo().endpoint.isEmpty()) showAddressDialog = true
        else showClearEndpointDialog = true
    }
}