package com.kaii.photos.compose.app_bars.se

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.IsSelectingBottomAppBar
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.shareMultipleSecuredImages
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

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

        val showRestoreDialog = remember { mutableStateOf(false) }
        val restorePermissionState = rememberDirectoryPermissionManager(
            onGranted = {
                context.appModule.scope.launch(Dispatchers.IO) {
                    moveImageOutOfLockedFolder(
                        list = selectedItemsList.toSecureMedia(context = context),
                        context = context,
                        applicationDatabase = MediaDatabase.getInstance(context)
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
            context.appModule.scope.launch(Dispatchers.IO) {
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