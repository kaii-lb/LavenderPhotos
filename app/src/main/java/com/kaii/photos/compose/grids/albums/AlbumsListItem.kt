package com.kaii.photos.compose.grids.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.net.toUri
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.compose.widgets.albums.AlbumGlideImage
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.permissions.files.DirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager

@Composable
fun MoveCopyAlbumsListItem(
    album: AlbumGridState.Album.Single,
    position: RowPosition,
    info: () -> ImmichBasicInfo,
    selectedItemsList: List<SelectionManager.SelectedItem>,
    show: MutableState<Boolean>,
    modifier: Modifier,
    dismissInfoDialog: () -> Unit,
    clear: () -> Unit,
    onClick: () -> Unit
) {
    val filePermissionManager = rememberFilePermissionManager(
        onGranted = {
            onClick()

            clear()
            dismissInfoDialog()

            show.value = false
        }
    )

    AlbumsListItemImpl(
        album = album,
        position = position,
        info = info,
        modifier = modifier,
        onDirPermissionGranted = {
            filePermissionManager.get(
                uris = selectedItemsList.fastMapNotNull { item ->
                    item.uri.takeIf {
                        !item.isCloud
                    }?.toUri()
                }
            )
        }
    )
}

@Preview
@Composable
private fun AlbumListItemPreview() {
    AlbumsListItemImpl(
        album = AlbumGridState.Album.Single(
            info = AlbumGridState.Info(
                album = AlbumType.PlaceHolder,
                thumbnail = AlbumGridState.Info.Thumbnail(
                    uri = "",
                    signature = ObjectKey(0),
                    albumId = "",
                    date = 0L,
                    isGif = false
                )
            ),
            id = "",
            name = "Test Album",
            summary = "Testing summary here",
            date = 0L,
            pinned = false,
        ),
        position = RowPosition.Single,
        info = { ImmichBasicInfo.Empty },
        modifier = Modifier,
        onDirPermissionGranted = {}
    )
}

@Composable
fun AlbumsListItemImpl(
    album: AlbumGridState.Album.Single,
    position: RowPosition,
    info: () -> ImmichBasicInfo,
    modifier: Modifier,
    suffix: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 24.dp)

    Row(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start
        )
    ) {
        AlbumGlideImage(
            albumInfo = album.info,
            info = info,
            modifier = Modifier
                .size(64.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (album.summary != null) {
                Text(
                    text = album.summary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    overflow = TextOverflow.StartEllipsis,
                    maxLines = 1
                )
            }
        }

        if (album.info.album !is AlbumType.Folder) {
            Icon(
                painter = painterResource(
                    id =
                        if (album.info.album is AlbumType.Custom) R.drawable.art_track
                        else R.drawable.cloud
                ),
                contentDescription = stringResource(
                    id =
                        if (album.info.album is AlbumType.Custom) R.string.albums_is_custom
                        else R.string.albums_is_cloud
                ),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(
                        if (album.info.album is AlbumType.Custom) 22.dp
                        else 20.dp
                    )
            )
        }

        suffix?.invoke()
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumsListItemImpl(
    album: AlbumGridState.Album.Single,
    position: RowPosition,
    info: () -> ImmichBasicInfo,
    modifier: Modifier,
    onDirPermissionGranted: () -> Unit,
    suffix: (@Composable () -> Unit)? = null,
    dirPermissionManager: DirectoryPermissionManager = rememberDirectoryPermissionManager(onGranted = onDirPermissionGranted)
) {
    AlbumsListItemImpl(
        album = album,
        position = position,
        info = info,
        modifier = modifier,
        suffix = suffix,
        onClick = {
            if (album.info.album is AlbumType.Folder) {
                dirPermissionManager.start(
                    directories = album.info.album.paths
                )
            } else {
                onDirPermissionGranted()
            }
        }
    )
}