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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.lavender.immichintegration.state_managers.LoginStateManager
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.AnimatableTextField
import com.kaii.photos.compose.widgets.MainDialogUserInfo
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.checkPathIsDownloads
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.secure_folder.rememberSecureFolderLaunchManager
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.compose.dialogs.InfoDialogs"

@Composable
fun SingleAlbumDialog(
    showDialog: MutableState<Boolean>,
    albumId: Int,
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    itemCount: Int
) {
    val mainViewModel = LocalMainViewModel.current
    val albums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()
    val dynamicAlbum by remember {
        derivedStateOf {
            albums.first { it.id == albumId }
        }
    }

    if (showDialog.value) {
        LavenderDialogBase(
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
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
                    second = dynamicAlbum.name,
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
                selectedItemsList.add(
                    PhotoLibraryUIModel.Media(
                        item = MediaStoreData.dummyItem,
                        accessToken = null
                    )
                )
            }


            val context = LocalContext.current
            val fileName = remember { mutableStateOf(dynamicAlbum.name) }

            if (dynamicAlbum.paths.size == 1 && !dynamicAlbum.isCustomAlbum) {
                val permissionManager = rememberDirectoryPermissionManager(
                    onGranted = {
                        Log.d(TAG, "Running rename ${fileName.value} ${dynamicAlbum.name}")
                        if (fileName.value != dynamicAlbum.name) {
                            Log.d(TAG, "Running rename - passed first check")
                            val basePath = dynamicAlbum.mainPath.toBasePath()
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
                                absolutePath = dynamicAlbum.mainPath,
                                newName = fileName.value,
                                base = volumeName!!
                            )

                            val newInfo = dynamicAlbum.copy(
                                name = fileName.value,
                                paths = setOf(dynamicAlbum.mainPath.replace(dynamicAlbum.name, fileName.value))
                            )
                            mainViewModel.settings.AlbumsList.edit(
                                id = dynamicAlbum.id,
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
                        }
                    }
                )

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    rowPosition = RowPosition.Middle,
                    enabled = dynamicAlbum.paths.all { !it.checkPathIsDownloads() },
                    modifier = Modifier
                        .padding(8.dp, 0.dp),
                    doAction = {
                        permissionManager.start(
                            directories = dynamicAlbum.paths
                        )
                    },
                    resetAction = {
                        fileName.value = dynamicAlbum.name
                    }
                )
            } else {
                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    rowPosition = RowPosition.Middle,
                    enabled = dynamicAlbum.paths.all { !it.checkPathIsDownloads() },
                    modifier = Modifier
                        .padding(8.dp, 0.dp),
                    doAction = {
                        val newInfo = dynamicAlbum.copy(name = fileName.value)

                        mainViewModel.settings.AlbumsList.edit(
                            id = dynamicAlbum.id,
                            newInfo = newInfo
                        )
                    },
                    resetAction = {
                        fileName.value = dynamicAlbum.name
                    }
                )
            }

            DialogClickableItem(
                text = stringResource(id = R.string.albums_remove),
                iconResId = R.drawable.delete,
                position = RowPosition.Middle,
                enabled = !dynamicAlbum.mainPath.checkPathIsDownloads(),
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    )
                    .height(if (!isEditingFileName.value) 42.dp else 0.dp)
                    .padding(8.dp, 0.dp)
            ) {
                mainViewModel.settings.AlbumsList.remove(dynamicAlbum.id)
                showDialog.value = false

                try {
                    context.contentResolver.delete(
                        LavenderContentProvider.CONTENT_URI,
                        "${LavenderMediaColumns.PARENT_ID} = ?",
                        arrayOf("${dynamicAlbum.id}")
                    )

                    context.contentResolver.releasePersistableUriPermission(
                        context.getExternalStorageContentUriFromAbsolutePath(
                            dynamicAlbum.mainPath,
                            true
                        ),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Throwable) {
                    Log.d(TAG, "Couldn't release permission for ${dynamicAlbum.mainPath}")
                    e.printStackTrace()
                }

                navController.popBackStack()
            }

            DialogClickableItem(
                text =
                    if (dynamicAlbum.isPinned) stringResource(id = R.string.albums_unpin)
                    else stringResource(id = R.string.albums_pin),
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
                mainViewModel.settings.AlbumsList.edit(
                    id = dynamicAlbum.id,
                    newInfo = dynamicAlbum.copy(isPinned = !dynamicAlbum.isPinned)
                )
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
                        firstText = if (!dynamicAlbum.isCustomAlbum) stringResource(id = R.string.albums_path) else stringResource(
                            id = R.string.albums_id
                        ),
                        secondText = if (!dynamicAlbum.isCustomAlbum) dynamicAlbum.mainPath else dynamicAlbum.id.toString(),
                        iconResId = R.drawable.folder,
                    )

                    DialogInfoText(
                        firstText = stringResource(id = R.string.albums_item_count),
                        secondText = itemCount.toString(),
                        iconResId = R.drawable.data,
                    )

                    DialogInfoText(
                        firstText = stringResource(id = R.string.immich_uuid),
                        secondText = dynamicAlbum.immichId,
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
    pagerState: PagerState,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    mainViewModel: MainViewModel,
    loginState: LoginStateManager
) {
    val navController = LocalNavController.current
    val vibratorManager = rememberVibratorManager()
    val userInfo by loginState.state.collectAsStateWithLifecycle()
    val tabList by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = mainViewModel.settings.DefaultTabs.defaultTabList)

    val currentTab by remember {
        derivedStateOf {
            tabList[pagerState.currentPage]
        }
    }

    if (showDialog.value) {
        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            }
        ) {
            TitleCloseRow(
                title = currentTab.name,
                closeOffset = 8.dp,
            ) {
                showDialog.value = false
            }

            val alwaysShowInfo by mainViewModel.settings.Immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)
            if (userInfo is LoginState.LoggedIn || alwaysShowInfo) {
                MainDialogUserInfo(
                    loginState = userInfo,
                    uploadPfp = loginState::uploadPfp,
                    setUsername = loginState::updateUsername
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                if (currentTab != DefaultTabs.TabTypes.albums && currentTab != DefaultTabs.TabTypes.secure) {
                    DialogClickableItem(
                        text = stringResource(id = R.string.media_select),
                        iconResId = R.drawable.check_item,
                        position = RowPosition.Top,
                    ) {
                        showDialog.value = false
                        selectedItemsList.clear()
                        selectedItemsList.add(
                            PhotoLibraryUIModel.Media(
                                item = MediaStoreData.dummyItem,
                                accessToken = null
                            )
                        )
                        vibratorManager.vibrateShort()
                    }
                }

                if (currentTab == DefaultTabs.TabTypes.albums) {
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

                // TODO
                // val immichUploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
                // val immichUploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()

                // if (immichUploadTotal != 0) {
                //     DialogClickableItem(
                //         text = stringResource(id = R.string.immich_main_dialog_sync_state, immichUploadCount, immichUploadTotal),
                //         iconResId = R.drawable.cloud_upload,
                //         position = if (currentView.value == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                //     )
                // }

                DialogClickableItem(
                    text = stringResource(id = R.string.data_and_backup),
                    iconResId = R.drawable.data,
                    // position = if (currentView.value == DefaultTabs.TabTypes.secure && immichUploadTotal == 0) RowPosition.Top else RowPosition.Middle,
                    position = if (currentTab == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.DataAndBackup.name)
                }

                val showExtraSecureItem by mainViewModel.settings.LookAndFeel
                    .getShowExtraSecureNav()
                    .collectAsStateWithLifecycle(initialValue = false)

                if (showExtraSecureItem) {
                    val authManager = rememberSecureFolderLaunchManager()
                    DialogClickableItem(
                        text = stringResource(id = R.string.secure_folder),
                        iconResId = R.drawable.secure_folder,
                        position = RowPosition.Middle,
                    ) {
                        showDialog.value = false

                        authManager.authenticate()
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
fun SingleSecurePhotoInfoDialog(
    currentMediaItem: MediaStoreData,
    privacyMode: Boolean,
    dismiss: () -> Unit,
    togglePrivacyMode: () -> Unit
) {
    val isLandscape by rememberDeviceOrientation()
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
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
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

            // TODO
            // LaunchedEffect(currentMediaItem) {
            //     withContext(Dispatchers.IO) {
            //         showLoadingDialog = true
            //
            //         val file = if (currentMediaItem.type == MediaType.Video) {
            //             val originalFile = File(currentMediaItem.absolutePath)
            //             val cachedFile = getSecureDecryptedVideoFile(
            //                 name = currentMediaItem.displayName,
            //                 context = context
            //             )
            //
            //             if (!cachedFile.exists()) {
            //                 val iv = currentMediaItem.bytes?.getIv()
            //
            //                 if (iv == null) {
            //                     Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
            //                     return@withContext
            //                 }
            //                 EncryptionManager.decryptVideo(
            //                     absolutePath = originalFile.absolutePath,
            //                     iv = iv,
            //                     context = context,
            //                     progress = {}
            //                 )
            //             } else if (cachedFile.length() < originalFile.length()) {
            //                 while (cachedFile.length() < originalFile.length()) {
            //                     delay(100)
            //                 }
            //
            //                 cachedFile
            //             } else {
            //                 cachedFile
            //             }
            //         } else {
            //             val originalFile = File(currentMediaItem.absolutePath)
            //             val cachedFile = getDecryptCacheForFile(
            //                 file = originalFile,
            //                 context = context
            //             )
            //
            //             if (!cachedFile.exists()) {
            //                 val iv = currentMediaItem.bytes?.getIv()
            //
            //                 if (iv == null) {
            //                     Log.e(TAG, "IV for ${currentMediaItem.displayName} was null, aborting")
            //                     return@withContext
            //                 }
            //                 EncryptionManager.decryptInputStream(
            //                     inputStream = originalFile.inputStream(),
            //                     outputStream = cachedFile.outputStream(),
            //                     iv = iv
            //                 )
            //
            //                 cachedFile
            //             } else if (cachedFile.length() < originalFile.length()) {
            //                 val threshold = 500
            //                 while (cachedFile.length() + threshold < originalFile.length()) {
            //                     delay(100)
            //                 }
            //
            //                 cachedFile
            //             } else {
            //                 cachedFile
            //             }
            //         }
            //
            //         showLoadingDialog = false
            //         mediaData = getExifDataForMedia(
            //             context = context,
            //             inputStream = file.inputStream(),
            //             absolutePath = file.absolutePath,
            //             fallback = currentMediaItem.dateModified
            //         )
            //     }
            // }

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
                            icon = key.icon,
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
                    }
                }

                if (!isLandscape) {
                    TallDialogInfoRow(
                        title = stringResource(id = if (privacyMode) R.string.privacy_scroll_mode_enabled else R.string.privacy_scroll_mode_disabled),
                        info = "",
                        icon = if (!privacyMode) R.drawable.swipe else R.drawable.do_not_touch,
                        position = RowPosition.Single
                    ) {
                        togglePrivacyMode()
                    }
                }
            }
        }
    }
}

@Composable
fun TrashDeleteDialog(
    showDialog: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()

                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.media_delete),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.media_delete_permanently_confirm),
                    fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                )
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}