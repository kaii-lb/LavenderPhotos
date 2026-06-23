package com.kaii.photos.compose.app_bars.secure_folder

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.parent
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.launch

@Composable
fun SecureFolderViewBottomAppBar(
    selectionManager: SelectionManager,
    isGettingPermissions: MutableState<Boolean>,
    process: (action: GenericFileManager.Action) -> Unit
) {
    IsSelectingBottomAppBar {
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
                process(
                    GenericFileManager.Action.Share(
                        list = selectedItemsList
                    )
                )
            },
            enabled = selectedItemsList.isNotEmpty()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.share),
                contentDescription = stringResource(id = R.string.media_share)
            )
        }

        var showRestoreDialog by remember { mutableStateOf(false) }
        val restorePermissionState = rememberDirectoryPermissionManager(
            onGranted = {
                process(
                    GenericFileManager.Action.Restore(
                        list = selectedItemsList
                    )
                )
                selectionManager.clear()
                isGettingPermissions.value = false
            },
            onRejected = {
                isGettingPermissions.value = false

                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = resources.getString(R.string.secure_restore_failed),
                            icon = R.drawable.unlock,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        )

        if (showRestoreDialog) {
            ConfirmationDialog(
                title = stringResource(id = R.string.media_restore_confirm),
                confirmButtonLabel = stringResource(id = R.string.media_restore),
                action = {
                    loadingDialogTitle = resources.getString(R.string.media_restore_processing)

                    isGettingPermissions.value = true

                    val directories = selectedItemsList.fastMapNotNull {
                        it.parentPath.parent() // parentPath is originalPath with filename, not parent directory for secured items
                    }.distinct().toSet()

                    restorePermissionState.start(directories = directories)
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
                painter = painterResource(id = R.drawable.unlock),
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
                    process(
                        GenericFileManager.Action.Delete(
                            list = selectedItemsList
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
                showPermaDeleteDialog = true
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