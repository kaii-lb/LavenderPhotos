package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.moveMediaToSecureFolder
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.scrolling.rememberSinglePhotoScrollState
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.permissions.favourites.rememberFavouritesState
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SinglePhotoView(
    albumInfo: AlbumInfo,
    window: Window,
    viewModel: CustomAlbumViewModel,
    index: Int,
    isOpenWithDefaultView: Boolean = false
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SinglePhotoViewCommon(
        items = items,
        navController = LocalNavController.current,
        window = window,
        startIndex = index,
        albumInfo = albumInfo,
        isOpenWithDefaultView = isOpenWithDefaultView,
        removeFromCustom = { item ->
            viewModel.remove(items = setOf(item))
        }
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    window: Window,
    viewModel: MultiAlbumViewModel,
    index: Int,
    albumInfo: AlbumInfo,
    isOpenWithDefaultView: Boolean = false,
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        albumInfo = albumInfo,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = isOpenWithDefaultView
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    viewModel: SearchViewModel,
    window: Window,
    index: Int,
    albumInfo: AlbumInfo
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        albumInfo = albumInfo,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = false
    )
}


@Composable
fun SinglePhotoView(
    viewModel: FavouritesViewModel,
    window: Window,
    index: Int
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        albumInfo = AlbumInfo.Empty,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = false
    )
}

@Composable
fun SinglePhotoView(
    navController: NavHostController,
    viewModel: ImmichAlbumViewModel,
    window: Window,
    index: Int,
    albumInfo: AlbumInfo
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        albumInfo = albumInfo,
        navController = navController,
        window = window,
        isOpenWithDefaultView = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun SinglePhotoViewCommon(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    startIndex: Int,
    albumInfo: AlbumInfo,
    navController: NavHostController,
    window: Window,
    isOpenWithDefaultView: Boolean,
    removeFromCustom: (MediaStoreData) -> Unit = {}
) {
    val state = rememberPagerState(
        initialPage = startIndex
    ) {
        items.itemCount
    }

    var currentIndex by rememberSaveable {
        mutableIntStateOf(
            startIndex
        )
    }

    LaunchedEffect(rememberUpdatedState(startIndex).value) {
        state.scrollToPage(startIndex)
    }

    val context = LocalContext.current
    val appBarsVisible = remember { mutableStateOf(true) }
    var mediaItem by remember { mutableStateOf(MediaStoreData.dummyItem) }

    LaunchedEffect(currentIndex, items, items.itemSnapshotList) {
        withContext(Dispatchers.IO) {
            mediaItem =
                if (currentIndex in 0..<items.itemCount && items.itemCount != 0) {
                    ((items[currentIndex] as? PhotoLibraryUIModel.MediaImpl))?.item ?: MediaStoreData.dummyItem
                } else {
                    MediaStoreData.dummyItem
                }

            if (mediaItem.type == MediaType.Image
                && mediaItem != MediaStoreData.dummyItem
                && mediaItem.immichUrl == null
            ) {
                val date = getDateTakenForMedia(
                    absolutePath = mediaItem.absolutePath,
                    dateModified = mediaItem.dateModified
                )

                if (date != mediaItem.dateTaken) {
                    context.contentResolver.setDateForMedia(
                        uri = mediaItem.uri.toUri(),
                        type = mediaItem.type,
                        dateTaken = date
                    )
                }
            }
        }
    }

    LaunchedEffect(items.itemCount) {
        snapshotFlow { items.itemCount }.collectLatest {
            delay(PhotoGridConstants.LOADING_TIME_SHORT)
            if (items.itemCount == 0) launch(Dispatchers.Main) {
                navController.popBackStack()
            }
        }
    }

    BackHandler(
        enabled = isOpenWithDefaultView
    ) {
        (context as Activity).finish()
    }

    val scrollState = rememberSinglePhotoScrollState(isOpenWithView = false)
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = { mediaItem },
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
                privacyMode = scrollState.privacyMode,
                isOpenWithDefaultView = isOpenWithDefaultView,
                expandInfoDialog = {
                    showInfoDialog = true
                }
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()

            BottomBar(
                visible = appBarsVisible.value,
                currentItem = { mediaItem },
                privacyMode = scrollState.privacyMode,
                isCustom = albumInfo.isCustomAlbum,
                showEditingView = {
                    coroutineScope.launch(Dispatchers.Main) {
                        setBarVisibility(
                            visible = true,
                            window = window
                        ) {
                            appBarsVisible.value = it
                        }

                        if (mediaItem.type == MediaType.Image) {
                            navController.navigate(
                                Screens.ImageEditor(
                                    absolutePath = mediaItem.absolutePath,
                                    uri = mediaItem.uri,
                                    dateTaken = mediaItem.dateTaken
                                )
                            )
                        } else {
                            navController.navigate(
                                Screens.VideoEditor(
                                    uri = mediaItem.uri,
                                    absolutePath = mediaItem.absolutePath,
                                    albumInfo = albumInfo
                                )
                            )
                        }
                    }
                },
                removeFromCustom = removeFromCustom
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )
        if (showInfoDialog) {
            val coroutineScope = rememberCoroutineScope()

            SinglePhotoInfoDialog(
                currentMediaItem = mediaItem,
                showMoveCopyOptions = true,
                sheetState = sheetState,
                privacyMode = scrollState.privacyMode,
                isCustomAlbum = albumInfo.isCustomAlbum,
                dismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInfoDialog = false
                    }
                },
                togglePrivacyMode = scrollState::togglePrivacyMode
            )
        }

        val useBlackBackground by LocalMainViewModel.current.useBlackViewBackgroundColor.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(state.currentPage) {
                snapshotFlow { state.currentPage }.collect {
                    sheetState.hide()
                    showInfoDialog = false

                    currentIndex = it
                }
            }

            HorizontalImageList(
                items = items,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible,
                scrollState = scrollState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    currentItem: () -> MediaStoreData,
    privacyMode: Boolean,
    isCustom: Boolean,
    showEditingView: () -> Unit,
    removeFromCustom: (MediaStoreData) -> Unit
) {
    var showLoadingDialog by remember { mutableStateOf(false) }

    if (showLoadingDialog) {
        LoadingDialog(
            title = stringResource(id = R.string.secure_encrypting),
            body = stringResource(id = R.string.secure_processing)
        )
    }

    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible || showLoadingDialog,
        enter = scaleIn(
            animationSpec = AnimationConstants.expressiveSpring(),
            initialScale = 0.2f
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = AnimationConstants.expressiveSpring(),
            targetScale = 0.2f
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(1f),
            contentAlignment = Alignment.Center
        ) {
            val coroutineScope = rememberCoroutineScope()
            val resources = LocalResources.current

            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    val filePermissionManager = rememberFilePermissionManager(
                        onGranted = {
                            showEditingView()
                        },
                        onRejected = {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = resources.getString(R.string.permissions_needed),
                                        icon = R.drawable.shield_lock,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    )

                    val dirPermissionManager = rememberDirectoryPermissionManager(
                        onGranted = {
                            filePermissionManager.get(uris = listOf(currentItem().uri.toUri()))
                        },
                        onRejected = {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = resources.getString(R.string.permissions_needed),
                                        icon = R.drawable.shield_lock,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    )

                    FilledIconButton(
                        onClick = {
                            dirPermissionManager.start(
                                directories = setOf(currentItem().absolutePath.parent())
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = vibrantFloatingToolbarColors().fabContentColor,
                            containerColor = vibrantFloatingToolbarColors().fabContainerColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shapes = IconButtonDefaults.shapes(
                            shape = IconButtonDefaults.mediumSquareShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
                        enabled = !privacyMode,
                        modifier = Modifier
                            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.paintbrush),
                            contentDescription = "Edit this media"
                        )
                    }
                },
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
            ) {
                IconButton(
                    onClick = {
                        shareImage(currentItem().uri.toUri(), context)
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(id = R.string.media_share)
                    )
                }

                val mainViewModel = LocalMainViewModel.current
                val applicationDatabase = LocalAppDatabase.current

                val filePermissionManager = rememberFilePermissionManager(
                    onGranted = {
                        mainViewModel.launch {
                            val item = currentItem()
                            moveMediaToSecureFolder(
                                list = listOf(
                                    SelectionManager.SelectedItem(
                                        id = item.id,
                                        isImage = item.type == MediaType.Image,
                                        parentPath = item.parentPath
                                    )
                                ),
                                context = context,
                                applicationDatabase = applicationDatabase
                            ) {
                                removeFromCustom(item)

                                showLoadingDialog = false
                            }
                        }
                    }
                )

                val dirPermissionManager = rememberDirectoryPermissionManager(
                    onGranted = {
                        showLoadingDialog = true
                        filePermissionManager.get(
                            uris = listOf(currentItem().uri.toUri())
                        )
                    }
                )

                val showMoveToSecureFolderDialog = remember { mutableStateOf(false) }
                ConfirmationDialog(
                    showDialog = showMoveToSecureFolderDialog,
                    dialogTitle = stringResource(id = R.string.media_secure_confirm),
                    confirmButtonLabel = stringResource(id = R.string.media_secure)
                ) {
                    dirPermissionManager.start(
                        directories = setOf(currentItem().absolutePath.parent())
                    )
                }

                val motionPhoto = rememberMotionPhoto(uri = currentItem().uri.toUri())
                IconButton(
                    onClick = {
                        showMoveToSecureFolderDialog.value = true
                    },
                    enabled = !motionPhoto.isMotionPhoto.value && !privacyMode && !isCustom
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.secure_folder),
                        contentDescription = stringResource(id = R.string.media_secure)
                    )
                }

                val favState = rememberFavouritesState(media = currentItem())

                val vibratorManager = rememberVibratorManager()
                val isFavourited by favState.state.collectAsStateWithLifecycle()

                IconButton(
                    onClick = {
                        vibratorManager.vibrateShort()

                        favState.setFavourite(
                            uri = currentItem().uri.toUri(),
                            favourite = !isFavourited
                        )
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = if (isFavourited) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = stringResource(id = R.string.favourites_add_this)
                    )
                }

                val doNotTrash by mainViewModel.settings.permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)
                // TODO: look into possibly sharing permission managers?
                val trashFilePermissionManager = rememberFilePermissionManager(
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            if (!isCustom) {
                                if (doNotTrash) {
                                    permanentlyDeletePhotoList(
                                        context = context,
                                        list = listOf(currentItem().uri.toUri())
                                    )
                                } else {
                                    setTrashedOnPhotoList(
                                        context = context,
                                        list = listOf(currentItem().uri.toUri()),
                                        trashed = true
                                    )
                                }
                            } else {
                                removeFromCustom(currentItem())
                            }
                        }
                    }
                )

                val showDeleteDialog = remember { mutableStateOf(false) }
                val confirmToDelete by mainViewModel.settings.permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)

                if (showDeleteDialog.value) {
                    ConfirmationDialog(
                        showDialog = showDeleteDialog,
                        dialogTitle = stringResource(
                            id =
                                when {
                                    isCustom -> R.string.custom_album_remove_media_desc

                                    doNotTrash -> R.string.media_delete_permanently_confirm

                                    else -> R.string.media_delete_confirm
                                }
                        ),
                        confirmButtonLabel = stringResource(
                            id =
                                when {
                                    isCustom -> R.string.custom_album_remove_media

                                    else -> R.string.media_delete
                                }
                        )
                    ) {
                        trashFilePermissionManager.get(
                            uris = listOf(currentItem().uri.toUri())
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (confirmToDelete) {
                            showDeleteDialog.value = true
                        } else {
                            trashFilePermissionManager.get(
                                uris = listOf(currentItem().uri.toUri())
                            )
                        }
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.trash),
                        contentDescription = stringResource(id = R.string.media_delete)
                    )
                }
            }
        }
    }
}


