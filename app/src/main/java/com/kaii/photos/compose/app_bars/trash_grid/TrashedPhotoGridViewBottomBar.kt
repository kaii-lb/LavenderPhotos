package com.kaii.photos.compose.app_bars.trash_grid

import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.toFileOperationMetadataItems
import com.kaii.photos.helpers.grid_management.SelectionManager
import kotlinx.coroutines.launch

@Composable
fun TrashedPhotoGridViewBottomBar(
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    runAction: (action: FileOperationAction) -> Unit
) {
    if (incomingIntent == null) {
        IsSelectingBottomAppBar {
            TrashPhotoGridBottomBarItems(
                selectionManager = selectionManager,
                runAction = runAction
            )
        }
    } else {
        MediaPickerConfirmButton(
            incomingIntent = incomingIntent,
            selectionManager = selectionManager
        )
    }
}

@Composable
fun TrashPhotoGridBottomBarItems(
    selectionManager: SelectionManager,
    runAction: (action: FileOperationAction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    IconButton(
        onClick = {
            coroutineScope.launch {
                runAction(
                    FileOperationAction.Share(
                        files = selectedItemsList.toFileOperationMetadataItems()
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

    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showRestoreDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.media_restore_confirm),
            confirmButtonLabel = stringResource(id = R.string.media_restore),
            action = {
                runAction(
                    FileOperationAction.Trash(
                        files = selectedItemsList.toFileOperationMetadataItems(),
                        isTrashed = false,
                        album = AlbumType.PlaceHolder
                    )
                )

                selectionManager.clear()
            },
            onDismiss = {
                showRestoreDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            showRestoreDialog = true
        },
        enabled = selectedItemsList.isNotEmpty()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.untrash),
            contentDescription = stringResource(id = R.string.media_restore)
        )
    }

    var showPermaDeleteDialog by remember { mutableStateOf(false) }

    if (showPermaDeleteDialog) {
        ConfirmationDialogWithBody(
            title = stringResource(id = R.string.media_delete_permanently_confirm),
            body = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_delete),
            action = {
                runAction(
                    FileOperationAction.Delete(
                        files = selectedItemsList.toFileOperationMetadataItems(),
                        album = AlbumType.PlaceHolder
                    )
                )

                selectionManager.clear()
            },
            onDismiss = {
                showPermaDeleteDialog = false
            }
        )
    }

    IconButton(
        onClick = {
            if (selectedItemsList.isNotEmpty()) {
                showPermaDeleteDialog = true
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