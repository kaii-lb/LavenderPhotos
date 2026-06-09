package com.kaii.photos.compose.widgets.popup_album_chooser

import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.grids.albums.AlbumsListItemImpl
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.RowPosition

@Composable
fun CheckableAlbumItem(
    album: AlbumGridState.Album.Single,
    selected: () -> Boolean,
    position: RowPosition,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    AlbumsListItemImpl(
        album = album,
        position = position,
        info = { ImmichBasicInfo.Empty },
        modifier = modifier,
        suffix = {
            FilledTonalIconToggleButton(
                checked = selected(),
                onCheckedChange = onCheckedChange
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
        },
        onClick = {
            onCheckedChange(!selected())
        }
    )
}