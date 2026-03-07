package com.kaii.photos.compose.dialogs.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.SelectableButtonListDialog
import com.kaii.photos.compose.widgets.RadioButtonRow

@Composable
fun ThumbnailSizeDialog(
    showDialog: MutableState<Boolean>,
    initialValue: Int,
    setThumbnailSize: (size: Int) -> Unit
) {
    var thumbnailSize by remember { mutableIntStateOf(initialValue) }

    SelectableButtonListDialog(
        title = stringResource(id = R.string.settings_storage_thumbnails_resolution),
        body = stringResource(id = R.string.settings_storage_thumbnails_notice),
        showDialog = showDialog,
        buttons = {
            RadioButtonRow(
                text = "32x32" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 32
            ) {
                thumbnailSize = 32
            }

            RadioButtonRow(
                text = "64x64" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 64
            ) {
                thumbnailSize = 64
            }

            RadioButtonRow(
                text = "128x128" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 128
            ) {
                thumbnailSize = 128
            }

            RadioButtonRow(
                text = "256x256" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 256
            ) {
                thumbnailSize = 256
            }

            RadioButtonRow(
                text = "512x512" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 512
            ) {
                thumbnailSize = 512
            }
        },
        onConfirm = {
            setThumbnailSize(thumbnailSize)
        }
    )
}