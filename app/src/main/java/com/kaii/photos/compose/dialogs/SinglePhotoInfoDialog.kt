package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.WallpaperSetter
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.widgets.DateTimePicker
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.eraseExifMedia
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.helpers.rememberMediaRenamer
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

private const val TAG = "com.kaii.photos.compose.dialogs.SinglePhotoInfoDialogs"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SinglePhotoInfoDialog(
    currentMediaItem: MediaStoreData,
    sheetState: SheetState,
    showMoveCopyOptions: Boolean,
    isTouchLocked: Boolean,
    dismiss: () -> Unit,
    onMoveMedia: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        sheetState.partialExpand()
    }

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
                BackHandler(
                    enabled = !WindowInsets.isImeVisible
                ) {
                    coroutineScope.launch {
                        sheetState.hide()
                        dismiss()
                    }
                }

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
                            currentMediaItem = currentMediaItem,
                            showMoveCopyOptions = showMoveCopyOptions,
                            onMoveMedia = onMoveMedia,
                            dismiss = dismiss,
                            privacyMode = isTouchLocked,
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
                            currentMediaItem = currentMediaItem,
                            showMoveCopyOptions = showMoveCopyOptions,
                            privacyMode = isTouchLocked,
                            onMoveMedia = onMoveMedia,
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
    currentMediaItem: MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: Boolean,
    onMoveMedia: () -> Unit,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    val context = LocalContext.current
    val mediaData = remember(currentMediaItem) {
        try {
            getExifDataForMedia(
                context = context,
                inputStream = context.contentResolver.openInputStream(currentMediaItem.uri.toUri()) ?: File(currentMediaItem.absolutePath).inputStream(),
                absolutePath = currentMediaItem.absolutePath,
                fallback = currentMediaItem.dateTaken
            )
        } catch (_: FileNotFoundException) {
            emptyMap()
        }
    }

    var location by remember { mutableStateOf("") }
    LaunchedEffect(mediaData) {
        with(Dispatchers.IO) {
            val latLong = mediaData[MediaData.LatLong] as? DoubleArray ?: return@LaunchedEffect

            Log.d(TAG, "this is running")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Geocoder(context).getFromLocation(
                    latLong[0],
                    latLong[1],
                    1
                ) {
                    it.firstOrNull()?.let { address ->
                        location = "${address.featureName}, ${address.thoroughfare}, ${address.subAdminArea}, ${address.countryName}"
                    }
                }
            } else {
                @Suppress("deprecation")
                Geocoder(context).getFromLocation(
                    latLong[0],
                    latLong[1],
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
                    currentMediaItem = currentMediaItem,
                    showMoveCopyOptions = showMoveCopyOptions,
                    privacyMode = privacyMode,
                    onMoveMedia = onMoveMedia,
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
                    currentMediaItem = currentMediaItem,
                    showMoveCopyOptions = showMoveCopyOptions,
                    privacyMode = privacyMode,
                    onMoveMedia = onMoveMedia,
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
                .offset(
                    y = if (isLandscape) (-4).dp else 0.dp
                )
        )

        val showConfirmEraseDialog = remember { mutableStateOf(false) }
        val runEraseExifData = remember { mutableStateOf(false) }

        var showDateTimePicker by remember { mutableStateOf(false) }

        if (showDateTimePicker) {
            DateTimePicker(
                mediaItem = currentMediaItem,
                onSuccess = dismiss,
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
                        if (key == MediaData.Date && currentMediaItem.type == MediaType.Image && !privacyMode) {
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
                        title = stringResource(id = if (privacyMode) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                        info = "",
                        icon = if (!privacyMode) R.drawable.swipe else R.drawable.do_not_touch,
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

        ConfirmationDialogWithBody(
            showDialog = showConfirmEraseDialog,
            dialogTitle = stringResource(id = R.string.media_exif_erase),
            dialogBody = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_erase)
        ) {
            runEraseExifData.value = true
        }

        val resources = LocalResources.current
        val mainViewModel = LocalMainViewModel.current
        val coroutineScope = rememberCoroutineScope()

        GetPermissionAndRun(
            uris = listOf(currentMediaItem.uri.toUri()),
            shouldRun = runEraseExifData,
            onGranted = {
                mainViewModel.launch(Dispatchers.IO) {
                    try {
                        eraseExifMedia(currentMediaItem.absolutePath)

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.media_exif_done),
                                icon = R.drawable.checkmark_thin,
                                duration = SnackbarDuration.Short
                            )
                        )
                    } catch (e: Throwable) {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.media_exif_failed),
                                icon = R.drawable.error_2,
                                duration = SnackbarDuration.Short
                            )
                        )

                        Log.e(
                            TAG,
                            "Failed erasing exif data for ${currentMediaItem.absolutePath}"
                        )
                        Log.e(TAG, e.toString())
                        e.printStackTrace()
                    }
                }
            },
            onRejected = {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.permissions_needed),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        )

        if (!isLandscape) {
            TallDialogInfoRow(
                title = stringResource(id = if (privacyMode) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                info = "",
                icon = if (!privacyMode) R.drawable.swipe else R.drawable.do_not_touch,
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
    currentMediaItem: MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: Boolean,
    onMoveMedia: () -> Unit,
    dismiss: () -> Unit
) {
    IconContentImpl(
        currentMediaItem = currentMediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        onMoveMedia = onMoveMedia,
        dismiss = dismiss,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun ColumnScope.IconContent(
    currentMediaItem: MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: Boolean,
    onMoveMedia: () -> Unit,
    dismiss: () -> Unit
) {
    IconContentImpl(
        currentMediaItem = currentMediaItem,
        showMoveCopyOptions = showMoveCopyOptions,
        privacyMode = privacyMode,
        onMoveMedia = onMoveMedia,
        dismiss = dismiss,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun IconContentImpl(
    currentMediaItem: MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: Boolean,
    onMoveMedia: () -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier
) {
    val file = remember(currentMediaItem) { File(currentMediaItem.absolutePath) }
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
    val saveFileName = remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf(originalFileName) }

    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val mediaRenamer = rememberMediaRenamer(uri = currentMediaItem.uri.toUri()) {
        coroutineScope.launch {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = resources.getString(R.string.permissions_needed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Short
                )
            )
        }
    }

    GetPermissionAndRun(
        uris = listOf(currentMediaItem.uri.toUri()),
        shouldRun = saveFileName,
        onGranted = {
            mediaRenamer.rename(
                newName = "${currentFileName}.${file.extension}",
                uri = currentMediaItem.uri.toUri()
            )

            originalFileName = currentFileName
        }
    )

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
                    saveFileName.value = true
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
        enabled = !privacyMode,
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

        val stateList = SnapshotStateList<PhotoLibraryUIModel>()
        stateList.add(PhotoLibraryUIModel.Media(item = currentMediaItem))

        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = stateList,
            isMoving = isMoving,
            groupedMedia = null,
            insetsPadding = WindowInsets.statusBars,
            onMoveMedia = onMoveMedia,
            dismissInfoDialog = dismiss
        )

        IconButton(
            onClick = {
                isMoving = true
                show.value = true
            },
            enabled = !privacyMode,
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
            enabled = !privacyMode,
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.copy),
                contentDescription = "copy this media"
            )
        }
    }

    if (currentMediaItem.type == MediaType.Image) {
        val context = LocalContext.current

        IconButton(
            onClick = {
                val intent = Intent(context, WallpaperSetter::class.java).apply {
                    action = Intent.ACTION_SET_WALLPAPER
                    data = currentMediaItem.uri.toUri()
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("mimeType", currentMediaItem.mimeType)
                }

                context.startActivity(intent)
            },
            enabled = !privacyMode,
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