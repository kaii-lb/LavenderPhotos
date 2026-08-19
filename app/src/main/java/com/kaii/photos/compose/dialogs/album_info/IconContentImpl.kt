package com.kaii.photos.compose.dialogs.album_info

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun IconContentImpl(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    val resources = LocalResources.current
    val navController = LocalNavController.current

    if (albumInfo().immichId != null) {
        IconButton(
            onClick = {
                // so it doesn't die when the dialog dismisses
                PhotosApplication.appModule.scope.launch {
                    if (albumInfo().immichId != null) {
                        val itemCount = itemCount()

                        if (itemCount == 0) {
                            LavenderSnackbarController.pushEvent(
                                event = LavenderSnackbarEvent.MessageEvent(
                                    message = resources.getString(R.string.immich_share_album_empty),
                                    icon = R.drawable.error_2,
                                    duration = SnackbarDuration.Short
                                )
                            )

                            return@launch
                        }

                        val albums = PhotosApplication.appModule.albumGridState.singleAlbums.value

                        albums.find {
                            it.id == albumInfo().id
                        }?.let { album ->
                            dismiss()
                            delay(500.milliseconds)

                            launch(Dispatchers.Main) {
                                navController.navigate(
                                    Screens.Immich.ShareAlbumPage(
                                        albumImmichId = albumInfo().immichId!!,
                                        albumTitle = albumInfo().name,
                                        itemCount = itemCount,
                                        latestImage = album.info.thumbnail.uri
                                    )
                                )
                            }
                        }
                    }
                }
            },
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.share),
                contentDescription = stringResource(id = R.string.media_share)
            )
        }
    }

    var fileName by remember { mutableStateOf(albumInfo().name) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val permissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            val album = albumInfo()
            if (fileName != album.name) {
                renameAlbum(fileName)
            }

            dismiss()
        }
    )

    if (showRenameDialog) {
        TextEntryDialog(
            title = stringResource(id = R.string.media_rename),
            placeholder = albumInfo().name.substringBeforeLast("."),
            startValue = albumInfo().name.substringBeforeLast("."),
            errorMessage = stringResource(id = R.string.albums_rename_failure),
            onConfirm = { _ ->
                permissionManager.start(
                    directories = (albumInfo() as? AlbumType.Folder)?.paths ?: emptySet()
                )
                true
            },
            onValueChange = { new ->
                fileName = new
                new != albumInfo().name
            },
            onDismiss = {
                showRenameDialog = false
            }
        )
    }

    IconButton(
        onClick = toggleSelectionMode,
        modifier = modifier
            .height(48.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.checklist),
            contentDescription = stringResource(id = R.string.media_select)
        )
    }

    val isPinned = albums().find {
        it.id == albumInfo().id
    }?.pinned ?: false

    IconButton(
        onClick = {
            val info = albumInfo()
            editAlbum(
                info.id,
                info.modify(pinned = !isPinned)
            )
        },
        modifier = modifier
            .height(48.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (isPinned) R.drawable.keep_off else R.drawable.keep
            ),
            contentDescription =
                if (isPinned) stringResource(id = R.string.albums_unpin)
                else stringResource(id = R.string.albums_pin)
        )
    }

    IconButton(
        onClick = {
            showRenameDialog = true
        },
        enabled = albumInfo() !is AlbumType.SAFFolder,
        modifier = modifier
            .height(48.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.name),
            contentDescription = stringResource(id = R.string.media_rename)
        )
    }

    if (!autoDetectAlbums() || (albumInfo() is AlbumType.Custom && albumInfo().immichId == null) || albumInfo() is AlbumType.SAFFolder) {
        var showDeleteDialog by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = stringResource(id = R.string.albums_remove_desc),
                confirmButtonLabel = stringResource(id = R.string.albums_remove),
                action = {
                    removeAlbum(albumInfo().id)

                    navController.popBackStack()
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }

        IconButton(
            onClick = {
                showDeleteDialog = true
            },
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = stringResource(id = R.string.albums_remove_desc)
            )
        }
    }
}