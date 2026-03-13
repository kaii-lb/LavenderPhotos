package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.pages.WallpaperSetter
import com.kaii.photos.compose.widgets.date_time.DateTimePicker
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.eraseExifMedia
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import com.kaii.photos.permissions.files.rememberMediaRenamer
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.reflect.KClass

private const val TAG = "com.kaii.photos.compose.dialogs.SinglePhotoInfoDialogs"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SinglePhotoInfoDialog(
    mediaItem: () -> MediaStoreData,
    mediaData: () -> Map<MediaData, String>,
    sheetState: SheetState,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    albumType: KClass<out AlbumType>,
    preserveDate: () -> Boolean,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides
                RippleConfiguration(
                    color = Color.Transparent,
                    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
                )
    ) {
        val isLandscape by rememberDeviceOrientation()

        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
        ) {
            // reset ripple for normal buttons
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration()
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        Content(
                            mediaItem = mediaItem,
                            mediaData = mediaData,
                            showMoveCopyOptions = showMoveCopyOptions,
                            privacyMode = privacyMode,
                            albumType = albumType,
                            preserveDate = preserveDate,
                            dismiss = dismiss,
                            togglePrivacyMode = togglePrivacyMode
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Content(
                            mediaItem = mediaItem,
                            mediaData = mediaData,
                            showMoveCopyOptions = showMoveCopyOptions,
                            privacyMode = privacyMode,
                            albumType = albumType,
                            preserveDate = preserveDate,
                            dismiss = dismiss,
                            togglePrivacyMode = togglePrivacyMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    mediaItem: () -> MediaStoreData,
    mediaData: () -> Map<MediaData, String>,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    albumType: KClass<out AlbumType>,
    preserveDate: () -> Boolean,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    val context = LocalContext.current

    var location by remember { mutableStateOf("") }
    LaunchedEffect(mediaData()) {
        withContext(Dispatchers.IO) {
            val latLong = mediaData()[MediaData.LatLong]?.split(' ') ?: return@withContext

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

    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    isCustomAlbum = albumType == AlbumType.Custom::class || albumType == AlbumType.Cloud::class,
                    preserveDate = preserveDate,
                    dismiss = dismiss
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
                    isCustomAlbum = albumType == AlbumType.Custom::class || albumType == AlbumType.Cloud::class,
                    preserveDate = preserveDate,
                    dismiss = dismiss
                )
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

        val showConfirmEraseDialog = remember { mutableStateOf(false) }
        var showDateTimePicker by remember { mutableStateOf(false) }

        if (showDateTimePicker) {
            DateTimePicker(
                mediaItem = mediaItem(),
                onDismiss = {
                    showDateTimePicker = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(1f)
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
        ) {
            items(
                items = mediaData().keys.filter { it != MediaData.LatLong }.toList() // we don't want to display straight up coordinates
            ) { key ->
                val data = mediaData()
                val value = data[key]
                val name = stringResource(id = key.description)

                TallDialogInfoRow(
                    title = name,
                    info = value.toString(),
                    icon = key.icon,
                    position =
                        if (data.keys.indexOf(key) == data.keys.size - 1 && location.isBlank())
                            RowPosition.Bottom
                        else if (data.keys.indexOf(key) == 0)
                            RowPosition.Top
                        else
                            RowPosition.Middle,
                    onClick = {
                        if (key == MediaData.Date &&
                            mediaItem().type == MediaType.Image &&
                            albumType != AlbumType.Cloud::class &&
                            !privacyMode()
                        ) {
                            showDateTimePicker = true
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
                        position = RowPosition.Single
                    ) {
                        togglePrivacyMode()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    TallDialogInfoRow(
                        title = stringResource(id = R.string.media_exif_erase),
                        info = "",
                        icon = R.drawable.error,
                        position = RowPosition.Single,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        showConfirmEraseDialog.value = true
                    }
                }
            }
        }

        val resources = LocalResources.current
        val coroutineScope = rememberCoroutineScope()
        val permissionState = rememberFilePermissionManager(
            onGranted = {
                context.appModule.scope.launch(Dispatchers.IO) {
                    try {
                        eraseExifMedia(mediaItem().absolutePath)

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvent.MessageEvent(
                                message = resources.getString(R.string.media_exif_done),
                                icon = R.drawable.checkmark_thin,
                                duration = SnackbarDuration.Short
                            )
                        )
                    } catch (e: Throwable) {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvent.MessageEvent(
                                message = resources.getString(R.string.media_exif_failed),
                                icon = R.drawable.error_2,
                                duration = SnackbarDuration.Short
                            )
                        )

                        Log.e(
                            TAG,
                            "Failed erasing exif data for ${mediaItem().absolutePath}"
                        )
                        Log.e(TAG, e.toString())
                        e.printStackTrace()
                    }
                }
            },
            onRejected = {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = resources.getString(R.string.permissions_needed),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        )

        ConfirmationDialogWithBody(
            showDialog = showConfirmEraseDialog,
            dialogTitle = stringResource(id = R.string.media_exif_erase),
            dialogBody = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_erase)
        ) {
            permissionState.get(uris = listOf(mediaItem().uri.toUri()))
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
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                showConfirmEraseDialog.value = true
            }
        }
    }
}

@Composable
private fun RowScope.IconContent(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    isCustomAlbum: Boolean,
    preserveDate: () -> Boolean,
    dismiss: () -> Unit
) {
    IconContentImpl(
        mediaItem = mediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        isCustomAlbum = isCustomAlbum,
        preserveDate = preserveDate,
        modifier = Modifier.weight(1f),
        dismiss = dismiss
    )
}

@Composable
private fun ColumnScope.IconContent(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    isCustomAlbum: Boolean,
    preserveDate: () -> Boolean,
    dismiss: () -> Unit
) {
    IconContentImpl(
        mediaItem = mediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        isCustomAlbum = isCustomAlbum,
        preserveDate = preserveDate,
        modifier = Modifier.weight(1f),
        dismiss = dismiss
    )
}

@Composable
private fun IconContentImpl(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    isCustomAlbum: Boolean,
    preserveDate: () -> Boolean,
    modifier: Modifier,
    dismiss: () -> Unit
) {
    val file = remember(mediaItem()) { File(mediaItem().absolutePath) }
    var originalFileName by remember(file) {
        mutableStateOf(
            file.nameWithoutExtension.let {
                if (it.startsWith(".")) {
                    it.replace("trashed-", "")
                        .replaceBefore("-", "")
                        .replaceFirst("-", "")
                } else {
                    it
                }
            }
        )
    }

    var currentFileName by remember { mutableStateOf(originalFileName) }
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val mediaRenamer = rememberMediaRenamer {
        coroutineScope.launch {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.MessageEvent(
                    message = resources.getString(R.string.permissions_needed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Short
                )
            )
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    if (showRenameDialog) {
        TextEntryDialog(
            title = stringResource(id = R.string.media_rename),
            placeholder = originalFileName,
            startValue = originalFileName,
            onConfirm = { newName ->
                val valid = newName != originalFileName

                if (valid) {
                    currentFileName = newName
                    mediaRenamer.rename(
                        newName = "${currentFileName}.${file.extension}",
                        uri = mediaItem().uri.toUri()
                    )

                    originalFileName = currentFileName
                    showRenameDialog = false
                }

                valid
            },
            onValueChange = { newName ->
                newName != originalFileName
            },
            onDismiss = {
                showRenameDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            showRenameDialog = true
        },
        enabled = !privacyMode(),
        modifier = modifier
            .height(48.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.name),
            contentDescription = "rename this media"
        )
    }

    if (showMoveCopyOptions) {
        val show = remember { mutableStateOf(false) }
        var isMoving by remember { mutableStateOf(false) }

        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = listOf(
                SelectionManager.SelectedItem(
                    id = mediaItem().id,
                    isImage = mediaItem().type == MediaType.Image,
                    parentPath = mediaItem().parentPath
                )
            ),
            isMoving = isMoving,
            insetsPadding = WindowInsets.statusBars,
            dismissInfoDialog = dismiss,
            preserveDate = preserveDate,
            clear = {}
        )

        IconButton(
            onClick = {
                isMoving = true
                show.value = true
            },
            enabled = !privacyMode() && !isCustomAlbum,
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cut),
                contentDescription = "move this media"
            )
        }

        IconButton(
            onClick = {
                isMoving = false
                show.value = true
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.copy),
                contentDescription = "copy this media"
            )
        }
    }

    if (mediaItem().type == MediaType.Image) {
        val context = LocalContext.current

        IconButton(
            onClick = {
                val intent = Intent(context, WallpaperSetter::class.java).apply {
                    action = Intent.ACTION_SET_WALLPAPER
                    data = mediaItem().uri.toUri()
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("mimeType", mediaItem().mimeType)
                }

                context.startActivity(intent)
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.imagesearch_roller),
                contentDescription = "set as wallpaper"
            )
        }
    }
}