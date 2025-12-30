package com.kaii.photos.compose.app_bars

import android.util.Log
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
import androidx.compose.runtime.State
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.widgets.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.widgets.SelectViewTopBarRightButtons
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.compose.app_bars.SelectingBars"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    mediaCount: State<Int>,
    sectionCount: State<Int>,
    getAllMedia: () -> List<MediaStoreData>
) {
    TopAppBar(
        title = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        actions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount,
                getAllMedia = getAllMedia
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
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
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section && it != MediaStoreData.dummyItem
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
            contentDescription = "share this image"
        )
    }

    val show = remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }
    MoveCopyAlbumListView(
        show = show,
        selectedItemsList = selectedItemsList,
        isMoving = isMoving,
        groupedMedia = null,
        insetsPadding = WindowInsets.statusBars
    )

    IconButton(
        onClick = {
            isMoving = true
            show.value = true
        },
        enabled = !albumInfo.isCustomAlbum
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
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = "copy this image"
        )
    }

    val showDeleteDialog = remember { mutableStateOf(false) }
    val runDeleteAction = remember { mutableStateOf(false) }

    val mainViewModel = LocalMainViewModel.current
    val applicationDatabase = LocalAppDatabase.current
    val doNotTrash by mainViewModel.settings.Permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)

    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.fastMapNotNull { it.uri },
        shouldRun = runDeleteAction,
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

    val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete()
        .collectAsStateWithLifecycle(initialValue = true)

    if (!albumInfo.isCustomAlbum) {
        ConfirmationDialog(
            showDialog = showDeleteDialog,
            dialogTitle = stringResource(id = if (doNotTrash) R.string.media_delete_permanently_confirm else R.string.media_trash_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            runDeleteAction.value = true
        }
    } else {
        var showExplanationDialog by remember { mutableStateOf(false) }
        if (showExplanationDialog) {
            ExplanationDialog(
                title = stringResource(id = R.string.custom_album_media_not_custom_title),
                explanation = stringResource(id = R.string.custom_album_media_not_custom_explanation)
            ) {
                showExplanationDialog = false
            }
        }

        ConfirmationDialog(
            showDialog = showDeleteDialog,
            dialogTitle = stringResource(id = R.string.custom_album_remove_media_desc),
            confirmButtonLabel = stringResource(id = R.string.custom_album_remove_media)
        ) {
            if (selectedItemsWithoutSection.any { it.customId == null }) {
                showExplanationDialog = true
            }

            mainViewModel.launch(Dispatchers.IO) {
                selectedItemsWithoutSection.forEach { item ->
                    Log.d(
                        TAG,
                        "Removed this many rows: " + context.contentResolver.delete(
                            LavenderContentProvider.CONTENT_URI,
                            "${LavenderMediaColumns.ID} = ? AND ${LavenderMediaColumns.PARENT_ID} = ?",
                            arrayOf(item.customId.toString(), albumInfo.id.toString())
                        )
                    )
                }
                selectedItemsList.clear()
            }
        }
    }

    IconButton(
        onClick = {
            if (confirmToDelete) showDeleteDialog.value = true
            else runDeleteAction.value = true
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete),
            contentDescription = "Delete this image"
        )
    }

    val moveToSecureFolder = remember { mutableStateOf(false) }
    val tryGetDirPermission = remember { mutableStateOf(false) }

    var showLoadingDialog by remember { mutableStateOf(false) }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = selectedItemsWithoutSection.fastMap {
            it.absolutePath.getParentFromPath()
        }.fastDistinctBy {
            it
        },
        shouldRun = tryGetDirPermission,
        onGranted = {
            showLoadingDialog = true
            moveToSecureFolder.value = true
        },
        onRejected = {}
    )

    if (showLoadingDialog) {
        LoadingDialog(
            title = stringResource(id = R.string.secure_encrypting),
            body = stringResource(id = R.string.secure_processing)
        )
    }

    val appDatabase = LocalAppDatabase.current
    GetPermissionAndRun(
        uris = selectedItemsWithoutSection.map { it.uri },
        shouldRun = moveToSecureFolder,
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                moveImageToLockedFolder(
                    list = selectedItemsWithoutSection,
                    context = context,
                    applicationDatabase = appDatabase
                ) {
                    selectedItemsList.clear()
                    showLoadingDialog = false
                }
            }
        }
    )

    IconButton(
        onClick = {
            if (selectedItemsWithoutSection.isNotEmpty()) tryGetDirPermission.value = true
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.secure_folder),
            contentDescription = "Secure this media"
        )
    }
}