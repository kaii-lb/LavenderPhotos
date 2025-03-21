package com.kaii.photos.compose.dialogs

import android.content.Intent
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.AnimatableTextField
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.compose.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.User
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MediaData
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.checkPathIsDownloads
import com.kaii.photos.helpers.createPersistablePermissionLauncher
import com.kaii.photos.helpers.eraseExifMedia
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.renameImage
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getExternalStorageContentUriFromAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "INFO_DIALOGS"

@Composable
fun SingleAlbumDialog(
    showDialog: MutableState<Boolean>,
    dir: String,
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    if (showDialog.value) {
        val title = dir.split("/").last()

        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            }
        ) {
            val isEditingFileName = remember { mutableStateOf(false) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(1f),
            ) {
                IconButton(
                    onClick = {
                        showDialog.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(0.dp, 0.dp, 0.dp, 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close dialog button",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                AnimatableText(
                    first = "Rename",
                    second = title,
                    state = isEditingFileName.value,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = maxWidth - 48.dp - 24.dp) // for button and right side
                        .padding(4.dp, 0.dp, 0.dp, 4.dp)
                )
            }

            val reverseHeight by animateDpAsState(
                targetValue = if (isEditingFileName.value) 0.dp else 42.dp,
                label = "height of other options",
                animationSpec = tween(
                    durationMillis = 500
                )
            )

            Column(
                modifier = Modifier
                    .height(reverseHeight)
                    .animateContentSize()
                    .padding(8.dp, 0.dp)
            ) {
                DialogClickableItem(
                    text = "Select",
                    iconResId = R.drawable.check_item,
                    position = RowPosition.Top,
                ) {
                    showDialog.value = false
                    selectedItemsList.clear()
                    selectedItemsList.add(MediaStoreData())
                }
            }
            val fileName = remember { mutableStateOf(title) }
            val saveFileName = remember { mutableStateOf(false) }

            val absoluteDirPath = "$baseInternalStorageDirectory$dir"
            val context = LocalContext.current

            GetDirectoryPermissionAndRun(
                absoluteDirPaths = listOf(absoluteDirPath),
                shouldRun = saveFileName,
                onGranted = {
                    if (saveFileName.value && fileName.value != dir.split("/").last()) {
                        renameDirectory(context, absoluteDirPath, fileName.value)

                        val mainViewModel = mainViewModel
                        val newDir = dir.replace(title, fileName.value)

                        mainViewModel.settings.AlbumsList.editInAlbumsList(dir, fileName.value)
                        showDialog.value = false

                        try {
                            context.contentResolver.releasePersistableUriPermission(
                                context.getExternalStorageContentUriFromAbsolutePath(absoluteDirPath, true),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (e: Throwable) {
                            Log.d(TAG, "Couldn't release permission for $absoluteDirPath")
                            e.printStackTrace()
                        }

                        navController.popBackStack()
                        navController.navigate(
                            Screens.SingleAlbumView(
                                albums = listOf(newDir)
                            )
                        )

                        saveFileName.value = false
                    }
                },
                onRejected = {}
            )

            AnimatableTextField(
                state = isEditingFileName,
                string = fileName,
                doAction = saveFileName,
                rowPosition = RowPosition.Middle,
                enabled = !dir.checkPathIsDownloads(),
                modifier = Modifier
                    .padding(8.dp, 0.dp)
            ) {
                fileName.value = title
            }

            Column(
                modifier = Modifier
                    .height(reverseHeight)
                    .padding(8.dp, 0.dp)
            ) {
                DialogClickableItem(
                    text = "Remove album from list",
                    iconResId = R.drawable.delete,
                    position = RowPosition.Middle,
                    enabled = !dir.checkPathIsDownloads()
                ) {
                    mainViewModel.settings.AlbumsList.removeFromAlbumsList(dir)
                    showDialog.value = false

                    try {
                        context.contentResolver.releasePersistableUriPermission(
                            context.getExternalStorageContentUriFromAbsolutePath(absoluteDirPath, true),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Throwable) {
                        Log.d(TAG, "Couldn't release permission for $absoluteDirPath")
                        e.printStackTrace()
                    }

                    navController.popBackStack()
                }
            }

            val expanded = remember { mutableStateOf(false) }
            val expandedHeight by animateDpAsState(
                targetValue = if (expanded.value && !isEditingFileName.value) 36.dp else 0.dp,
                animationSpec = tween(
                    durationMillis = 250
                ),
                label = "height of expandable album info",
            )

            Column(
                modifier = Modifier
                    .height(reverseHeight + 6.dp + expandedHeight)
                    .padding(8.dp, 0.dp, 8.dp, 6.dp)
            ) {
                DialogExpandableItem(
                    text = "Album Info",
                    iconResId = R.drawable.info,
                    position = RowPosition.Bottom,
                    expanded = expanded
                ) {
                    DialogInfoText(
                        firstText = "Path",
                        secondText = absoluteDirPath,
                        iconResId = R.drawable.folder,
                    )
                }
            }
        }
    }
}

/** @param moveCopyInsetsPadding should only be used when showMoveCopyOptions is enabled */
@Composable
fun SinglePhotoInfoDialog(
    showDialog: MutableState<Boolean>,
    currentMediaItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    loadsFromMainViewModel: Boolean,
    showMoveCopyOptions: Boolean = true,
    moveCopyInsetsPadding: WindowInsets? = WindowInsets.statusBars
) {
    val context = LocalContext.current
    val isEditingFileName = remember { mutableStateOf(false) }

    val isLandscape by rememberDeviceOrientation()

    val modifier = remember(isLandscape) {
        if (isLandscape)
            Modifier.width(328.dp)
        else
            Modifier.fillMaxWidth(1f)
    }

    if (showDialog.value) {
        LavenderDialogBase(
            modifier = modifier,
            onDismiss = {
                showDialog.value = false
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f),
            ) {
                IconButton(
                    onClick = {
                        showDialog.value = false
                        isEditingFileName.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close dialog button",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                AnimatableText(
                    first = "Rename",
                    second = "More Options",
                    state = isEditingFileName.value,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight()
            ) {
                var originalFileName = currentMediaItem.displayName
                val fileName = remember { mutableStateOf(originalFileName) }
                val saveFileName = remember { mutableStateOf(false) }

                val expanded = remember { mutableStateOf(false) }

                GetPermissionAndRun(
                    uris = listOf(currentMediaItem.uri),
                    shouldRun = saveFileName,
                    onGranted = {
                        renameImage(context, currentMediaItem.uri, fileName.value)

                        originalFileName = fileName.value

                        if (loadsFromMainViewModel) {
                            val oldName = currentMediaItem.displayName
                            val path = currentMediaItem.absolutePath

                            val newGroupedMedia = groupedMedia.value.toMutableList()

                            // set currentMediaItem to new one with new name
                            val newMedia = currentMediaItem.copy(
                                displayName = fileName.value,
                                absolutePath = path.replace(oldName, fileName.value)
                            )

                            val index = groupedMedia.value.indexOf(currentMediaItem)
                            newGroupedMedia[index] = newMedia
                            groupedMedia.value = newGroupedMedia
                        }
                    }
                )

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    extraAction = expanded,
                    rowPosition = RowPosition.Top
                ) {
                    // fileName.value = originalFileName // TODO: fix so it doesn't reset while trying to allow by user
                }

                var mediaData by remember {
                    mutableStateOf(
                        emptyMap<MediaData, Any>()
                    )
                }

                LaunchedEffect(Unit) {
                    getExifDataForMedia(currentMediaItem.absolutePath).collect {
                        mediaData = it
                    }
                }

                // should add a way to automatically calculate height needed for this
                val addedHeight by remember { derivedStateOf { 36.dp * (mediaData.keys.size + 1) } } // + 1 for the delete exif data row
                val moveCopyHeight = if (showMoveCopyOptions) 82.dp else 0.dp // 40.dp is height of one single row
                val setAsHeight = if (currentMediaItem.type != MediaType.Video) 40.dp else 0.dp

                val height by animateDpAsState(
                    targetValue = if (!isEditingFileName.value && expanded.value) {
                        42.dp + addedHeight + moveCopyHeight + setAsHeight
                    } else if (!isEditingFileName.value && !expanded.value) {
                        42.dp + moveCopyHeight + setAsHeight
                    } else {
                        0.dp
                    },
                    label = "height of other options",
                    animationSpec = tween(
                        durationMillis = 350
                    )
                )

                Column(
                    modifier = Modifier
                        .height(height)
                        .fillMaxWidth(1f)
                ) {
                    if (showMoveCopyOptions && moveCopyInsetsPadding != null) {
                        val show = remember { mutableStateOf(false) }
                        var isMoving by remember { mutableStateOf(false) }

                        val stateList = SnapshotStateList<MediaStoreData>()
                        stateList.add(currentMediaItem)

                        MoveCopyAlbumListView(
                            show = show,
                            selectedItemsList = stateList,
                            isMoving = isMoving,
                            groupedMedia = null,
                            insetsPadding = moveCopyInsetsPadding
                        )

                        DialogClickableItem(
                            text = "Copy to Album",
                            iconResId = R.drawable.copy,
                            position = RowPosition.Middle,
                        ) {
                            isMoving = false
                            show.value = true
                        }

                        DialogClickableItem(
                            text = "Move to Album",
                            iconResId = R.drawable.cut,
                            position = RowPosition.Middle,
                        ) {
                            isMoving = true
                            show.value = true
                        }
                    }

                    val infoComposable = @Composable {
                        LazyColumn(
                            modifier = Modifier
                                .wrapContentHeight()
                        ) {
                            for (key in mediaData.keys) {
                                item {
                                    val value = mediaData[key]

                                    val splitBy = Regex("(?=[A-Z])")
                                    val split = key.toString().split(splitBy)
                                    // println("SPLIT IS $split")
                                    val name = if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()

                                    DialogInfoText(
                                        firstText = name,
                                        secondText = value.toString(),
                                        iconResId = key.iconResInt,
                                    )
                                }
                            }

                            item {
                            	val showConfirmEraseDialog = remember { mutableStateOf(false) }
                                val runEraseExifData = remember { mutableStateOf(false) }
                                val coroutineScope = rememberCoroutineScope()

								ConfirmationDialogWithBody(
									showDialog = showConfirmEraseDialog,
									dialogTitle = "Erase EXIF data?",
									dialogBody = "This action cannot be undone",
									confirmButtonLabel = "Erase"
								) {
									runEraseExifData.value = true
								}

                                GetPermissionAndRun(
                                    uris = listOf(currentMediaItem.uri),
                                    shouldRun = runEraseExifData,
                                    onGranted = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                eraseExifMedia(currentMediaItem.absolutePath)

                                                LavenderSnackbarController.pushEvent(
                                                    LavenderSnackbarEvents.MessageEvent(
                                                        message = "Removed EXIF data",
                                                        iconResId = R.drawable.file_is_selected_foreground,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                )
                                            } catch (e: Throwable) {
                                                LavenderSnackbarController.pushEvent(
                                                    LavenderSnackbarEvents.MessageEvent(
                                                        message = "Failed erasing exif data",
                                                        iconResId = R.drawable.error_2,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                )

                                                Log.e(TAG, "Failed erasing exif data for ${currentMediaItem.absolutePath}")
                                                Log.e(TAG, e.toString())
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )

                                DialogInfoText(
                                    firstText = "",
                                    secondText = "Erase Exif Data",
                                    iconResId = R.drawable.error,
                                    color = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    showConfirmEraseDialog.value = true
                                }
                            }
                        }
                    }

                    if (currentMediaItem.type == MediaType.Image) {
                        DialogClickableItem(
                            text = "Set As",
                            iconResId = R.drawable.paintbrush,
                            position = RowPosition.Middle
                        ) {
                            val intent = Intent().apply {
                                action = Intent.ACTION_ATTACH_DATA
                                data = currentMediaItem.uri
                                addCategory(Intent.CATEGORY_DEFAULT)
                                putExtra("mimeType", currentMediaItem.mimeType)
                            }

                            context.startActivity(Intent.createChooser(intent, "Set as wallpaper"))
                        }
                    }

                    DialogExpandableItem(
                        text = "More Info",
                        iconResId = R.drawable.info,
                        position = RowPosition.Bottom,
                        expanded = expanded
                    ) {
                        infoComposable()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainAppDialog(
    showDialog: MutableState<Boolean>,
    currentView: MutableState<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val vibratorManager = rememberVibratorManager()
    val navController = LocalNavController.current

    if (showDialog.value) {
        LavenderDialogBase(
            onDismiss = {
                showDialog.value = false
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f),
            ) {
                IconButton(
                    onClick = {
                        showDialog.value = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close dialog button",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                // val splitBy = Regex("(?=[A-Z])")
                Text(
                    text = currentView.value.name, // .split(splitBy)[1],
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val storedName = mainViewModel.settings.User.getUsername().collectAsStateWithLifecycle(initialValue = null).value ?: return@Row

                var originalName by remember { mutableStateOf(storedName) }

                var username by remember {
                    mutableStateOf(
                        originalName
                    )
                }

                GlideImage(
                    model = R.drawable.cat_picture,
                    contentDescription = "User profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(1000.dp))
                ) {
                    it.override(256)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                }

                Spacer(modifier = Modifier.width(8.dp))

                val focus = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                var changeName by remember { mutableStateOf(false) }
                var backPressedCallbackEnabled by remember { mutableStateOf(false) }

                LaunchedEffect(key1 = changeName) {
                    focusManager.clearFocus()

                    if (!changeName && username != originalName) {
                        username = originalName
                        return@LaunchedEffect
                    }

                    mainViewModel.settings.User.setUsername(username)
                    originalName = username
                    changeName = false
                }

                TextField(
                    value = username,
                    onValueChange = { newVal ->
                        username = newVal
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = TextUnit(16f, TextUnitType.Sp),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    colors = TextFieldDefaults.colors().copy(
                        unfocusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTrailingIconColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = true
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            changeName = true
                        },
                    ),
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Cancel filename change button",
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    focusManager.clearFocus()
                                    changeName = false
                                    username = originalName
                                }
                        )
                    },
                    shape = RoundedCornerShape(1000.dp),
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged {
                            backPressedCallbackEnabled = it.isFocused
                        }
                )
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentHeight()
            ) {
                if (currentView.value != DefaultTabs.TabTypes.albums && currentView.value != DefaultTabs.TabTypes.secure) {
                    DialogClickableItem(
                        text = "Select",
                        iconResId = R.drawable.check_item,
                        position = RowPosition.Top,
                    ) {
                        showDialog.value = false
                        selectedItemsList.clear()
                        selectedItemsList.add(MediaStoreData())
                        vibratorManager.vibrateShort()
                    }
                }

                if (currentView.value == DefaultTabs.TabTypes.albums) {
                    val coroutineScope = rememberCoroutineScope()
                    val activityLauncher = createPersistablePermissionLauncher(
                        onGranted = { uri ->
                            uri.path?.let {
                                val dir = File(it)

                                val pathSections = dir.absolutePath.toRelativePath().split(":")
                                val path = pathSections[pathSections.size - 1]

                                Log.d(TAG, "Added album path $path")

                                mainViewModel.settings.AlbumsList.addToAlbumsList(path)
                            }
                        },

                        onFailure = {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = "Failed to add album :<",
                                        iconResId = R.drawable.error_2,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    )

                    DialogClickableItem(
                        text = "Add an album",
                        iconResId = R.drawable.add,
                        position = RowPosition.Top,
                    ) {
                        activityLauncher.launch(null)
                    }
                }

                DialogClickableItem(
                    text = "Data & Backup",
                    iconResId = R.drawable.data,
                    position = if (currentView.value == DefaultTabs.TabTypes.secure) RowPosition.Top else RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.DataAndBackup.name)
                }

                DialogClickableItem(
                    text = "Settings",
                    iconResId = R.drawable.settings,
                    position = RowPosition.Middle,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.SettingsMainView.name)
                }

                DialogClickableItem(
                    text = "About & Updates",
                    iconResId = R.drawable.info,
                    position = RowPosition.Bottom,
                ) {
                    showDialog.value = false
                    navController.navigate(MultiScreenViewType.AboutAndUpdateView.name)
                }
            }
        }
    }
}

/** always pass selected items without sections */
@Composable
fun SelectingMoreOptionsDialog(
    showDialog: MutableState<Boolean>,
    selectedItems: List<MediaStoreData>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isEditingFileName = remember { mutableStateOf(false) }

    val isLandscape by rememberDeviceOrientation()

    val modifier = if (isLandscape)
        Modifier.width(328.dp)
    else
        Modifier.fillMaxWidth(1f)

    val moveToSecureFolder = remember { mutableStateOf(false) }
    val tryGetDirPermission = remember { mutableStateOf(false) }

    var showLoadingDialog by remember { mutableStateOf(false) }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = selectedItems.fastMap {
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
        LoadingDialog(title = "Encrypting Files", body = "Please wait while the media is processed")
    }

    GetPermissionAndRun(
        uris = selectedItems.map { it.uri },
        shouldRun = moveToSecureFolder,
        onGranted = {
            mainViewModel.launch(Dispatchers.IO) {
                moveImageToLockedFolder(
                    selectedItems,
                    context
                ) {
                    onDone()
                    showLoadingDialog = false
                    showDialog.value = false
                }
            }
        }
    )

    LavenderDialogBase(
        modifier = modifier,
        onDismiss = {
            if (!showLoadingDialog) showDialog.value = false
            isEditingFileName.value = false
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f),
        ) {
            IconButton(
                onClick = {
                    if (!showLoadingDialog) showDialog.value = false
                    isEditingFileName.value = false
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Close dialog button",
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            Text(
                text = "More Options",
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .padding(12.dp)
                .wrapContentHeight()
        ) {
	        DialogClickableItem(
	            text = "Move to Secure Folder",
	            iconResId = R.drawable.locked_folder,
	            position = RowPosition.Single
	        ) {
	            if (selectedItems.isNotEmpty()) tryGetDirPermission.value = true
	        }
        }
    }
}

@Composable
fun FeatureNotAvailableDialog(showDialog: MutableState<Boolean>) {
    ExplanationDialog(
        title = "Not Available",
        explanation = "This feature is not available yet as the app is in Beta, please wait for a future release.",
        showDialog = showDialog
    )
}
