package com.kaii.photos.compose.app_bars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.widgets.SelectViewTopBarLeftButtons
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.moveMediaToSecureFolder
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(
    selectionManager: SelectionManager
) {
    TopAppBar(
        title = {
            SelectViewTopBarLeftButtons(selectionManager = selectionManager)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IsSelectingBottomAppBar(
    items: @Composable (RowScope.() -> Unit)
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(all = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = false,
            collapsedShadowElevation = 12.dp,
            expandedShadowElevation = 12.dp,
            scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                exitDirection = FloatingToolbarExitDirection.Bottom
            )
        ) {
            items()
        }
    }
}

@Composable
fun SelectingBottomBarItems(
    albumInfo: AlbumInfo,
    selectionManager: SelectionManager
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch(Dispatchers.IO) {
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
            contentDescription = "share this image"
        )
    }

    val show = remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }
    MoveCopyAlbumListView(
        show = show,
        selectedItemsList = selectedItemsList,
        isMoving = isMoving,
        clear = selectionManager::clear,
        insetsPadding = WindowInsets.statusBars
    )

    IconButton(
        onClick = {
            isMoving = true
            show.value = true
        },
        enabled = !albumInfo.isCustomAlbum && !selectedItemsList.isEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.cut),
            contentDescription = "move this image"
        )
    }

    IconButton(
        onClick = {
            isMoving = false
            show.value = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = "copy this image"
        )
    }

    val mainViewModel = LocalMainViewModel.current
    val doNotTrash by mainViewModel.settings.permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)

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

    val confirmToDelete by mainViewModel.settings.permissions
        .getConfirmToDelete()
        .collectAsStateWithLifecycle(initialValue = true)

    val showDeleteDialog = remember { mutableStateOf(false) }
    if (!albumInfo.isCustomAlbum) {
        ConfirmationDialog(
            showDialog = showDeleteDialog,
            dialogTitle = stringResource(id = if (doNotTrash) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            permissionState.get(
                uris = selectedItemsList.fastMap { it.toUri() }
            )
        }
    } else {
        ConfirmationDialog(
            showDialog = showDeleteDialog,
            dialogTitle = stringResource(id = R.string.custom_album_remove_media_desc),
            confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
        ) {
            mainViewModel.launch(Dispatchers.IO) {
                MediaDatabase.getInstance(context)
                    .customDao()
                    .deleteAll(
                        ids = selectedItemsList.fastMap { it.id }.toSet(),
                        album = albumInfo.id
                    )

                selectionManager.clear()
            }
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete) {
                showDeleteDialog.value = true
            } else if (!albumInfo.isCustomAlbum) {
                permissionState.get(
                    uris = selectedItemsList.fastMap { it.toUri() }
                )
            } else {
                mainViewModel.launch(Dispatchers.IO) {
                    MediaDatabase.getInstance(context)
                        .customDao()
                        .deleteAll(
                            ids = selectedItemsList.fastMap { it.id }.toSet(),
                            album = albumInfo.id
                        )

                    selectionManager.clear()
                }
            }
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = "Delete this image"
        )
    }

    var showLoadingDialog by remember { mutableStateOf(false) }
    if (showLoadingDialog) {
        LoadingDialog(
            title = stringResource(id = R.string.secure_encrypting),
            body = stringResource(id = R.string.secure_processing)
        )
    }

    val appDatabase = LocalAppDatabase.current
    val filePermissionState = rememberFilePermissionManager(
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                moveMediaToSecureFolder(
                    list = selectedItemsList,
                    context = context,
                    applicationDatabase = appDatabase
                ) {
                    selectionManager.clear()
                    showLoadingDialog = false
                }
            }
        }
    )

    val dirPermissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            showLoadingDialog = true
            filePermissionState.get(
                uris = selectedItemsList.map { it.toUri() }
            )
        }
    )

    IconButton(
        onClick = {
            if (selectedItemsList.isNotEmpty()) {
                dirPermissionManager.start(
                    directories = selectedItemsList.fastMapNotNull {
                        context.contentResolver.getAbsolutePathFromUri(it.toUri())?.parent()
                    }.fastDistinctBy {
                        it
                    }.toSet()
                )
            }
        },
        enabled = selectedItemsList.isNotEmpty() && !albumInfo.isCustomAlbum
    ) {
        Icon(
            painter = painterResource(id = R.drawable.secure_folder),
            contentDescription = "Secure this media"
        )
    }
}