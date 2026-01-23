package com.kaii.photos.compose.app_bars

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.dialogs.AlbumPathsDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.permissions.favourites.rememberListFavouritesState
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.helpers.shareMultipleSecuredImages
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getOriginalPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "com.kaii.photos.compose.app_bars.GridViewBars"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumViewTopBar(
    albumInfo: AlbumInfo,
    media: State<List<MediaStoreData>>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    showDialog: MutableState<Boolean>,
    isMediaPicker: Boolean = false,
    onBackClick: () -> Unit
) {
    val mainViewModel = LocalMainViewModel.current
    val normalAlbums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            normalAlbums.firstOrNull { it.id == albumInfo.id } ?: albumInfo
        }
    }

    val show by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val mediaCount = remember {
        derivedStateOf {
            media.value.filter {
                it.type != MediaType.Section
            }.size
        }
    }
    val sectionCount = remember {
        derivedStateOf {
            media.value.size - mediaCount.value
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "SingleAlbumViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            val mainViewModel = LocalMainViewModel.current
            val navController = LocalNavController.current

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = "Go back to previous page",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = dynamicAlbum.name,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    if (!isMediaPicker && !dynamicAlbum.isCustomAlbum) {
                        var showPathsDialog by remember { mutableStateOf(false) }

                        if (showPathsDialog) {
                            AlbumPathsDialog(
                                albumInfo = dynamicAlbum,
                                onConfirm = { selectedPaths ->
                                    val newInfo =
                                        dynamicAlbum.copy(
                                            id = dynamicAlbum.id,
                                            paths = selectedPaths
                                        )

                                    mainViewModel.settings.AlbumsList.edit(
                                        id = dynamicAlbum.id,
                                        newInfo = newInfo
                                    )

                                    navController.popBackStack()
                                    navController.navigate(
                                        Screens.SingleAlbumView(
                                            albumInfo = newInfo
                                        )
                                    )
                                },
                                onDismiss = {
                                    showPathsDialog = false
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                showPathsDialog = true
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = "show more options for the album view",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }
                    }

                    // TODO
                    // if (!isMediaPicker) {
                    //     val userInfo by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()
                    //
                    //     // TODO: rework
                    //     if (userInfo is ImmichUserLoginState.IsLoggedIn) {
                    //         var loadingBackupState by remember { mutableStateOf(false) }
                    //         val albumState by immichViewModel.immichAlbumsSyncState.collectAsStateWithLifecycle()
                    //
                    //         var deviceBackupMedia by remember { mutableStateOf(emptyList<ImmichBackupMedia>()) }
                    //
                    //         LaunchedEffect(media) {
                    //             loadingBackupState = true
                    //             withContext(Dispatchers.IO) {
                    //                 deviceBackupMedia = getImmichBackupMedia(
                    //                     groupedMedia = media.value,
                    //                     cancellationSignal = CancellationSignal()
                    //                 )
                    //
                    //                 immichViewModel.checkSyncStatus(
                    //                     immichAlbumId = albumInfo.immichId,
                    //                     expectedBackupMedia = deviceBackupMedia.toSet()
                    //                 )
                    //                 immichViewModel.refreshDuplicateState(
                    //                     immichId = albumInfo.immichId,
                    //                     media = deviceBackupMedia.toSet()
                    //                 ) {
                    //                     loadingBackupState = false
                    //                 }
                    //             }
                    //         }
                    //
                    //         val isBackedUp by remember(albumState) {
                    //             derivedStateOf {
                    //                 if (albumInfo.immichId.isEmpty()) null
                    //                 else if (albumState[albumInfo.immichId] is ImmichAlbumSyncState.InSync) true
                    //                 else if (albumState[albumInfo.immichId] is ImmichAlbumSyncState.OutOfSync) false
                    //                 else null
                    //             }
                    //         }
                    //
                    //         Box(
                    //             modifier = Modifier
                    //                 .wrapContentSize()
                    //         ) {
                    //             val navController = LocalNavController.current
                    //             IconButton(
                    //                 onClick = {
                    //                     navController.navigate(Screens.ImmichAlbumPage(albumInfo))
                    //                 },
                    //                 enabled = !loadingBackupState
                    //             ) {
                    //                 Icon(
                    //                     painter =
                    //                         when (isBackedUp) {
                    //                             true -> painterResource(id = R.drawable.cloud_done)
                    //                             false -> painterResource(id = R.drawable.cloud_upload)
                    //                             else -> painterResource(id = R.drawable.cloud_off)
                    //                         },
                    //                     contentDescription = "show more options for the album view",
                    //                     tint = if (!loadingBackupState) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                    //                         alpha = 0.5f
                    //                     ),
                    //                     modifier = Modifier
                    //                         .size(24.dp)
                    //                 )
                    //             }
                    //
                    //             if (loadingBackupState) {
                    //                 CircularProgressIndicator(
                    //                     color = MaterialTheme.colorScheme.primary,
                    //                     strokeWidth = 2.dp,
                    //                     strokeCap = StrokeCap.Round,
                    //                     modifier = Modifier
                    //                         .size(12.dp)
                    //                         .align(Alignment.BottomEnd)
                    //                         .offset(x = (-8).dp, y = (-10).dp)
                    //                 )
                    //             }
                    //         }
                    //     }
                    // }

                    IconButton(
                        onClick = {
                            showDialog.value = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the album view",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount,
                getAllMedia = { media.value }
            )
        }
    }
}

@Composable
fun SingleAlbumViewBottomBar(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    incomingIntent: Intent? = null
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            SelectingBottomBarItems(
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList
            )
        }
    } else {
        val context = LocalContext.current

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            selectedItemsList = selectedItemsList,
            contentResolver = context.contentResolver
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: State<List<MediaStoreData>>,
    onBackClick: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    val runEmptyTrashAction = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(runEmptyTrashAction.value) {
        if (runEmptyTrashAction.value) {
            permanentlyDeletePhotoList(
                context = context,
                list = groupedMedia.value.filter { it.type != MediaType.Section }.map { it.uri }
            )

            runEmptyTrashAction.value = false
        }
    }

    ConfirmationDialogWithBody(
        showDialog = showDialog,
        dialogTitle = stringResource(id = R.string.trash_empty),
        dialogBody = stringResource(id = R.string.trash_empty),
        confirmButtonLabel = stringResource(id = R.string.trash_empty_confirm)
    ) {
        runEmptyTrashAction.value = true
    }

    val show by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val mediaCount = remember {
        derivedStateOf {
            groupedMedia.value.filter {
                it.type != MediaType.Section
            }.size
        }
    }
    val sectionCount = remember {
        derivedStateOf {
            groupedMedia.value.size - mediaCount.value
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "TrashedPhotoGridViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.trash_bin),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showDialog.value = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = stringResource(id = R.string.trash_empty_desc),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount,
                getAllMedia = { groupedMedia.value }
            )
        }
    }
}

@Composable
fun TrashedPhotoGridViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    incomingIntent: Intent?
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            TrashPhotoGridBottomBarItems(selectedItemsList = selectedItemsList)
        }
    } else {
        val context = LocalContext.current

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            selectedItemsList = selectedItemsList,
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun TrashPhotoGridBottomBarItems(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section && it != MediaStoreData()
            }
        }
    }

    IconButton(
        onClick = {
            coroutineScope.launch {
                shareMultipleImages(
                    uris = selectedItemsWithoutSection.map { it.uri },
                    context = context,
                    hasVideos = selectedItemsWithoutSection.any { it.type == MediaType.Video }
                )
            }
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.share),
            contentDescription = stringResource(id = R.string.media_share)
        )
    }

    val showRestoreDialog = remember { mutableStateOf(false) }
    val runRestoreAction = remember { mutableStateOf(false) }

    val mainViewModel = LocalMainViewModel.current
    val applicationDatabase = LocalAppDatabase.current
    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.map { it.uri },
        shouldRun = runRestoreAction,
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsWithoutSection,
                    trashed = false,
                    appDatabase = applicationDatabase
                )

                selectedItemsList.clear()
            }
        }
    )

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = stringResource(id = R.string.media_restore_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_restore)
    ) {
        runRestoreAction.value = true
    }

    IconButton(
        onClick = {
            showRestoreDialog.value = true
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.untrash),
            contentDescription = stringResource(id = R.string.media_restore)
        )
    }

    val showPermaDeleteDialog = remember { mutableStateOf(false) }
    val runPermaDeleteAction = remember { mutableStateOf(false) }

    LaunchedEffect(runPermaDeleteAction.value) {
        if (runPermaDeleteAction.value) {
            permanentlyDeletePhotoList(
                context,
                selectedItemsWithoutSection.map { it.uri }
            )

            selectedItemsList.clear()

            runPermaDeleteAction.value = false
        }
    }

    ConfirmationDialogWithBody(
        showDialog = showPermaDeleteDialog,
        dialogTitle = stringResource(id = R.string.media_delete_permanently_confirm),
        dialogBody = stringResource(id = R.string.action_cannot_be_undone),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        runPermaDeleteAction.value = true
    }

    IconButton(
        onClick = {
            if (selectedItemsWithoutSection.isNotEmpty()) {
                showPermaDeleteDialog.value = true
            }
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = stringResource(id = R.string.media_delete)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderViewTopAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    media: State<List<MediaStoreData>>,
    onBackClicked: () -> Unit
) {
    val show by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val mediaCount = remember {
        derivedStateOf {
            media.value.filter {
                it.type != MediaType.Section
            }.size
        }
    }
    val sectionCount = remember {
        derivedStateOf {
            media.value.size - mediaCount.value
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "SecureFolderGridViewBottomBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { onBackClicked() },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.secure_folder),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                }
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount,
                getAllMedia = { media.value }
            )
        }
    }
}

@Composable
fun SecureFolderViewBottomAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>,
    isGettingPermissions: MutableState<Boolean>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val resources = LocalResources.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section
                }
            }
        }

        var showLoadingDialog by remember { mutableStateOf(false) }
        var loadingDialogTitle by remember { mutableStateOf(resources.getString(R.string.secure_decrypting)) }

        if (showLoadingDialog) {
            LoadingDialog(
                title = loadingDialogTitle,
                body = stringResource(id = R.string.secure_processing)
            )
        }

        IconButton(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    async {
                        loadingDialogTitle = resources.getString(R.string.secure_decrypting)
                        showLoadingDialog = true

                        val cachedPaths = emptyList<Pair<String, MediaType>>().toMutableList()

                        selectedItemsWithoutSection.forEach { item ->
                            val iv = item.bytes?.getIv()
                            if (iv == null) {
                                Log.e(TAG, "IV for ${item.displayName} was null, aborting decrypt")
                                return@async
                            }

                            val originalFile = File(item.absolutePath)
                            val cachedFile = File(context.cacheDir, item.displayName)

                            EncryptionManager.decryptInputStream(
                                inputStream = originalFile.inputStream(),
                                outputStream = cachedFile.outputStream(),
                                iv = iv
                            )

                            cachedFile.deleteOnExit()
                            cachedPaths.add(Pair(cachedFile.absolutePath, item.type))
                        }

                        showLoadingDialog = false

                        shareMultipleSecuredImages(paths = cachedPaths, context = context)
                    }.await()
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.share),
                contentDescription = stringResource(id = R.string.media_share)
            )
        }

        val showRestoreDialog = remember { mutableStateOf(false) }
        val runRestoreAction = remember { mutableStateOf(false) }
        val restoredFilesDir = remember { context.appRestoredFilesDir }

        val mainViewModel = LocalMainViewModel.current
        val applicationDatabase = LocalAppDatabase.current
        GetDirectoryPermissionAndRun(
            absoluteDirPaths =
                selectedItemsWithoutSection.fastMap {
                    it.bytes?.getOriginalPath()?.getParentFromPath() ?: restoredFilesDir
                }.fastDistinctBy {
                    it
                },
            shouldRun = runRestoreAction,
            onGranted = { grantedList ->
                mainViewModel.launch(Dispatchers.IO) {
                    val hasPermission = selectedItemsWithoutSection.fastFilter { selected ->
                        (selected.bytes?.getOriginalPath()?.getParentFromPath()
                            ?: restoredFilesDir) in grantedList
                    }

                    val newList = groupedMedia.value.toMutableList()

                    moveImageOutOfLockedFolder(
                        list = hasPermission,
                        context = context,
                        applicationDatabase = applicationDatabase
                    ) {
                        showLoadingDialog = false
                        isGettingPermissions.value = false
                    }

                    newList.removeAll(selectedItemsList.fastFilter { selected ->
                        selected.section in hasPermission.fastMap { it.section }
                    }.toSet())

                    selectedItemsList.clear()
                    groupedMedia.value = newList
                }
            },
            onRejected = {
                isGettingPermissions.value = false
                showLoadingDialog = false
            }
        )

        ConfirmationDialog(
            showDialog = showRestoreDialog,
            dialogTitle = stringResource(id = R.string.media_restore_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_restore)
        ) {
            loadingDialogTitle =
                resources.getString(R.string.media_restore_processing)
            showLoadingDialog = true

            isGettingPermissions.value = true
            runRestoreAction.value = true
        }

        IconButton(
            onClick = {
                showRestoreDialog.value = true
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.unlock),
                contentDescription = stringResource(id = R.string.media_restore)
            )
        }

        val showPermaDeleteDialog = remember { mutableStateOf(false) }
        val runPermaDeleteAction = remember { mutableStateOf(false) }

        LaunchedEffect(runPermaDeleteAction.value) {
            if (runPermaDeleteAction.value) {
                loadingDialogTitle = "Deleting Files"
                showLoadingDialog = true

                mainViewModel.launch(Dispatchers.IO) {
                    val newList = groupedMedia.value.toMutableList()

                    permanentlyDeleteSecureFolderImageList(
                        list = selectedItemsWithoutSection.map { it.absolutePath },
                        context = context
                    )


                    selectedItemsWithoutSection.forEach {
                        newList.remove(it)
                    }

                    newList.filter {
                        it.type == MediaType.Section
                    }.forEach { item ->
                        // remove sections which no longer have any children
                        val filtered = newList.filter { newItem ->
                            newItem.getLastModifiedDay() == item.getLastModifiedDay()
                        }

                        if (filtered.size == 1) newList.remove(item)
                    }

                    selectedItemsList.clear()
                    groupedMedia.value = newList

                    showLoadingDialog = false
                    runPermaDeleteAction.value = false
                }
            }
        }

        ConfirmationDialogWithBody(
            showDialog = showPermaDeleteDialog,
            dialogTitle = stringResource(id = R.string.media_delete_permanently_confirm),
            dialogBody = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            runPermaDeleteAction.value = true
        }

        IconButton(
            onClick = {
                showPermaDeleteDialog.value = true
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = stringResource(id = R.string.media_delete)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesViewTopAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    media: State<List<MediaStoreData>>,
    onBackClick: () -> Unit
) {
    val show by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val mediaCount = remember {
        derivedStateOf {
            media.value.filter {
                it.type != MediaType.Section
            }.size
        }
    }
    val sectionCount = remember {
        derivedStateOf {
            media.value.size - mediaCount.value
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "FavouritesGridViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { onBackClick() },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.favourites),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                }
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount,
                getAllMedia = { media.value }
            )
        }
    }
}

@Composable
fun FavouritesViewBottomAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    incomingIntent: Intent?
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            FavouritesBottomAppBarItems(
                selectedItemsList = selectedItemsList
            )
        }
    } else {
        val context = LocalContext.current

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            selectedItemsList = selectedItemsList,
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun FavouritesBottomAppBarItems(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val applicationDatabase = LocalAppDatabase.current
    val dao = applicationDatabase.favouritedItemEntityDao()

    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section
            }
        }
    }

    IconButton(
        onClick = {
            coroutineScope.launch {
                shareMultipleImages(
                    uris = selectedItemsWithoutSection.map { it.uri },
                    context = context,
                    hasVideos = selectedItemsWithoutSection.any { it.type == MediaType.Video }
                )
            }
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.share),
            contentDescription = stringResource(id = R.string.media_share)
        )
    }

    val show = remember { mutableStateOf(false) }
    MoveCopyAlbumListView(
        show = show,
        selectedItemsList = selectedItemsList,
        isMoving = false,
        groupedMedia = null,
        insetsPadding = WindowInsets.statusBars
    )

    IconButton(
        onClick = {
            show.value = true
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = stringResource(id = R.string.media_copy)
        )
    }

    val showUnFavDialog = remember { mutableStateOf(false) }
    val favState = rememberListFavouritesState {
        selectedItemsList.clear()
    }

    ConfirmationDialog(
        showDialog = showUnFavDialog,
        dialogTitle = stringResource(id = R.string.favourites_remove_this),
        confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
    ) {
        coroutineScope.launch {
            favState.setFavourite(
                favourite = false,
                list = selectedItemsWithoutSection.map { it.uri }
            )
        }
    }

    IconButton(
        onClick = {
            showUnFavDialog.value = true
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.unfavourite),
            contentDescription = stringResource(id = R.string.custom_album_remove_media)
        )
    }

    val showDeleteDialog = remember { mutableStateOf(false) }
    val runTrashAction = remember { mutableStateOf(false) }
    val mainViewModel = LocalMainViewModel.current

    val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete()
        .collectAsStateWithLifecycle(initialValue = true)
    val doNotTrash by mainViewModel.settings.Permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)

    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.map { it.uri },
        shouldRun = runTrashAction,
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                if (doNotTrash) {
                    permanentlyDeletePhotoList(
                        context = context,
                        list = selectedItemsWithoutSection.fastMap { it.uri }
                    )
                } else {
                    setTrashedOnPhotoList(
                        context = context,
                        list = selectedItemsWithoutSection,
                        trashed = true,
                        appDatabase = applicationDatabase
                    )
                }

                selectedItemsList.clear()
            }
        }
    )

    ConfirmationDialog(
        showDialog = showDeleteDialog,
        dialogTitle = stringResource(id = if (doNotTrash) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        coroutineScope.launch {
            selectedItemsList.forEach {
                dao.deleteEntityById(it.id)
            }
            runTrashAction.value = true
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete) {
                showDeleteDialog.value = true
            } else {
                coroutineScope.launch {
                    selectedItemsList.forEach {
                        dao.deleteEntityById(it.id)
                    }
                    runTrashAction.value = true
                }
            }
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = stringResource(id = R.string.media_delete)
        )
    }
}