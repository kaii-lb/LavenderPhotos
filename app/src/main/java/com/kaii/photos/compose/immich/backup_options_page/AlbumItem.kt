package com.kaii.photos.compose.immich.backup_options_page

import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.grids.albums.AlbumsListItemImpl
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager

@Composable
fun ImmichAlbumListItem(
    album: AlbumGridState.Album.Single,
    selected: () -> Boolean,
    position: RowPosition,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val dirPermissionManager = rememberDirectoryPermissionManager(onGranted = onToggle)

    AlbumsListItemImpl(
        album = album,
        position = position,
        modifier = modifier,
        onDirPermissionGranted = onToggle,
        suffix = {
            FilledTonalIconToggleButton(
                checked = selected(),
                onCheckedChange = {
                    if (album.info.album is AlbumType.Folder) {
                        dirPermissionManager.start(
                            directories = album.info.album.paths
                        )
                    } else {
                        onToggle()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        id =
                            if (selected()) R.drawable.close
                            else R.drawable.checkmark_thin
                    ),
                    contentDescription = stringResource(id = R.string.immich_album_item_description)
                )
            }
        }
    )
}