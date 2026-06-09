package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.TextEntryDialog
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "com.kaii.photos.compose.dialogs.AlbumInfoDialog"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumInfoDialog(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    itemCount: suspend () -> Int,
    albumSize: suspend () -> String,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit,
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides null
    ) {
        val isLandscape by rememberDeviceOrientation()

        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = dismiss,
            contentWindowInsets = {
                if (!isLandscape) WindowInsets.systemBars
                else WindowInsets()
            },
            modifier = modifier
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // reset ripple for normal buttons
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration()
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(
                                space = 12.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = albumInfo().name,
                                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconContentVertical(
                                albumInfo = albumInfo,
                                albums = albums,
                                autoDetectAlbums = autoDetectAlbums,
                                itemCount = itemCount,
                                toggleSelectionMode = toggleSelectionMode,
                                editAlbum = editAlbum,
                                renameAlbum = renameAlbum,
                                removeAlbum = removeAlbum,
                                dismiss = dismiss
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(
                                space = 12.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.albums_info),
                                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                                fontWeight = FontWeight.Bold
                            )

                            InfoContent(
                                albumInfo = albumInfo,
                                itemCount = itemCount,
                                albumSize = albumSize,
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, start = 16.dp, bottom = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = albumInfo().name,
                            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconContentHorizontal(
                            albumInfo = albumInfo,
                            albums = albums,
                            autoDetectAlbums = autoDetectAlbums,
                            itemCount = itemCount,
                            toggleSelectionMode = toggleSelectionMode,
                            editAlbum = editAlbum,
                            removeAlbum = removeAlbum,
                            renameAlbum = renameAlbum,
                            dismiss = dismiss
                        )

                        Text(
                            text = stringResource(id = R.string.albums_info),
                            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                            fontWeight = FontWeight.Bold
                        )

                        InfoContent(
                            albumInfo = albumInfo,
                            itemCount = itemCount,
                            albumSize = albumSize,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconContentHorizontal(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: suspend () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        IconContentImpl(
            albumInfo = albumInfo,
            albums = albums,
            autoDetectAlbums = autoDetectAlbums,
            itemCount = itemCount,
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            editAlbum = editAlbum,
            renameAlbum = renameAlbum,
            removeAlbum = removeAlbum,
            dismiss = dismiss
        )
    }
}

@Composable
private fun IconContentVertical(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: suspend () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconContentImpl(
            albumInfo = albumInfo,
            albums = albums,
            autoDetectAlbums = autoDetectAlbums,
            itemCount = itemCount,
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            editAlbum = editAlbum,
            renameAlbum = renameAlbum,
            removeAlbum = removeAlbum,
            dismiss = dismiss
        )
    }
}

@Composable
private fun IconContentImpl(
    albumInfo: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    itemCount: suspend () -> Int,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    renameAlbum: (newName: String) -> Unit,
    removeAlbum: (id: String) -> Unit,
    dismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navController = LocalNavController.current

    if (albumInfo().immichId != null) {
        IconButton(
            onClick = {
                // so it doesn't die when the dialog dismisses
                context.appModule.scope.launch {
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

                        val albums = context.appModule.albumGridState.singleAlbums.value

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
                when (info) {
                    is AlbumType.Folder -> info.copy(pinned = !isPinned)
                    is AlbumType.Custom -> info.copy(pinned = !isPinned)
                    is AlbumType.Cloud -> info.copy(pinned = !isPinned)
                    else -> AlbumType.PlaceHolder
                }
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
        modifier = modifier
            .height(48.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.name),
            contentDescription = stringResource(id = R.string.media_rename)
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.albums_remove_desc),
            confirmButtonLabel = stringResource(id = R.string.albums_remove),
            action = {
                val album = albumInfo()
                removeAlbum(album.id)

                if (album is AlbumType.Folder) {
                    try {
                        context.contentResolver.releasePersistableUriPermission(
                            context.getExternalStorageContentUriFromAbsolutePath(
                                album.paths.first(),
                                true
                            ),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Throwable) {
                        Log.d(TAG, "Couldn't release permission for ${album.paths.first()}")
                        e.printStackTrace()
                    }
                } else if (album is AlbumType.Custom) {
                    // TODO: possible make less messy
                    context.appModule.scope.launch(Dispatchers.IO) {
                        MediaDatabase.getInstance(context)
                            .customDao()
                            .deleteAlbum(album = album.id)
                    }
                }

                navController.popBackStack()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    if (!autoDetectAlbums() || (albumInfo() is AlbumType.Custom && albumInfo().immichId == null)) {
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

@Composable
private fun InfoContent(
    albumInfo: () -> AlbumType,
    modifier: Modifier = Modifier,
    itemCount: suspend () -> Int,
    albumSize: suspend () -> String
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
    ) {
        if (albumInfo() is AlbumType.Folder) {
            item {
                val context = LocalContext.current
                val resources = LocalResources.current

                val info = albumInfo() as AlbumType.Folder

                TallDialogInfoRow(
                    title =
                        if (info.paths.size > 1) stringResource(id = R.string.albums_paths)
                        else stringResource(id = R.string.albums_path),
                    info = info.paths.joinToString(separator = ",") { it },
                    icon = R.drawable.folder,
                    position = RowPosition.Top,
                    onClick = {
                        val clipboardManager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(
                            resources.getString(R.string.albums_path),
                            info.paths.joinToString(separator = ",") { it }
                        )
                        clipboardManager.setPrimaryClip(clipData)
                    }
                )
            }
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current
            var mediaCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                mediaCount = itemCount()
            }

            TallDialogInfoRow(
                title = stringResource(id = R.string.albums_item_count),
                info = mediaCount.toString(),
                icon = R.drawable.data,
                position = RowPosition.Middle,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.albums_item_count),
                        mediaCount.toString()
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current

            TallDialogInfoRow(
                title = stringResource(id = R.string.immich_uuid),
                info = albumInfo().immichId ?: stringResource(id = R.string.albums_immich_not_backed_up),
                icon = R.drawable.cloud_upload,
                position = RowPosition.Middle,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.immich_uuid),
                        albumInfo().immichId
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }

        item {
            val context = LocalContext.current
            val resources = LocalResources.current
            var size by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                size = albumSize()
            }

            TallDialogInfoRow(
                title = stringResource(id = R.string.exif_size),
                info = size,
                icon = R.drawable.storage,
                position = RowPosition.Bottom,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.exif_size),
                        size
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
        }
    }
}