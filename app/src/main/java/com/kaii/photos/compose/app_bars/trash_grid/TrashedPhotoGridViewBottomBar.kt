package com.kaii.photos.compose.app_bars.trash_grid

import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleImages
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    val showRestoreDialog = remember { mutableStateOf(false) }
    val permissionState = rememberFilePermissionManager(
        onGranted = {
            context.appModule.scope.launch(Dispatchers.IO) {
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
        context.appModule.scope.launch(Dispatchers.IO) {
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