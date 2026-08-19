package com.kaii.photos.compose.dialogs.album_info

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.TallDialogInfoRow
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.RowPosition

@Composable
fun InfoContent(
    albumInfo: () -> AlbumType,
    modifier: Modifier = Modifier,
    itemCount: () -> Int,
    albumSize: () -> String
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
    ) {
        if (albumInfo() is AlbumType.Folder) {
            item {
                val context = LocalContext.current
                val resources = LocalResources.current

                val info = albumInfo() as AlbumType.Folder

                TallDialogInfoRow(
                    title =
                        if (info.paths.size > 1) stringResource(id = R.string.albums_paths)
                        else stringResource(id = R.string.albums_path),
                    info = info.paths.joinToString(separator = ",") { it },
                    icon = R.drawable.folder,
                    position = RowPosition.Top,
                    onClick = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(
                            resources.getString(R.string.albums_path),
                            info.paths.joinToString(separator = ",") { it }
                        )
                        clipboardManager.setPrimaryClip(clipData)
                    }
                )
            }
        }

        if (albumInfo() is AlbumType.SAFFolder) {
            item {
                val context = LocalContext.current
                val resources = LocalResources.current

                val info = albumInfo() as AlbumType.SAFFolder

                TallDialogInfoRow(
                    title = stringResource(id = R.string.albums_path),
                    info = info.path,
                    icon = R.drawable.folder,
                    position = RowPosition.Top,
                    onClick = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(
                            resources.getString(R.string.albums_path),
                            info.path
                        )
                        clipboardManager.setPrimaryClip(clipData)
                    }
                )
            }
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current

            TallDialogInfoRow(
                title = stringResource(id = R.string.albums_item_count),
                info = itemCount().toString(),
                icon = R.drawable.data,
                position = RowPosition.Middle,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.albums_item_count),
                        itemCount().toString()
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current

            TallDialogInfoRow(
                title = stringResource(id = R.string.immich_uuid),
                info = albumInfo().immichId ?: stringResource(id = R.string.albums_immich_not_backed_up),
                icon = R.drawable.cloud_upload,
                position = RowPosition.Middle,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.immich_uuid),
                        albumInfo().immichId
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current

            TallDialogInfoRow(
                title = stringResource(id = R.string.exif_size),
                info = albumSize(),
                icon = R.drawable.storage,
                position = RowPosition.Bottom,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.exif_size),
                        albumSize()
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }
    }
}