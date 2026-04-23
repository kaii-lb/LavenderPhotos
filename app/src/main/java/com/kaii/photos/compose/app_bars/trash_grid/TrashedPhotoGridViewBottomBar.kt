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
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.launch

@Composable
fun TrashedPhotoGridViewBottomBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    process: (action: GenericFileManager.Action) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            TrashPhotoGridBottomBarItems(
                selectionManager = selectionManager,
                process = process
            )
        }
    } else {
        val context = LocalContext.current
        val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            uris = selectedItemsList.fastMap { it.uri.toUri() }, // TODO: move to file manager
            contentResolver = context.contentResolver
        )
    }
}

@Composable
fun TrashPhotoGridBottomBarItems(
    selectionManager: SelectionManager,
    process: (action: GenericFileManager.Action) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                process(
                    GenericFileManager.Action.Share(
                        list = selectedItemsList
                    )
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
            process(
                GenericFileManager.Action.Trash(
                    list = selectedItemsList,
                    trashed = false
                )
            )

            selectionManager.clear()
        }
    )

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = stringResource(id = R.string.media_restore_confirm),
        confirmButtonLabel = stringResource(id = R.string.media_restore)
    ) {
        permissionState.get(
            uris = selectedItemsList.map { it.uri.toUri() }
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
        process(
            GenericFileManager.Action.Delete(
                list = selectedItemsList
            )
        )

        selectionManager.clear()
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