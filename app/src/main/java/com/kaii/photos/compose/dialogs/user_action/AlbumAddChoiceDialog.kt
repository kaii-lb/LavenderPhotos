package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.compose.dialogs.TitleCloseRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.helpers.filename
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AlbumAddChoiceDialog(
    groups: List<AlbumGroup>,
    addAlbum: (album: AlbumType) -> Unit,
    addGroup: (name: String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.albums_type),
            onClose = onDismiss,
            closeOffset = 8.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .padding(8.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activityLauncher = createDirectoryPicker { path, basePath ->
                if (path != null && basePath != null) addAlbum(
                    AlbumType.Folder(
                        id = Uuid.random().toString(),
                        name = path.filename(),
                        paths = setOf(basePath + path),
                        pinned = false,
                        immichId = null
                    )
                )
            }

            PreferencesRow(
                title = stringResource(id = R.string.albums_folder),
                summary = stringResource(id = R.string.albums_folder_desc),
                position = RowPosition.Top,
                iconResID = R.drawable.albums
            ) {
                activityLauncher.launch(null)
            }

            var showCustomAlbumDialog by remember { mutableStateOf(false) }
            if (showCustomAlbumDialog) {
                AddCustomAlbumDialog(
                    addAlbum = addAlbum,
                    onDismissPrev = onDismiss,
                    onDismiss = {
                        showCustomAlbumDialog = false
                    }
                )
            }

            PreferencesRow(
                title = stringResource(id = R.string.albums_custom),
                summary = stringResource(id = R.string.albums_custom_desc),
                position = RowPosition.Middle,
                iconResID = R.drawable.art_track
            ) {
                showCustomAlbumDialog = true
            }

            var showAlbumGroupDialog by remember { mutableStateOf(false) }
            if (showAlbumGroupDialog) {
                CreateAlbumGroupDialog(
                    groups = groups,
                    addGroup = addGroup,
                    onDismiss = {
                        showAlbumGroupDialog = false
                    }
                )
            }

            PreferencesRow(
                title = stringResource(id = R.string.album_group),
                summary = stringResource(id = R.string.album_group_desc),
                position = RowPosition.Bottom,
                iconResID = R.drawable.grid_view
            ) {
                showAlbumGroupDialog = true
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}