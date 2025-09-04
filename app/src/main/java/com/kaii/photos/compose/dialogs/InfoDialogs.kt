package com.kaii.photos.compose.dialogs

import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.WallpaperSetter
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.widgets.AnimatableTextField
import com.kaii.photos.compose.widgets.MainDialogUserInfo
import com.kaii.photos.compose.widgets.TitleCloseRow
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MediaData
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.checkPathIsDownloads
import com.kaii.photos.helpers.eraseExifMedia
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.renameImage
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.models.main_activity.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "INFO_DIALOGS"

@Composable
fun SingleAlbumDialog(
    showDialog: MutableState<Boolean>,
    album: AlbumInfo,
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    itemCount: Int
) {
    if (showDialog.value) {
        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            },
            modifier = Modifier
                .wrapContentHeight(unbounded = true)
        ) {
            val isEditingFileName = remember { mutableStateOf(false) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                val textWidth = this.maxWidth - 48.dp - 24.dp

                IconButton(
                    onClick = {
                        showDialog.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(0.dp, 0.dp, 0.dp, 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close dialog button",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                AnimatableText(
                    first = stringResource(id = R.string.media_rename),
                    second = album.name,
                    state = isEditingFileName.value,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = textWidth) // for button and right side
                        .padding(4.dp, 0.dp, 0.dp, 4.dp)
                )
            }

            DialogClickableItem(
                text = stringResource(id = R.string.media_select),
                iconResId = R.drawable.check_item,
                position = RowPosition.Top,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    )
                    .height(if (!isEditingFileName.value) 42.dp else 0.dp)
                    .padding(8.dp, 0.dp)
            ) {
                showDialog.value = false
                selectedItemsList.clear()
                selectedItemsList.add(MediaStoreData())
            }

            val fileName = remember { mutableStateOf(album.name) }
            val saveFileName = remember { mutableStateOf(false) }

            val context = LocalContext.current
            val mainViewModel = LocalMainViewModel.current

            if (album.paths.size == 1) {
                GetDirectoryPermissionAndRun(
                    absoluteDirPaths = album.paths,
                    shouldRun = saveFileName,
                    onGranted = {
                        Log.d(TAG, "Running rename ${fileName.value} ${album.name}")
                        if (fileName.value != album.name) {
                            Log.d(TAG, "Running rename - passed first check")
                            val basePath = album.mainPath.toBasePath()
                            val currentVolumes = MediaStore.getExternalVolumeNames(context)
                            val volumeName =
                                if (basePath == baseInternalStorageDirectory) "primary"
                                else currentVolumes.find {
                                    val possible =
                                        basePath.replace("/storage/", "").removeSuffix("/")
                                    it == possible || it == possible.lowercase()
                                }

                            renameDirectory(
                                context = context,
                                absolutePath = album.mainPath,
                                newName = fileName.value,
                                base = volumeName!!
                            )

                            val newInfo = album.copy(
                                name = fileName.value,
                                paths = listOf(album.mainPath.replace(album.name, fileName.value))
                            )
                            mainViewModel.settings.AlbumsList.editInAlbumsList(
                                albumInfo = album,
                                newInfo = newInfo
                            )
                            showDialog.value = false

                            try {
                                context.contentResolver.releasePersistableUriPermission(
                                    context.getExternalStorageContentUriFromAbsolutePath(
                                        absolutePath = newInfo.mainPath,
                                        trimDoc = true
                                    ),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            } catch (e: Throwable) {
                                Log.d(TAG, "Couldn't release permission for ${newInfo.mainPath}")
                                e.printStackTrace()
                            }

                            navController.popBackStack()
                            navController.navigate(
                                Screens.SingleAlbumView(
                                    albumInfo = newInfo
                                )
                            )

                            saveFileName.value = false
                        }
                    },
                    onRejected = {}
                )

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    rowPosition = RowPosition.Middle,
                    enabled = album.paths.fastAll { !it.checkPathIsDownloads() },
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                ) {
                    fileName.value = album.name
                }
            } else {
                LaunchedEffect(saveFileName.value) {
                    if (!saveFileName.value) return@LaunchedEffect

                    val newInfo = album.copy(
                        name = fileName.value
                    )
                    mainViewModel.settings.AlbumsList.editInAlbumsList(
                        albumInfo = album,
                        newInfo = newInfo
                    )
                    navController.popBackStack()
                    navController.navigate(
                        Screens.SingleAlbumView(
                            albumInfo = newInfo
                        )
                    )
                }

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    rowPosition = RowPosition.Middle,
                    enabled = album.paths.fastAll { !it.checkPathIsDownloads() },
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                ) {
                    fileName.value = album.name
                }
            }

            DialogClickableItem(
                text = stringResource(id = R.string.albums_remove),
                iconResId = R.drawable.delete,
                position = RowPosition.Middle,
                enabled = !album.mainPath.checkPathIsDownloads(),
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    )
                    .height(if (!isEditingFileName.value) 42.dp else 0.dp)
                    .padding(8.dp, 0.dp)
            ) {
                mainViewModel.settings.AlbumsList.removeFromAlbumsList(id = album.id)
                showDialog.value = false

                try {
                    context.contentResolver.delete(
                        LavenderContentProvider.CONTENT_URI,
                        "${LavenderMediaColumns.PARENT_ID} = ?",
                        arrayOf("${album.id}")
                    )

                    context.contentResolver.releasePersistableUriPermission(
                        context.getExternalStorageContentUriFromAbsolutePath(
                            album.mainPath,
                            true
                        ),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Throwable) {
                    Log.d(TAG, "Couldn't release permission for ${album.mainPath}")
                    e.printStackTrace()
                }

                navController.popBackStack()
            }

            var isPinned by remember { mutableStateOf(album.isPinned) }
            DialogClickableItem(
                text = if (isPinned) stringResource(id = R.string.albums_unpin) else stringResource(
                    id = R.string.albums_pin
                ),
                iconResId = R.drawable.pin,
                position = RowPosition.Middle,
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    )
                    .height(if (!isEditingFileName.value) 42.dp else 0.dp)
                    .padding(8.dp, 0.dp)
            ) {
                mainViewModel.settings.AlbumsList.editInAlbumsList(
                    albumInfo = album,
                    newInfo = album.copy(
                        isPinned = !isPinned
                    )
                )

                isPinned = !isPinned
            }

            val expanded = remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    )
                    .then(
                        if (!isEditingFileName.value) Modifier
                            .wrapContentHeight(unbounded = true)
                        else Modifier.height(0.dp)
                    )
                    .padding(8.dp, 0.dp, 8.dp, 6.dp)
            ) {
                DialogExpandableItem(
                    text = stringResource(id = R.string.albums_info),
                    iconResId = R.drawable.info,
                    position = RowPosition.Bottom,
                    expanded = expanded
                ) {
                    DialogInfoText(
                        firstText = if (!album.isCustomAlbum) stringResource(id = R.string.albums_path) else stringResource(
                            id = R.string.albums_id
                        ),
                        secondText = if (!album.isCustomAlbum) album.mainPath else album.id.toString(),
                        iconResId = R.drawable.folder,
                    )

                    DialogInfoText(
                        firstText = stringResource(id = R.string.albums_item_count),
                        secondText = itemCount.toString(),
                        iconResId = R.drawable.data,
                    )

                    DialogInfoText(
                        firstText = stringResource(id = R.string.immich_uuid),
                        secondText = album.immichId,
                        iconResId = R.drawable.cloud_done,
                    )
                }
            }
        }
    }
}

/** @param moveCopyInsetsPadding should only be used when showMoveCopyOptions is enabled */
@Composable
fun SinglePhotoInfoDialog(
    showDialog: MutableState<Boolean>,
    currentMediaItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    loadsFromMainViewModel: Boolean,
    showMoveCopyOptions: Boolean = true,
    moveCopyInsetsPadding: WindowInsets? = WindowInsets.statusBars
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val isEditingFileName = remember { mutableStateOf(false) }

    val isLandscape by rememberDeviceOrientation()

    val modifier = remember(isLandscape) {
        if (isLandscape)
            Modifier.width(328.dp)
        else
            Modifier.fillMaxWidth(1f)
    }

    if (showDialog.value) {
        LavenderDialogBase(
            modifier = modifier,
            onDismiss = {
                showDialog.value = false
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f),
            ) {
                IconButton(
                    onClick = {
                        showDialog.value = false
                        isEditingFileName.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close dialog button",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                AnimatableText(
                    first = stringResource(id = R.string.media_rename),
                    second = stringResource(id = R.string.more_options),
                    state = isEditingFileName.value,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight()
            ) {
                var originalFileName = currentMediaItem.displayName
                val fileName = remember { mutableStateOf(originalFileName) }
                val saveFileName = remember { mutableStateOf(false) }

                val expanded = remember { mutableStateOf(false) }

                GetPermissionAndRun(
                    uris = listOf(currentMediaItem.uri),
                    shouldRun = saveFileName,
                    onGranted = {
                        renameImage(context, currentMediaItem.uri, fileName.value)

                        originalFileName = fileName.value

                        if (loadsFromMainViewModel) {
                            val oldName = currentMediaItem.displayName
                            val path = currentMediaItem.absolutePath

                            val newGroupedMedia = groupedMedia.value.toMutableList()

                            // set currentMediaItem to new one with new name
                            val newMedia = currentMediaItem.copy(
                                displayName = fileName.value,
                                absolutePath = path.replace(oldName, fileName.value)
                            )

                            val index = groupedMedia.value.indexOf(currentMediaItem)
                            newGroupedMedia[index] = newMedia
                            groupedMedia.value = newGroupedMedia
                        }
                    }
                )

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    extraAction = expanded,
                    rowPosition = RowPosition.Top
                ) {
                    // fileName.value = originalFileName // TODO: fix so it doesn't reset while trying to allow by user
                }

                var mediaData by remember {
                    mutableStateOf(
                        emptyMap<MediaData, Any>()
                    )
                }

                LaunchedEffect(Unit) {
                    getExifDataForMedia(currentMediaItem.absolutePath).collect {
                        mediaData = it
                    }
                }

                // should add a way to automatically calculate height needed for this
                val addedHeight by remember { derivedStateOf { 36.dp * (mediaData.keys.size + 1) } } // + 1 for the delete exif data row
                val moveCopyHeight =
                    if (showMoveCopyOptions) 82.dp else 0.dp // 40.dp is height of one single row
                val setAsHeight = if (currentMediaItem.type != MediaType.Video) 40.dp else 0.dp

                val height by animateDpAsState(
                    targetValue = if (!isEditingFileName.value && expanded.value) {
                        42.dp + addedHeight + moveCopyHeight + setAsHeight
                    } else if (!isEditingFileName.value && !expanded.value) {
                        42.dp + moveCopyHeight + setAsHeight
                    } else {
                        0.dp
                    },
                    label = "height of other options",
                    animationSpec = tween(
                        durationMillis = 350
                    )
                )

                Column(
                    modifier = Modifier
                        .height(height)
                        .fillMaxWidth(1f)
                ) {
                    if (showMoveCopyOptions && moveCopyInsetsPadding != null) {
                        val show = remember { mutableStateOf(false) }
                        var isMoving by remember { mutableStateOf(false) }

                        val stateList = SnapshotStateList<MediaStoreData>()
                        stateList.add(currentMediaItem)

                        MoveCopyAlbumListView(
                            show = show,
                            selectedItemsList = stateList,
                            isMoving = isMoving,
                            groupedMedia = null,
                            insetsPadding = moveCopyInsetsPadding
                        )

                        DialogClickableItem(
                            text = stringResource(id = R.string.albums_copy_to),
                            iconResId = R.drawable.copy,
                            position = RowPosition.Middle,
                        ) {
                            isMoving = false
                            show.value = true
                        }

                        DialogClickableItem(
                            text = stringResource(id = R.string.albums_move_to),
                            iconResId = R.drawable.cut,
                            position = RowPosition.Middle,
                        ) {
                            isMoving = true
                            show.value = true
                        }
                    }

                    val infoComposable = @Composable {
                        LazyColumn(
                            modifier = Modifier
                                .wrapContentHeight()
                        ) {
                            for (key in mediaData.keys) {
                                item {
                                    val value = mediaData[key]

                                    val splitBy = Regex("(?=[A-Z])")
                                    val split = key.toString().split(splitBy)
                                    // println("SPLIT IS $split")
                                    val name =
                                        if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()

                                    DialogInfoText(
                                        firstText = name,
                                        secondText = value.toString(),
                                        iconResId = key.iconResInt,
                                    )
                                }
                            }

                            item {
                                val showConfirmEraseDialog = remember { mutableStateOf(false) }
                                val runEraseExifData = remember { mutableStateOf(false) }
                                val coroutineScope = rememberCoroutineScope()

                                ConfirmationDialogWithBody(
                                    showDialog = showConfirmEraseDialog,
                                    dialogTitle = stringResource(id = R.string.media_non_existent),
                                    dialogBody = stringResource(id = R.string.action_cannot_be_undone),
                                    confirmButtonLabel = stringResource(id = R.string.media_erase)
                                ) {
                                    runEraseExifData.value = true
                                }

                                GetPermissionAndRun(
                                    uris = listOf(currentMediaItem.uri),
                                    shouldRun = runEraseExifData,
                                    onGranted = {
                                        coroutineScope.launch(Dispatchers.IO) {
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
                                                        duration = SnackbarDuration.Long
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
                                    }
                                )

                                DialogInfoText(
                                    firstText = "",
                                    secondText = "Erase Exif Data",
                                    iconResId = R.drawable.error,
                                    color = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    showConfirmEraseDialog.value = true
                                }
                            }
                        }
                    }

                    if (currentMediaItem.type == MediaType.Image) {
                        DialogClickableItem(
                            text = stringResource(id = R.string.set_as),
                            iconResId = R.drawable.paintbrush,
                            position = RowPosition.Middle
                        ) {
                            val intent = Intent(context, WallpaperSetter::class.java).apply {
                                action = Intent.ACTION_SET_WALLPAPER
                                data = currentMediaItem.uri
                                addCategory(Intent.CATEGORY_DEFAULT)
                                putExtra("mimeType", currentMediaItem.mimeType)
                            }

                            context.startActivity(intent)
                        }
                    }

                    DialogExpandableItem(
                        text = stringResource(id = R.string.more_info),
                        iconResId = R.drawable.info,
                        position = RowPosition.Bottom,
                        expanded = expanded
                    ) {
                        infoComposable()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainAppDialog(
    showDialog: MutableState<Boolean>,
    currentView: MutableState<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    mainViewModel: MainViewModel
) {
    val vibratorManager = rememberVibratorManager()
    val navController = LocalNavController.current

    if (showDialog.value) {
        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            }
        ) {
            TitleCloseRow(
                title = currentView.value.name,
                closeOffset = 8.dp,
            ) {
                showDialog.value = false
            }

            MainDialogUserInfo()

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight()
            ) {
                if (currentView.value != DefaultTabs.TabTypes.albums && currentView.value != DefaultTabs.TabTypes.secure) {
                    DialogClickableItem(
                        text = stringResource(id = R.string.media_select),
                        iconResId = R.drawable.check_item,
                        position = RowPosition.Top,
                    ) {
                        showDialog.value = false
                        selectedItemsList.clear()
                        selectedItemsList.add(MediaStoreData())
                        vibratorManager.vibrateShort()
                    }
                }

                if (currentView.value == DefaultTabs.TabTypes.albums) {
                    var showAlbumTypeDialog by remember { mutableStateOf(false) }
                    if (showAlbumTypeDialog) {
                        AlbumAddChoiceDialog {
                            showAlbumTypeDialog = false
                        }
                    }

                    DialogClickableItem(
                        text = stringResource(id = R.string.add_album),
                        iconResId = R.drawable.add,
                        position = RowPosition.Top,
                    ) {
                        showAlbumTypeDialog = true
                    }
                }

                val immichUploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
                val immichUploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()

                if (immichUploadTotal != 0) {
                    DialogClickableItem(
                        text = stringResource(id = R.string.immich_main_dialog_sync_state, immichUploadCount, immichUploadTotal),
                        iconResId = R.drawable.cloud_upload,
                        position = if (currentView.value == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                    )
                }

                DialogClickableItem(
                    text = stringResource(id = R.string.data_and_backup),
                    iconResId = R.drawable.data,
                    position = if (currentView.value == DefaultTabs.TabTypes.secure && immichUploadTotal == 0) RowPosition.Top else RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.DataAndBackup.name)
                }

                val showExtraSecureItem by mainViewModel.settings.LookAndFeel.getShowExtraSecureNav().collectAsStateWithLifecycle(initialValue = false)
                if (showExtraSecureItem) {
                    DialogClickableItem(
                        text = stringResource(id = R.string.secure_folder),
                        iconResId = R.drawable.secure_folder,
                        position = RowPosition.Middle,
                    ) {
                        showDialog.value = false
                        currentView.value = DefaultTabs.TabTypes.secure
                    }
                }

                DialogClickableItem(
                    text = stringResource(id = R.string.settings),
                    iconResId = R.drawable.settings,
                    position = RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.SettingsMainView.name)
                }

                DialogClickableItem(
                    text = stringResource(id = R.string.settings_about_and_updates),
                    iconResId = R.drawable.info,
                    position = RowPosition.Bottom,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.AboutAndUpdateView.name)
                }
            }
        }
    }
}

@Composable
fun FeatureNotAvailableDialog(onDismiss: () -> Unit) {
    ExplanationDialog(
        title = stringResource(id = R.string.not_available),
        explanation = stringResource(id = R.string.not_available_desc),
        onDismiss = onDismiss
    )
}