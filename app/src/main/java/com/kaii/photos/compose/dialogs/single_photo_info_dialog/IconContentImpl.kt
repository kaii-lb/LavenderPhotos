package com.kaii.photos.compose.dialogs.single_photo_info_dialog

import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.grids.albums.MoveCopyAlbumListView
import com.kaii.photos.compose.pages.WallpaperSetter
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.toFileOperationMetadata
import java.io.File

@Composable
fun IconContentImpl(
    mediaItem: () -> MediaStoreData,
    showMoveCopyOptions: Boolean,
    privacyMode: () -> Boolean,
    album: () -> AlbumType,
    modifier: Modifier,
    dismiss: () -> Unit,
    runAction: (action: FileOperationAction) -> Any?
) {
    val file = remember(mediaItem()) { File(mediaItem().absolutePath) }
    var originalFileName by remember(file) {
        mutableStateOf(
            file.nameWithoutExtension.let {
                if (it.startsWith(".")) {
                    it.replace("trashed-", "")
                        .replaceBefore("-", "")
                        .replaceFirst("-", "")
                } else {
                    it
                }
            }
        )
    }

    var currentFileName by remember { mutableStateOf(originalFileName) }

    var showRenameDialog by remember { mutableStateOf(false) }
    if (showRenameDialog) {
        TextEntryDialog(
            title = stringResource(id = R.string.media_rename),
            placeholder = originalFileName,
            startValue = originalFileName,
            onConfirm = { newName ->
                val valid = newName != originalFileName

                if (valid) {
                    runAction(
                        FileOperationAction.PrepareFilesForWrite(
                            files = listOf(mediaItem().toFileOperationMetadata()),
                            FileOperationAction.RenameFile(
                                file = mediaItem().toFileOperationMetadata(),
                                newName = newName
                            )
                        )
                    )

                    currentFileName = newName
                    originalFileName = currentFileName
                    showRenameDialog = false
                }

                valid
            },
            onValueChange = { newName ->
                newName != originalFileName
            },
            onDismiss = {
                showRenameDialog = false
            }
        )
    }

    if (!mediaItem().isCloud) {
        IconButton(
            onClick = {
                showRenameDialog = true
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.name),
                contentDescription = "rename this media"
            )
        }
    }

    if (showMoveCopyOptions) {
        val show = remember { mutableStateOf(false) }
        var isMoving by remember { mutableStateOf(false) }

        MoveCopyAlbumListView(
            show = show,
            insetsPadding = WindowInsets.statusBars,
            dismissInfoDialog = dismiss,
            clear = {},
            isMoving = { isMoving },
            currentAlbum = album,
            onClick = { destination ->
                val files = listOf(mediaItem().toFileOperationMetadata())

                runAction(
                    if (isMoving) FileOperationAction.Move(
                        files = files,
                        origin = album(),
                        destination = destination
                    ) else FileOperationAction.Copy(
                        files = files,
                        destination = destination
                    )
                )
            }
        )

        IconButton(
            onClick = {
                isMoving = true
                show.value = true
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cut),
                contentDescription = "move this media"
            )
        }

        IconButton(
            onClick = {
                isMoving = false
                show.value = true
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.copy),
                contentDescription = "copy this media"
            )
        }
    }

    if (mediaItem().type == MediaType.Image && !mediaItem().isCloud) {
        val context = LocalContext.current

        IconButton(
            onClick = {
                val intent = Intent(context, WallpaperSetter::class.java).apply {
                    action = Intent.ACTION_SET_WALLPAPER
                    data = mediaItem().uri.toUri()
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("mimeType", mediaItem().mimeType)
                }

                context.startActivity(intent)
            },
            enabled = !privacyMode(),
            modifier = modifier
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.imagesearch_roller),
                contentDescription = "set as wallpaper"
            )
        }
    }
}