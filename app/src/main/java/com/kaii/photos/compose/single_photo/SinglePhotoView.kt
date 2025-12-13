package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.getParentFromPath
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    customAlbumViewModel: CustomAlbumViewModel,
    mediaItemId: Long,
    previousMediaItemId: Long?,
    albumInfo: AlbumInfo,
    loadsFromMainViewModel: Boolean
) {
    val mainViewModel = LocalMainViewModel.current
    val holderGroupedMedia: MutableState<List<MediaStoreData>?> = remember { mutableStateOf(null) }

    if (!loadsFromMainViewModel) {
        val customMediaStoreData by customAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

        LaunchedEffect(customMediaStoreData) {
            holderGroupedMedia.value = customMediaStoreData
        }
    } else {
        val media by mainViewModel.groupedMedia.collectAsStateWithLifecycle(initialValue = null)
        LaunchedEffect(media) {
            holderGroupedMedia.value = media
        }
    }

    if (holderGroupedMedia.value == null) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    LaunchedEffect(holderGroupedMedia.value) {
        if (holderGroupedMedia.value!!.isEmpty()) delay(PhotoGridConstants.LOADING_TIME)
        else delay(PhotoGridConstants.UPDATE_TIME)

        groupedMedia.value =
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
    }

    SinglePhotoViewCommon(
        navController = navController,
        window = window,
        mediaItemId = mediaItemId,
        previousMediaItemId = previousMediaItemId,
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        loadsFromMainViewModel = loadsFromMainViewModel
    )
}

@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    multiAlbumViewModel: MultiAlbumViewModel,
    mediaItemId: Long,
    previousMediaItemId: Long?,
    albumInfo: AlbumInfo,
    loadsFromMainViewModel: Boolean
) {
    val mainViewModel = LocalMainViewModel.current
    val holderGroupedMedia: MutableState<List<MediaStoreData>?> = remember { mutableStateOf(null) }

    if (!loadsFromMainViewModel) {
        val multiMediaStoreData by multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

        LaunchedEffect(multiMediaStoreData) {
            holderGroupedMedia.value = multiMediaStoreData
        }
    } else {
        val media by mainViewModel.groupedMedia.collectAsStateWithLifecycle(initialValue = null)
        LaunchedEffect(media) {
            holderGroupedMedia.value = media
        }
    }

    if (holderGroupedMedia.value == null) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    LaunchedEffect(holderGroupedMedia.value) {
        if (holderGroupedMedia.value!!.isEmpty()) delay(PhotoGridConstants.LOADING_TIME)
        else delay(PhotoGridConstants.UPDATE_TIME)

        groupedMedia.value =
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
    }

    SinglePhotoViewCommon(
        navController = navController,
        window = window,
        mediaItemId = mediaItemId,
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        previousMediaItemId = previousMediaItemId,
        loadsFromMainViewModel = loadsFromMainViewModel
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoViewCommon(
    navController: NavHostController,
    window: Window,
    mediaItemId: Long,
    previousMediaItemId: Long?,
    groupedMedia: MutableState<List<MediaStoreData>>,
    albumInfo: AlbumInfo,
    loadsFromMainViewModel: Boolean
) {
    var currentMediaItemIndex by rememberSaveable {
        mutableIntStateOf(
            groupedMedia.value.indexOf(
                groupedMedia.value.firstOrNull {
                    it.id == mediaItemId //|| it.id == previousMediaItemId
                }
            )
        )
    }

    val state = rememberPagerState(
        initialPage = currentMediaItemIndex
            .coerceIn(
                0,
                (groupedMedia.value.size - 1)
                    .coerceAtLeast(0)
            )
    ) {
        groupedMedia.value.size
    }

    LaunchedEffect(key1 = state.currentPage) {
        currentMediaItemIndex = state.currentPage
    }

    var lastGroupedMediaSize by remember { mutableIntStateOf(groupedMedia.value.size) }
    val editedMediaItemIndex by remember(groupedMedia.value.size) {
        derivedStateOf {
            groupedMedia.value.indexOf(
                groupedMedia.value.find {
                    it.id == mediaItemId
                }
            )
        }
    }

    LaunchedEffect(editedMediaItemIndex) {
        // if user is not scrolling, move to edited media
        if (editedMediaItemIndex != -1 && previousMediaItemId != null) {
            val index = if (groupedMedia.value.size == lastGroupedMediaSize) editedMediaItemIndex else editedMediaItemIndex - 1
            state.animateScrollToPage(
                page = index,
                animationSpec = tween(
                    durationMillis = AnimationConstants.DURATION,
                    easing = FastOutSlowInEasing
                )
            )

            lastGroupedMediaSize = groupedMedia.value.size
        }
    }

    val appBarsVisible = remember { mutableStateOf(true) }
    val resources = LocalResources.current
    val currentMediaItem = remember {
        derivedStateOf {
            val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
            if (index < groupedMedia.value.size) {
                groupedMedia.value[index]
            } else {
                MediaStoreData(
                    displayName = resources.getString(R.string.media_broken)
                )
            }
        }
    }

    val mediaDao = LocalAppDatabase.current.mediaEntityDao()
    LaunchedEffect(currentMediaItem.value) {
        withContext(Dispatchers.IO) {
            val media = currentMediaItem.value

            val date = getDateTakenForMedia(
                absolutePath = media.absolutePath,
                dateModified = media.dateModified
            )

            if (date != media.dateTaken) {
                mediaDao.deleteEntityById(id = mediaItemId)
                mediaDao.insertEntity(
                    MediaEntity(
                        id = mediaItemId,
                        dateTaken = date,
                        mimeType = media.mimeType ?: "image/png",
                        displayName = media.displayName
                    )
                )
            }
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }

    BackHandler(
        enabled = !showInfoDialog
    ) {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = currentMediaItem.value,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
                expandInfoDialog = {
                    showInfoDialog = true
                }
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()
            val mainViewModel = LocalMainViewModel.current

            BottomBar(
                visible = appBarsVisible.value,
                currentItem = currentMediaItem.value,
                groupedMedia = groupedMedia,
                loadsFromMainViewModel = loadsFromMainViewModel,
                state = state,
                showEditingView = {
                    mainViewModel.launch {
                        setBarVisibility(
                            visible = true,
                            window = window
                        ) {
                            appBarsVisible.value = it
                        }

                        if (currentMediaItem.value.type == MediaType.Image) {
                            navController.navigate(
                                Screens.ImageEditor(
                                    absolutePath = currentMediaItem.value.absolutePath,
                                    uri = currentMediaItem.value.uri.toString(),
                                    dateTaken = currentMediaItem.value.dateTaken,
                                    albumInfo = albumInfo
                                )
                            )
                        } else {
                            navController.navigate(
                                Screens.VideoEditor(
                                    uri = currentMediaItem.value.uri.toString(),
                                    absolutePath = currentMediaItem.value.absolutePath,
                                    albumInfo = albumInfo
                                )
                            )
                        }
                    }
                },
                onZeroItemsLeft = {
                    mainViewModel.launch {
                        navController.popBackStack()
                    }
                },
                removeIfInFavGrid = {
                    if (navController.previousBackStackEntry?.destination?.route == MultiScreenViewType.FavouritesGridView.name) {
                        sortOutMediaMods(
                            currentMediaItem.value,
                            groupedMedia,
                            coroutineScope,
                            state
                        ) {
                            navController.popBackStack()
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        if (showInfoDialog) {
            SinglePhotoInfoDialog(
                currentMediaItem = currentMediaItem.value,
                showMoveCopyOptions = true
            ) {
                showInfoDialog = false
            }
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
            HorizontalImageList(
                currentMediaItem = currentMediaItem.value,
                groupedMedia = groupedMedia.value,
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
    groupedMedia: MutableState<List<MediaStoreData>>,
    loadsFromMainViewModel: Boolean,
    state: PagerState,
    showEditingView: () -> Unit,
    onZeroItemsLeft: () -> Unit,
    removeIfInFavGrid: () -> Unit
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
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
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
                val coroutineScope = rememberCoroutineScope()

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
                                if (groupedMedia.value.none { it.type != MediaType.Section }) onZeroItemsLeft()

                                if (loadsFromMainViewModel) {
                                    sortOutMediaMods(
                                        currentItem,
                                        groupedMedia,
                                        coroutineScope,
                                        state,
                                        onZeroItemsLeft
                                    )
                                }

                                showLoadingDialog = false
                            }
                        }
                    }
                )

                IconButton(
                    onClick = {
                        showMoveToSecureFolderDialog.value = true
                    }
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
                            removeIfInFavGrid()
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

                            if (loadsFromMainViewModel) {
                                sortOutMediaMods(
                                    currentItem,
                                    groupedMedia,
                                    coroutineScope,
                                    state
                                ) {
                                    onZeroItemsLeft()
                                }
                            }

                            if (groupedMedia.value.all { it == currentItem }) onZeroItemsLeft()
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


