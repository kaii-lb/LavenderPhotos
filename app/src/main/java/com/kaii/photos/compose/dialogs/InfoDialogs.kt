package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MediaData
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.checkPathIsDownloads
import com.kaii.photos.helpers.eraseExifMedia
import com.kaii.photos.helpers.getDecryptCacheForFile
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.helpers.rememberMediaRenamer
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.immich.ImmichUserLoginState
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.models.main_activity.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "com.kaii.photos.compose.dialogs.InfoDialogs"

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

            val normalAlbums by mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
            val dynamicAlbum by remember {
                derivedStateOf {
                    normalAlbums.firstOrNull { it.id == album.id }
                }
            }

            var isPinned by remember(dynamicAlbum?.isPinned) { mutableStateOf(dynamicAlbum?.isPinned ?: false) }
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
                mainViewModel.settings.AlbumsList.removeFromAlbumsList(id = album.id)
                mainViewModel.settings.AlbumsList.addToAlbumsList(albumInfo = album.copy(isPinned = !isPinned))

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

            val alwaysShowInfo by mainViewModel.settings.Immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)
            val userInfo by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()
            if (userInfo is ImmichUserLoginState.IsLoggedIn || alwaysShowInfo) {
                MainDialogUserInfo()
            }

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

                val showExtraSecureItem by mainViewModel.settings.LookAndFeel.getShowExtraSecureNav()
                    .collectAsStateWithLifecycle(initialValue = false)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SinglePhotoInfoDialog(
    currentMediaItem: MediaStoreData,
    showMoveCopyOptions: Boolean,
    dismiss: () -> Unit
) {
    val mainViewModel = LocalMainViewModel.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

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
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = { WindowInsets.systemBars },
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

                val mediaData by getExifDataForMedia(currentMediaItem.absolutePath).collectAsStateWithLifecycle(initialValue = emptyMap(), context = Dispatchers.IO)

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
                    Text(
                        text = stringResource(id = R.string.media_tools),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                        val mediaRenamer = rememberMediaRenamer(uri = currentMediaItem.uri) {
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
                            uris = listOf(currentMediaItem.uri),
                            shouldRun = saveFileName,
                            onGranted = {
                                mediaRenamer.rename(
                                    newName = "${currentFileName}.${file.extension}",
                                    uri = currentMediaItem.uri
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
                            modifier = Modifier
                                .weight(1f)
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

                            val stateList = SnapshotStateList<MediaStoreData>()
                            stateList.add(currentMediaItem)

                            MoveCopyAlbumListView(
                                show = show,
                                selectedItemsList = stateList,
                                isMoving = isMoving,
                                groupedMedia = null,
                                insetsPadding = WindowInsets.statusBars
                            )

                            IconButton(
                                onClick = {
                                    isMoving = true
                                    show.value = true
                                },
                                modifier = Modifier
                                    .weight(1f)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.copy),
                                    contentDescription = "copy this media"
                                )
                            }
                        }

                        if (currentMediaItem.type == MediaType.Image) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(context, WallpaperSetter::class.java).apply {
                                        action = Intent.ACTION_SET_WALLPAPER
                                        data = currentMediaItem.uri
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        putExtra("mimeType", currentMediaItem.mimeType)
                                    }

                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.imagesearch_roller),
                                    contentDescription = "set as wallpaper"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.media_information),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        items(
                            items = mediaData.keys.toList()
                        ) { key ->
                            val value = mediaData[key]

                            val splitBy = Regex("(?=[A-Z])")
                            val split = key.toString().split(splitBy)
                            val name =
                                "${if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()}:"

                            TallDialogInfoRow(
                                title = name,
                                info = value.toString(),
                                icon = key.iconResInt,
                                position =
                                    if (mediaData.keys.indexOf(key) == mediaData.keys.size - 1)
                                        RowPosition.Bottom
                                    else if (mediaData.keys.indexOf(key) == 0)
                                        RowPosition.Top
                                    else
                                        RowPosition.Middle
                            ) {
                                val clipboardManager =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = ClipData.newPlainText(name, value.toString())
                                clipboardManager.setPrimaryClip(clipData)
                            }
                        }
                    }
                    val showConfirmEraseDialog = remember { mutableStateOf(false) }
                    val runEraseExifData = remember { mutableStateOf(false) }
                    ConfirmationDialogWithBody(
                        showDialog = showConfirmEraseDialog,
                        dialogTitle = stringResource(id = R.string.media_exif_erase),
                        dialogBody = stringResource(id = R.string.action_cannot_be_undone),
                        confirmButtonLabel = stringResource(id = R.string.media_erase)
                    ) {
                        runEraseExifData.value = true
                    }

                    val resources = LocalResources.current
                    GetPermissionAndRun(
                        uris = listOf(currentMediaItem.uri),
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SingleSecurePhotoInfoDialog(
    currentMediaItem: MediaStoreData,
    dismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

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
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = { WindowInsets.systemBars },
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                )
        ) {
            BackHandler(
                enabled = !WindowInsets.isImeVisible
            ) {
                coroutineScope.launch {
                    sheetState.hide()
                    dismiss()
                }
            }


            var showLoadingDialog by remember { mutableStateOf(false) }
            if (showLoadingDialog) {
                LoadingDialog(
                    title = stringResource(id = R.string.secure_getting_file_info),
                    body = stringResource(id = R.string.secure_please_wait)
                )
            }

            val context = LocalContext.current
            var mediaData by remember {
                mutableStateOf(
                    emptyMap<MediaData, Any>()
                )
            }
            LaunchedEffect(currentMediaItem) {
                withContext(Dispatchers.IO) {
                    showLoadingDialog = true

                    val file = if (currentMediaItem.type == MediaType.Video) {
                        val originalFile = File(currentMediaItem.absolutePath)
                        val cachedFile = getSecureDecryptedVideoFile(
                            name = currentMediaItem.displayName,
                            context = context
                        )

                        if (!cachedFile.exists()) {
                            val iv = currentMediaItem.bytes?.getIv()

                            if (iv == null) {
                                Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
                                return@withContext
                            }
                            EncryptionManager.decryptVideo(
                                absolutePath = originalFile.absolutePath,
                                iv = iv,
                                context = context,
                                progress = {}
                            )
                        } else if (cachedFile.length() < originalFile.length()) {
                            while (cachedFile.length() < originalFile.length()) {
                                delay(100)
                            }

                            cachedFile
                        } else {
                            cachedFile
                        }
                    } else {
                        val originalFile = File(currentMediaItem.absolutePath)
                        val cachedFile = getDecryptCacheForFile(
                            file = originalFile,
                            context = context
                        )

                        if (!cachedFile.exists()) {
                            val iv = currentMediaItem.bytes?.getIv()

                            if (iv == null) {
                                Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
                                return@withContext
                            }
                            EncryptionManager.decryptInputStream(
                                inputStream = originalFile.inputStream(),
                                outputStream = cachedFile.outputStream(),
                                iv = iv
                            )

                            cachedFile
                        } else if (cachedFile.length() < originalFile.length()) {
                            val threshold = 500
                            while (cachedFile.length() + threshold < originalFile.length()) {
                                delay(100)
                            }

                            cachedFile
                        } else {
                            cachedFile
                        }
                    }

                    showLoadingDialog = false
                    getExifDataForMedia(file.absolutePath).collect {
                        mediaData = it
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(top = 4.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.media_information),
                    fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    items(
                        items = mediaData.keys.toList()
                    ) { key ->
                        val value = mediaData[key]

                        val splitBy = Regex("(?=[A-Z])")
                        val split = key.toString().split(splitBy)
                        val name =
                            "${if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()}:"

                        TallDialogInfoRow(
                            title = name,
                            info = value.toString(),
                            icon = key.iconResInt,
                            position =
                                if (mediaData.keys.indexOf(key) == mediaData.keys.size - 1)
                                    RowPosition.Bottom
                                else if (mediaData.keys.indexOf(key) == 0)
                                    RowPosition.Top
                                else
                                    RowPosition.Middle
                        ) {
                            val clipboardManager =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText(name, value.toString())
                            clipboardManager.setPrimaryClip(clipData)
                        }
                    }
                }
            }
        }
    }
}