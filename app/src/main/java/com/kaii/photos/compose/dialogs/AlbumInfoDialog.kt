package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
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
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import kotlinx.coroutines.Dispatchers

private const val TAG = "com.kaii.photos.compose.dialogs.AlbumInfoDialog"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumInfoDialog(
    albumInfo: () -> AlbumInfo,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    itemCount: suspend () -> Int,
    albumSize: suspend () -> String,
    toggleSelectionMode: () -> Unit,
    dismiss: () -> Unit,
) {
    // remove (weird) drag handle ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides
                RippleConfiguration(
                    color = Color.Transparent,
                    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
                )
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
                                toggleSelectionMode = toggleSelectionMode,
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
                            toggleSelectionMode = toggleSelectionMode,
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
    albumInfo: () -> AlbumInfo,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
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
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            dismiss = dismiss
        )
    }
}

@Composable
private fun IconContentVertical(
    albumInfo: () -> AlbumInfo,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
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
            modifier = Modifier.weight(1f),
            toggleSelectionMode = toggleSelectionMode,
            dismiss = dismiss
        )
    }
}

@Composable
private fun IconContentImpl(
    albumInfo: () -> AlbumInfo,
    modifier: Modifier = Modifier,
    toggleSelectionMode: () -> Unit,
    dismiss: () -> Unit
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val albums by mainViewModel.settings.albums.get().collectAsStateWithLifecycle(initialValue = emptyList())
    var fileName by remember { mutableStateOf(albumInfo().name) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val permissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            val album = albumInfo()
            if (fileName != album.name) {
                if (album.paths.size == 1) {
                    val basePath = album.mainPath.toBasePath()
                    val currentVolumes = MediaStore.getExternalVolumeNames(context)
                    val volumeName =
                        if (basePath == baseInternalStorageDirectory) "primary"
                        else currentVolumes.find {
                            val possible =
                                basePath.replace("/storage/", "").removeSuffix("/")
                            it == possible || it == possible.lowercase()
                        }

                    renameDirectory(
                        context = context,
                        absolutePath = album.mainPath,
                        newName = fileName,
                        base = volumeName!!
                    )

                    val newInfo = album.copy(
                        name = fileName,
                        paths = setOf(album.mainPath.replace(album.name, fileName))
                    )

                    albums.filter { child ->
                        child.paths.any { it.startsWith(album.mainPath) }
                    }.forEach { child ->
                        mainViewModel.settings.albums.edit(
                            id = child.id,
                            newInfo = child.copy(
                                paths = child.paths.map {
                                    if (it.startsWith(album.mainPath)) it.replace(album.mainPath, newInfo.mainPath)
                                    else it
                                }.toSet()
                            )
                        )
                    }

                    mainViewModel.settings.albums.edit(
                        id = album.id,
                        newInfo = newInfo
                    )

                    try {
                        context.contentResolver.releasePersistableUriPermission(
                            context.getExternalStorageContentUriFromAbsolutePath(
                                absolutePath = newInfo.mainPath,
                                trimDoc = true
                            ),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Throwable) {
                        Log.d(TAG, "Couldn't release permission for ${newInfo.mainPath}")
                        e.printStackTrace()
                    }
                } else {
                    mainViewModel.settings.albums.edit(
                        id = album.id,
                        newInfo = album.copy(name = fileName)
                    )
                }
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
                    directories = albumInfo().paths
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

    val isPinned by remember {
        derivedStateOf {
            albums.find {
                it.equalsIgnoringPinned(albumInfo())
            }?.isPinned ?: false
        }
    }

    IconButton(
        onClick = {
            mainViewModel.settings.albums.edit(
                id = albumInfo().id,
                newInfo = albumInfo().copy(isPinned = !isPinned)
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

    val navController = LocalNavController.current
    val showDeleteDialog = remember { mutableStateOf(false) }
    val autoDetect by mainViewModel.settings.albums.getAutoDetect().collectAsStateWithLifecycle(initialValue = false)

    ConfirmationDialog(
        showDialog = showDeleteDialog,
        dialogTitle = stringResource(id = R.string.albums_remove_desc),
        confirmButtonLabel = stringResource(id = R.string.albums_remove)
    ) {
        val album = albumInfo()
        mainViewModel.settings.albums.remove(album.id)

        try {
            // TODO: possible make less messy
            mainViewModel.launch(Dispatchers.IO) {
                MediaDatabase.getInstance(context)
                    .customDao()
                    .deleteAlbum(album = album.id)
            }

            context.contentResolver.releasePersistableUriPermission(
                context.getExternalStorageContentUriFromAbsolutePath(
                    album.mainPath,
                    true
                ),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Throwable) {
            Log.d(TAG, "Couldn't release permission for ${album.mainPath}")
            e.printStackTrace()
        }

        navController.popBackStack()
    }

    if (!autoDetect || (albumInfo().isCustomAlbum && albumInfo().immichId.isBlank())) {
        IconButton(
            onClick = {
                showDeleteDialog.value = true
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
    albumInfo: () -> AlbumInfo,
    modifier: Modifier = Modifier,
    itemCount: suspend () -> Int,
    albumSize: suspend () -> String
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
    ) {
        item {
            val context = LocalContext.current
            val resources = LocalResources.current

            TallDialogInfoRow(
                title =
                    if (albumInfo().paths.size > 1) stringResource(id = R.string.albums_paths)
                    else stringResource(id = R.string.albums_path),
                info = albumInfo().paths.joinToString(separator = ",") { it },
                icon = R.drawable.folder,
                position = RowPosition.Top,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(
                        resources.getString(R.string.albums_path),
                        albumInfo().paths.joinToString(separator = ",") { it }
                    )
                    clipboardManager.setPrimaryClip(clipData)
                }
            )
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
                info = albumInfo().immichId.takeIf { albumInfo().immichId.isNotBlank() } ?: stringResource(id = R.string.albums_immich_not_backed_up),
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