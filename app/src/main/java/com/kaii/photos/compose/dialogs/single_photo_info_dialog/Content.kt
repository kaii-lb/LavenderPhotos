package com.kaii.photos.compose.dialogs.single_photo_info_dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.date_time.DateTimePicker
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.exif.MediaData

@Composable
fun Content(
    mediaItem: () -> MediaStoreData,
    mediaDataResult: () -> Result<Map<MediaData, String>, FileOperationError>,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    album: () -> AlbumType,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit,
    runAction: (FileOperationAction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // can't operate on SAF media, so just hide the tools
        if (album() !is AlbumType.SAFFolder) {
            Text(
                text = stringResource(id = R.string.media_tools),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                fontWeight = FontWeight.Bold
            )

            val isLandscape by rememberDeviceOrientation()
            if (!isLandscape) {
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(CircleShape)
                        .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 12.dp,
                        alignment = Alignment.CenterHorizontally
                    )
                ) {
                    IconContent(
                        mediaItem = mediaItem,
                        showMoveCopyOptions = showMoveCopyOptions,
                        privacyMode = privacyMode,
                        album = album,
                        dismiss = dismiss,
                        runAction = runAction
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(CircleShape)
                        .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterVertically
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconContent(
                        mediaItem = mediaItem,
                        showMoveCopyOptions = showMoveCopyOptions,
                        privacyMode = privacyMode,
                        album = album,
                        dismiss = dismiss,
                        runAction = runAction
                    )
                }
            }
        }
    }

    val isLandscape by rememberDeviceOrientation()
    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.media_information),
            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = if (isLandscape) (-4).dp.roundToPx() else 0.dp.roundToPx()
                    )
                }
        )

        var showDateTimePicker by remember { mutableStateOf(false) }
        if (showDateTimePicker) {
            DateTimePicker(
                mediaItem = mediaItem(),
                onDismiss = {
                    showDateTimePicker = false
                }
            )
        }

        MediaDataContent(
            mediaDataResult = mediaDataResult,
            mediaItem = mediaItem,
            album = album,
            privacyMode = privacyMode,
            showDateTimePicker = { showDateTimePicker = true },
            togglePrivacyMode = togglePrivacyMode,
            runAction = runAction
        )
    }
}