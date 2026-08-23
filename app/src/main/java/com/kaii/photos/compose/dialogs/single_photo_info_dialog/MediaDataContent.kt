package com.kaii.photos.compose.dialogs.single_photo_info_dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.TallDialogInfoRow
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.MediaDataContent(
    mediaDataResult: () -> Result<Map<MediaData, String>, FileOperationError>,
    mediaItem: () -> MediaStoreData,
    album: () -> AlbumType,
    privacyMode: () -> Boolean,
    modifier: Modifier = Modifier,
    showDateTimePicker: () -> Unit,
    togglePrivacyMode: () -> Unit,
    runAction: (FileOperationAction) -> Unit
) {
    val context = LocalContext.current
    val mediaData by rememberUpdatedState((mediaDataResult() as? Result.Success)?.data ?: MediaData.Empty)

    var location by remember { mutableStateOf("") }
    LaunchedEffect(mediaData) {
        withContext(Dispatchers.IO) {
            val latLong = mediaData[MediaData.LatLong]?.split(' ') ?: return@withContext

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Geocoder(context).getFromLocation(
                    latLong[0].toDouble(),
                    latLong[1].toDouble(),
                    1
                ) {
                    it.firstOrNull()?.let { address ->
                        location = "${address.featureName}, ${address.thoroughfare}, ${address.subAdminArea}, ${address.countryName}"
                    }
                }
            } else {
                @Suppress("deprecation")
                Geocoder(context).getFromLocation(
                    latLong[0].toDouble(),
                    latLong[1].toDouble(),
                    1
                )?.firstOrNull()?.let { address ->
                    location = "${address.featureName}, ${address.thoroughfare}, ${address.subAdminArea}, ${address.countryName}"
                }
            }
        }
    }

    val isLandscape by rememberDeviceOrientation()
    var showConfirmEraseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth(1f)
            .weight(1f)
            .clip(RoundedCornerShape(32.dp))
    ) {
        items(
            items = mediaData.keys.filter { it != MediaData.LatLong }.toList() // we don't want to display straight up coordinates
        ) { key ->
            val value = mediaData[key]
            val name = stringResource(id = key.description)

            TallDialogInfoRow(
                title = name,
                info = value.toString(),
                icon = key.icon,
                position =
                    if (mediaData.keys.indexOf(key) == mediaData.keys.size - 1 && location.isBlank())
                        RowPosition.Bottom
                    else if (mediaData.keys.indexOf(key) == 0)
                        RowPosition.Top
                    else
                        RowPosition.Middle,
                onClick = {
                    if (key == MediaData.Date &&
                        mediaItem().type == MediaType.Image &&
                        album()::class != AlbumType.Cloud::class &&
                        album()::class != AlbumType.SAFFolder::class &&
                        !privacyMode()
                    ) {
                        showDateTimePicker()
                    } else {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(name, value.toString())
                        clipboardManager.setPrimaryClip(clipData)
                    }
                },
                onLongClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(name, value.toString())
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        if (location.isNotBlank()) {
            item {
                TallDialogInfoRow(
                    title = "Location:",
                    info = location,
                    icon = R.drawable.location,
                    position = RowPosition.Bottom
                ) {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Location", location)
                    clipboardManager.setPrimaryClip(clipData)
                }
            }
        }

        if (isLandscape) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                TallDialogInfoRow(
                    title = stringResource(id = if (privacyMode()) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                    info = "",
                    icon = if (!privacyMode()) R.drawable.swipe else R.drawable.do_not_touch,
                    position = RowPosition.Single,
                    onClick = togglePrivacyMode
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                TallDialogInfoRow(
                    title = stringResource(id = R.string.media_exif_erase),
                    info = "",
                    icon = R.drawable.error,
                    position = RowPosition.Single,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    enabled = !privacyMode() && album() !is AlbumType.SAFFolder && !mediaItem().isCloud,
                    onClick = {
                        showConfirmEraseDialog = true
                    }
                )
            }
        }
    }

    if (showConfirmEraseDialog) {
        ConfirmationDialogWithBody(
            title = stringResource(id = R.string.media_exif_erase),
            body = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_erase),
            action = {
                runAction(
                    FileOperationAction.ClearExifData(
                        absolutePath = mediaItem().absolutePath
                    )
                )
            },
            onDismiss = {
                showConfirmEraseDialog = false
            }
        )
    }

    if (!isLandscape) {
        TallDialogInfoRow(
            title = stringResource(id = if (privacyMode()) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
            info = "",
            icon = if (!privacyMode()) R.drawable.swipe else R.drawable.do_not_touch,
            position = RowPosition.Single
        ) {
            togglePrivacyMode()
        }

        TallDialogInfoRow(
            title = stringResource(id = R.string.media_exif_erase),
            info = "",
            icon = R.drawable.error,
            position = RowPosition.Single,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            enabled = !privacyMode() && album() !is AlbumType.SAFFolder && !mediaItem().isCloud,
            onClick = {
                showConfirmEraseDialog = true
            }
        )
    }
}