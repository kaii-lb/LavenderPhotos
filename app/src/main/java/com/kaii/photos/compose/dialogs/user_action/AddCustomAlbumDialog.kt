package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AddCustomAlbumDialog(
    addAlbum: (album: AlbumType) -> Unit,
    onDismiss: () -> Unit,
    onDismissPrev: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(8.dp, 0.dp)
    ) {
        TextEntryDialog(
            title = stringResource(id = R.string.albums_custom),
            placeholder = stringResource(id = R.string.albums_name),
            errorMessage = stringResource(id = R.string.album_group_name_error),
            onDismiss = onDismiss,
            onValueChange = { text ->
                text.isNotEmpty()
            },
            onConfirm = { text ->
                val albumInfo = AlbumType.Custom(
                    id = Uuid.random().toString(),
                    name = text,
                    pinned = false,
                    immichId = null
                )

                addAlbum(albumInfo)
                onDismissPrev()

                text.isNotEmpty()
            }
        )
    }
}