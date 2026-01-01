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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.mapSync
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    viewModel: CustomAlbumViewModel,
    mediaItemId: Long,
    albumInfo: AlbumInfo,
    nextMediaItemId: Long?,
    isOpenWithDefaultView: Boolean = false
) {
    val mediaStoreData = viewModel.mediaFlow.mapSync {
        it.filter { item ->
            item.type != MediaType.Section
        }
    }.collectAsStateWithLifecycle()

    val startIndex = remember {
        mediaStoreData.value.indexOfFirst { item ->
            item.id == nextMediaItemId || item.id == mediaItemId
        }
    }

    SinglePhotoViewCommon(
        navController = navController,
        window = window,
        startIndex = startIndex,
        nextMediaItemId = nextMediaItemId,
        mediaStoreData = mediaStoreData,
        albumInfo = albumInfo,
        isSearchPage = false,
        isOpenWithDefaultView = isOpenWithDefaultView
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    viewModel: MultiAlbumViewModel,
    mediaItemId: Long,
    albumInfo: AlbumInfo,
    nextMediaItemId: Long?,
    isOpenWithDefaultView: Boolean = false,
) {
    val mediaStoreData = viewModel.mediaFlow.mapSync {
        it.filter { item ->
            item.type != MediaType.Section
        }
    }.collectAsStateWithLifecycle()

    val startIndex = remember(mediaStoreData.value.isEmpty()) {
        mediaStoreData.value.indexOfFirst { item ->
            item.id == nextMediaItemId || item.id == mediaItemId
        }
    }

    SinglePhotoViewCommon(
        mediaStoreData = mediaStoreData,
        startIndex = startIndex,
        albumInfo = albumInfo,
        navController = navController,
        window = window,
        isOpenWithDefaultView = isOpenWithDefaultView,
        isSearchPage = false,
        nextMediaItemId = nextMediaItemId
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    navController: NavHostController,
    searchViewModel: SearchViewModel,
    window: Window,
    mediaItemId: Long,
    albumInfo: AlbumInfo,
    nextMediaItemId: Long?
) {
    val mediaStoreData = searchViewModel.groupedMedia.mapSync {
        it.filter { item ->
            item.type != MediaType.Section
        }
    }.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val allData by searchViewModel.mediaFlow.collectAsStateWithLifecycle()
    LaunchedEffect(allData) {
        searchViewModel.search(
            search = searchViewModel.query,
            context = context
        )
    }

    val startIndex = remember(mediaStoreData.value.isEmpty()) {
        mediaStoreData.value.indexOfFirst { item ->
            item.id == mediaItemId
        }
    }

    SinglePhotoViewCommon(
        mediaStoreData = mediaStoreData,
        startIndex = startIndex,
        albumInfo = albumInfo,
        navController = navController,
        window = window,
        isOpenWithDefaultView = false,
        isSearchPage = true,
        nextMediaItemId = nextMediaItemId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoViewCommon(
    mediaStoreData: State<List<MediaStoreData>>,
    startIndex: Int,
    albumInfo: AlbumInfo,
    navController: NavHostController,
    window: Window,
    isOpenWithDefaultView: Boolean,
    isSearchPage: Boolean,
    nextMediaItemId: Long?
) {
    val state = rememberPagerState(
        initialPage = startIndex.coerceIn(0, mediaStoreData.value.size - 1)
    ) {
        mediaStoreData.value.size
    }

    LaunchedEffect(Unit) {
        var trials = 0

        do {
            val index = mediaStoreData.value.indexOfFirst { it.id == nextMediaItemId }
            trials += 1

            if (trials >= 20) break

            if (index == -1) {
                delay(1000)
            } else {
                state.animateScrollToPage(
                    page = index,
                    animationSpec = AnimationConstants.expressiveTween(
                        durationMillis = AnimationConstants.DURATION_LONG
                    )
                )
            }
        } while (index == -1)
    }

    var currentIndex by rememberSaveable(startIndex) {
        mutableIntStateOf(
            startIndex
        )
    }

    val mediaDao = LocalAppDatabase.current.mediaEntityDao()
    val appBarsVisible = remember { mutableStateOf(true) }
    var mediaItem by remember { mutableStateOf(MediaStoreData.dummyItem) }

    LaunchedEffect(currentIndex) {
        withContext(Dispatchers.IO) {
            mediaItem =
                if (currentIndex in 0..mediaStoreData.value.size && mediaStoreData.value.isNotEmpty()) {
                    mediaStoreData.value[currentIndex]
                } else {
                    MediaStoreData.dummyItem
                }

            if (mediaItem.type == MediaType.Image) {
                val date = getDateTakenForMedia(
                    absolutePath = mediaItem.absolutePath,
                    dateModified = mediaItem.dateModified
                )

                if (date != mediaItem.dateTaken) {
                    mediaDao.deleteEntityById(id = mediaItem.id)
                    mediaDao.insertEntity(
                        MediaEntity(
                            id = mediaItem.id,
                            dateTaken = date,
                            mimeType = mediaItem.mimeType ?: "image/png",
                            displayName = mediaItem.displayName
                        )
                    )
                }
            }
        }
    }

    val context = LocalContext.current
    BackHandler(
        enabled = isOpenWithDefaultView
    ) {
        (context as Activity).finish()
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = mediaItem,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
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
                currentItem = mediaItem,
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
                                    uri = mediaItem.uri.toString(),
                                    dateTaken = mediaItem.dateTaken,
                                    albumInfo = albumInfo,
                                    isSearchPage = isSearchPage
                                )
                            )
                        } else {
                            navController.navigate(
                                Screens.VideoEditor(
                                    uri = mediaItem.uri.toString(),
                                    absolutePath = mediaItem.absolutePath,
                                    albumInfo = albumInfo,
                                    isSearchPage = isSearchPage
                                )
                            )
                        }
                    }
                },
                checkZeroItemsLeft = {
                    if (mediaStoreData.value.isEmpty()) coroutineScope.launch(Dispatchers.Main) {
                        navController.popBackStack()
                    }
                },
                onMoveMedia = {
                    coroutineScope.launch {
                        state.animateScrollToPage(
                            page = (currentIndex + 1) % mediaStoreData.value.size,
                            animationSpec = AnimationConstants.expressiveTween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        )
                        delay(AnimationConstants.DURATION_SHORT.toLong())
                    }
                }
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
                dismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInfoDialog = false
                    }
                },
                onMoveMedia = {
                    coroutineScope.launch {
                        state.animateScrollToPage(
                            page = (currentIndex + 1) % mediaStoreData.value.size,
                            animationSpec = AnimationConstants.expressiveTween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        )
                        delay(AnimationConstants.DURATION_SHORT.toLong())
                    }
                }
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
            LaunchedEffect(state) {
                snapshotFlow { state.currentPage }.collect {
                    sheetState.hide()
                    showInfoDialog = false

                    currentIndex = it
                }
            }

            HorizontalImageList(
                groupedMedia = mediaStoreData.value,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    currentItem: MediaStoreData,
    showEditingView: () -> Unit,
    checkZeroItemsLeft: () -> Unit,
    onMoveMedia: () -> Unit
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
            val getDirPerm = remember { mutableStateOf(false) }
            val getMediaPerm = remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val resources = LocalResources.current

            GetDirectoryPermissionAndRun(
                absoluteDirPaths = listOf(currentItem.absolutePath.getParentFromPath()),
                shouldRun = getDirPerm,
                onGranted = {
                    getMediaPerm.value = true
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

            GetPermissionAndRun(
                uris = listOf(currentItem.uri),
                shouldRun = getMediaPerm,
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

            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    FilledIconButton(
                        onClick = {
                            getDirPerm.value = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = vibrantFloatingToolbarColors().fabContentColor,
                            containerColor = vibrantFloatingToolbarColors().fabContainerColor
                        ),
                        shapes = IconButtonDefaults.shapes(
                            shape = IconButtonDefaults.mediumSquareShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
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
                        shareImage(currentItem.uri, context)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(id = R.string.media_share)
                    )
                }

                // TODO: maybe restructure this
                val showMoveToSecureFolderDialog = remember { mutableStateOf(false) }
                val moveToSecureFolder = remember { mutableStateOf(false) }
                val tryGetDirPermission = remember { mutableStateOf(false) }
                val mainViewModel = LocalMainViewModel.current
                val applicationDatabase = LocalAppDatabase.current

                GetDirectoryPermissionAndRun(
                    absoluteDirPaths = listOf(
                        currentItem.absolutePath.getParentFromPath()
                    ),
                    shouldRun = tryGetDirPermission,
                    onGranted = {
                        showLoadingDialog = true
                        moveToSecureFolder.value = true
                    },
                    onRejected = {}
                )

                ConfirmationDialog(
                    showDialog = showMoveToSecureFolderDialog,
                    dialogTitle = stringResource(id = R.string.media_secure_confirm),
                    confirmButtonLabel = stringResource(id = R.string.media_secure)
                ) {
                    tryGetDirPermission.value = true
                }

                GetPermissionAndRun(
                    uris = listOf(currentItem.uri),
                    shouldRun = moveToSecureFolder,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            moveImageToLockedFolder(
                                list = listOf(currentItem),
                                context = context,
                                applicationDatabase = applicationDatabase
                            ) {
                                onMoveMedia()
                                checkZeroItemsLeft()

                                showLoadingDialog = false
                            }
                        }
                    }
                )

                val motionPhoto = rememberMotionPhoto(uri = currentItem.uri)
                IconButton(
                    onClick = {
                        showMoveToSecureFolderDialog.value = true
                    },
                    enabled = !motionPhoto.isMotionPhoto.value
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.secure_folder),
                        contentDescription = stringResource(id = R.string.media_secure)
                    )
                }

                val vibratorManager = rememberVibratorManager()
                val favouritesViewModel: FavouritesViewModel = viewModel(
                    factory = FavouritesViewModelFactory(applicationDatabase)
                )
                val isSelected by favouritesViewModel.isInFavourites(currentItem.id).collectAsStateWithLifecycle()

                IconButton(
                    onClick = {
                        vibratorManager.vibrateShort()

                        if (!isSelected) {
                            favouritesViewModel.addToFavourites(currentItem, context)
                        } else {
                            favouritesViewModel.removeFromFavourites(currentItem.id)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = stringResource(id = R.string.favourites_add_this)
                    )
                }

                val showDeleteDialog = remember { mutableStateOf(false) }
                val runTrashAction = remember { mutableStateOf(false) }

                val doNotTrash by mainViewModel.settings.Permissions.getDoNotTrash().collectAsStateWithLifecycle(initialValue = true)
                if (showDeleteDialog.value) {
                    ConfirmationDialog(
                        showDialog = showDeleteDialog,
                        dialogTitle = stringResource(id = if (doNotTrash) R.string.media_delete_permanently_confirm else R.string.media_delete_confirm),
                        confirmButtonLabel = stringResource(id = R.string.media_delete)
                    ) {
                        runTrashAction.value = true
                    }
                }

                GetPermissionAndRun(
                    uris = listOf(currentItem.uri),
                    shouldRun = runTrashAction,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            if (doNotTrash) {
                                permanentlyDeletePhotoList(
                                    context = context,
                                    list = listOf(currentItem.uri)
                                )
                            } else {
                                setTrashedOnPhotoList(
                                    context = context,
                                    list = listOf(currentItem),
                                    trashed = true,
                                    appDatabase = applicationDatabase
                                )
                            }

                            onMoveMedia()
                            checkZeroItemsLeft()
                        }
                    }
                )

                val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)
                IconButton(
                    onClick = {
                        if (confirmToDelete) showDeleteDialog.value = true
                        else runTrashAction.value = true
                    }
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


