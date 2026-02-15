package com.kaii.photos.compose.app_bars

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
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
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.helpers.shareMultipleSecuredImages
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.permissions.favourites.rememberListFavouritesState
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

// private const val TAG = "com.kaii.photos.compose.app_bars.GridViewBars"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumViewTopBar(
    albumInfo: AlbumInfo,
    selectionManager: SelectionManager,
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

    val show by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

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

                                    mainViewModel.settings.albums.edit(
                                        id = dynamicAlbum.id,
                                        newInfo = newInfo
                                    )

                                    navController.popBackStack()
                                    navController.navigate(
                                        route =
                                            if (albumInfo.isCustomAlbum) {
                                                Screens.CustomAlbum.GridView(
                                                    albumInfo = newInfo
                                                )
                                            } else {
                                                Screens.Album.GridView(
                                                    albumInfo = newInfo
                                                )
                                            }
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
            IsSelectingTopBar(selectionManager = selectionManager)
        }
    }
}

@Composable
fun SingleAlbumViewBottomBar(
    albumInfo: AlbumInfo,
    selectionManager: SelectionManager,
    incomingIntent: Intent? = null
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            SelectingBottomBarItems(
                albumInfo = albumInfo,
                selectionManager = selectionManager
            )
        }
    } else {
        val context = LocalContext.current
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.toUri() },
            contentResolver = context.contentResolver
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    selectionManager: SelectionManager,
    deleteAll: () -> Unit,
    onBackClick: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    ConfirmationDialogWithBody(
        showDialog = showDialog,
        dialogTitle = stringResource(id = R.string.trash_empty),
        dialogBody = stringResource(id = R.string.trash_empty),
        confirmButtonLabel = stringResource(id = R.string.trash_empty_confirm),
        action = deleteAll
    )

    val show by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

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
            IsSelectingTopBar(selectionManager = selectionManager)
        }
    }
}

@Composable
fun TrashedPhotoGridViewBottomBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            TrashPhotoGridBottomBarItems(selectionManager = selectionManager)
        }
    } else {
        val context = LocalContext.current
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.toUri() },
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun TrashPhotoGridBottomBarItems(
    selectionManager: SelectionManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                shareMultipleImages(
                    uris = selectedItemsList.fastMap { it.toUri() },
                    context = context,
                    hasVideos = selectedItemsList.fastAny { !it.isImage }
                )
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.share),
            contentDescription = stringResource(id = R.string.media_share)
        )
    }

    val mainViewModel = LocalMainViewModel.current
    val showRestoreDialog = remember { mutableStateOf(false) }
    val permissionState = rememberFilePermissionManager(
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsList.fastMap { it.toUri() },
                    trashed = false
                )

                selectionManager.clear()
            }
        }
    )

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = stringResource(id = R.string.media_restore_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_restore)
    ) {
        permissionState.get(
            uris = selectedItemsList.map { it.toUri() }
        )
    }

    IconButton(
        onClick = {
            showRestoreDialog.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.untrash),
            contentDescription = stringResource(id = R.string.media_restore)
        )
    }

    val showPermaDeleteDialog = remember { mutableStateOf(false) }
    ConfirmationDialogWithBody(
        showDialog = showPermaDeleteDialog,
        dialogTitle = stringResource(id = R.string.media_delete_permanently_confirm),
        dialogBody = stringResource(id = R.string.action_cannot_be_undone),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        mainViewModel.launch(Dispatchers.IO) {
            permanentlyDeletePhotoList(
                context = context,
                list = selectedItemsList.fastMap { it.toUri() }
            )

            selectionManager.clear()
        }
    }

    IconButton(
        onClick = {
            if (selectedItemsList.isNotEmpty()) {
                showPermaDeleteDialog.value = true
            }
        },
        enabled = selectedItemsList.isNotEmpty()
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
    selectionManager: SelectionManager,
    onBackClicked: () -> Unit
) {
    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    AnimatedContent(
        targetState = isSelecting,
        transitionSpec = {
            getAppBarContentTransition(isSelecting)
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
            IsSelectingTopBar(selectionManager = selectionManager)
        }
    }
}

@Composable
fun SecureFolderViewBottomAppBar(
    selectionManager: SelectionManager,
    isGettingPermissions: MutableState<Boolean>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val resources = LocalResources.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

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
                        val items = selectedItemsList.toSecureMedia(context = context)

                        items.forEach { item ->
                            val iv = item.bytes?.getIv() ?: return@async

                            val originalFile = File(item.item.absolutePath)
                            val cachedFile = File(context.cacheDir, item.item.displayName)

                            EncryptionManager.decryptInputStream(
                                inputStream = originalFile.inputStream(),
                                outputStream = cachedFile.outputStream(),
                                iv = iv
                            )

                            cachedFile.deleteOnExit()
                            cachedPaths.add(Pair(cachedFile.absolutePath, item.item.type))
                        }

                        showLoadingDialog = false

                        shareMultipleSecuredImages(paths = cachedPaths, context = context)
                    }.await()
                }
            },
            enabled = selectedItemsList.isNotEmpty()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.share),
                contentDescription = stringResource(id = R.string.media_share)
            )
        }

        val mainViewModel = LocalMainViewModel.current
        val appDatabase = LocalAppDatabase.current
        val showRestoreDialog = remember { mutableStateOf(false) }

        val restorePermissionState = rememberDirectoryPermissionManager(
            onGranted = {
                mainViewModel.launch(Dispatchers.IO) {
                    moveImageOutOfLockedFolder(
                        list = selectedItemsList.toSecureMedia(context = context),
                        context = context,
                        applicationDatabase = appDatabase
                    ) {
                        selectionManager.clear()
                        showLoadingDialog = false
                        isGettingPermissions.value = false
                    }
                }
            },
            onRejected = {
                isGettingPermissions.value = false
                showLoadingDialog = false

                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.secure_restore_failed),
                            icon = R.drawable.unlock,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        )

        ConfirmationDialog(
            showDialog = showRestoreDialog,
            dialogTitle = stringResource(id = R.string.media_restore_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_restore)
        ) {
            loadingDialogTitle = resources.getString(R.string.media_restore_processing)
            showLoadingDialog = true

            isGettingPermissions.value = true

            restorePermissionState.start(
                directories = selectedItemsList.fastMap { it.parentPath.parent() }.distinct().toSet()
            )
        }

        IconButton(
            onClick = {
                showRestoreDialog.value = true
            },
            enabled = selectedItemsList.isNotEmpty()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.unlock),
                contentDescription = stringResource(id = R.string.media_restore)
            )
        }


        val showPermaDeleteDialog = remember { mutableStateOf(false) }
        ConfirmationDialogWithBody(
            showDialog = showPermaDeleteDialog,
            dialogTitle = stringResource(id = R.string.media_delete_permanently_confirm),
            dialogBody = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            mainViewModel.launch(Dispatchers.IO) {
                permanentlyDeleteSecureFolderImageList(
                    list = selectedItemsList.toSecureMedia(context = context).fastMap {
                        it.item.absolutePath
                    },
                    context = context
                )

                selectionManager.clear()
            }
        }

        IconButton(
            onClick = {
                showPermaDeleteDialog.value = true
            },
            enabled = selectedItemsList.isNotEmpty()
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
    selectionManager: SelectionManager,
    onBackClick: () -> Unit
) {
    val show by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

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
            IsSelectingTopBar(selectionManager = selectionManager)
        }
    }
}

@Composable
fun FavouritesViewBottomAppBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            FavouritesBottomAppBarItems(selectionManager = selectionManager)
        }
    } else {
        val context = LocalContext.current
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.toUri() },
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun FavouritesBottomAppBarItems(
    selectionManager: SelectionManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                shareMultipleImages(
                    uris = selectedItemsList.fastMap { it.toUri() },
                    context = context,
                    hasVideos = selectedItemsList.fastAny { !it.isImage }
                )
            }
        },
        enabled = selectedItemsList.isNotEmpty()
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
        insetsPadding = WindowInsets.statusBars,
        clear = {}
    )

    IconButton(
        onClick = {
            show.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = stringResource(id = R.string.media_copy)
        )
    }

    val showUnFavDialog = remember { mutableStateOf(false) }
    val favState = rememberListFavouritesState {
        selectionManager.clear()
    }

    ConfirmationDialog(
        showDialog = showUnFavDialog,
        dialogTitle = stringResource(id = R.string.favourites_remove_this),
        confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
    ) {
        coroutineScope.launch {
            favState.setFavourite(
                favourite = false,
                list = selectedItemsList.fastMap { it.toUri() }
            )
        }
    }

    IconButton(
        onClick = {
            showUnFavDialog.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.unfavourite),
            contentDescription = stringResource(id = R.string.custom_album_remove_media)
        )
    }

    val mainViewModel = LocalMainViewModel.current
    val showDeleteDialog = remember { mutableStateOf(false) }

    val doNotTrash by mainViewModel.settings.permissions
        .getDoNotTrash()
        .collectAsStateWithLifecycle(initialValue = true)

    val confirmToDelete by mainViewModel.settings.permissions
        .getConfirmToDelete()
        .collectAsStateWithLifecycle(initialValue = true)


    val permissionState = rememberFilePermissionManager(
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                if (doNotTrash) {
                    permanentlyDeletePhotoList(
                        context = context,
                        list = selectedItemsList.fastMap { it.toUri() }
                    )
                } else {
                    setTrashedOnPhotoList(
                        context = context,
                        list = selectedItemsList.fastMap { it.toUri() },
                        trashed = true
                    )
                }

                selectionManager.clear()
            }
        }
    )

    ConfirmationDialog(
        showDialog = showDeleteDialog,
        dialogTitle = stringResource(id = if (doNotTrash) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_delete)
    ) {
        coroutineScope.launch {
            // TODO: bulk delete
            // selectedItemsWithoutSection.forEach {
            //     dao.deleteEntityById(it.id)
            // }
            permissionState.get(
                uris = selectedItemsList.fastMap { it.toUri() }
            )
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete) {
                showDeleteDialog.value = true
            } else {
                coroutineScope.launch {
                    // TODO: bulk delete
                    // selectedItemsWithoutSection.forEach {
                    //     dao.deleteEntityById(it.id)
                    // }
                    permissionState.get(
                        uris = selectedItemsList.fastMap { it.toUri() }
                    )
                }
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = stringResource(id = R.string.media_delete)
        )
    }
}