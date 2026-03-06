package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMap
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.TextEntryDialog
import com.kaii.photos.datastore.AlbumGroup

@Composable
fun CreateAlbumGroupDialog(
    groups: List<AlbumGroup>,
    addGroup: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    TextEntryDialog(
        title = stringResource(id = R.string.album_group),
        placeholder = stringResource(id = R.string.album_group_name),
        errorMessage = stringResource(id = R.string.album_group_name_error),
        onValueChange = { name ->
            name.isNotBlank() && name !in groups.fastMap { it.name }
        },
        onConfirm = { name ->
            addGroup(name)
            onDismiss()
            true
        },
        onDismiss = onDismiss
    )
}