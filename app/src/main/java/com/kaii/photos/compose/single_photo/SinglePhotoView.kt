package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.app_bars.single_view.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.widgets.tags.AnimatedMediaTagManager
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.scrolling.retainSinglePhotoScrollState
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.tag_page.TagViewModel
import com.kaii.photos.models.tag_page.TagViewModelFactory
import com.kaii.photos.permissions.favourites.rememberCloudFavouritesState
import com.kaii.photos.permissions.favourites.rememberLocalFavouritesState
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import com.kaii.photos.presentation.single_photos_views.DismissDragState.Companion.barScaleModifier
import com.kaii.photos.presentation.single_photos_views.rememberDismissSinglePhotoState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SinglePhotoView(
    album: AlbumType.Custom,
    window: Window,
    viewModel: CustomAlbumViewModel,
    index: Int,
    editId: () -> Long?,
    isOpenWithDefaultView: Boolean = false
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        navController = LocalNavController.current,
        window = window,
        startIndex = index,
        editId = editId,
        album = album,
        isOpenWithDefaultView = isOpenWithDefaultView,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    window: Window,
    viewModel: MultiAlbumViewModel,
    index: Int,
    editId: () -> Long?,
    album: AlbumType.Folder,
    isOpenWithDefaultView: Boolean = false,
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        editId = editId,
        album = album,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = isOpenWithDefaultView,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    window: Window,
    viewModel: MainGridViewModel,
    index: Int,
    editId: () -> Long?,
    album: AlbumType.Folder,
    isOpenWithDefaultView: Boolean = false,
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        editId = editId,
        album = album,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = isOpenWithDefaultView,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Composable
fun SinglePhotoView(
    viewModel: SearchViewModel,
    window: Window,
    index: Int,
    editId: () -> Long?
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        editId = editId,
        album = AlbumType.PlaceHolder,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = false,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}


@Composable
fun SinglePhotoView(
    viewModel: FavouritesViewModel,
    window: Window,
    index: Int,
    editId: () -> Long?
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        editId = editId,
        album = AlbumType.PlaceHolder,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = false,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}

@Composable
fun SinglePhotoView(
    viewModel: ImmichAlbumViewModel,
    window: Window,
    index: Int,
    editId: () -> Long?,
    album: AlbumType.Cloud
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val tapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    SinglePhotoViewCommon(
        items = items,
        startIndex = index,
        editId = editId,
        album = album,
        navController = LocalNavController.current,
        window = window,
        isOpenWithDefaultView = false,
        useBlackBackground = useBlackBackground,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = blurViews,
        useCache = useCache,
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        getExifData = viewModel::getExifData,
        allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
        process = viewModel::runAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun SinglePhotoViewCommon(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    startIndex: Int,
    editId: () -> Long?,
    album: AlbumType,
    navController: NavHostController,
    window: Window,
    isOpenWithDefaultView: Boolean,
    useBlackBackground: Boolean,
    confirmToDelete: Boolean,
    doNotTrash: Boolean,
    topBarDetailsFormat: TopBarDetailsFormat,
    blurViews: Boolean,
    useCache: Boolean,
    useTapToNav: () -> Boolean,
    tags: () -> List<Tag>,
    selectedTags: () -> List<Tag>,
    onTagAdd: (name: String) -> Unit,
    onTagClick: (tag: Tag) -> Unit,
    onTagDelete: (tag: Tag) -> Unit,
    setTagMediaId: (id: Long) -> Unit,
    getExifData: suspend (context: Context, media: MediaStoreData) -> Map<MediaData, String>,
    allowedAlbumsFor: (moving: Boolean) -> List<KClass<out AlbumType>>,
    process: (context: Context, action: GenericFileManager.Action) -> Any?
) {
    val state = rememberPagerState(
        initialPage = startIndex
    ) {
        items.itemCount
    }

    var currentIndex by retain {
        mutableIntStateOf(
            startIndex
        )
    }

    LaunchedEffect(editId(), items.itemCount > 0) {
        if (items.itemCount <= 0) return@LaunchedEffect

        val end = (items.itemCount - 1).coerceAtMost(5)
        for (i in 0..end) {
            val item = items[i] as? PhotoLibraryUIModel.MediaImpl

            if (item?.item?.id == editId()) {
                state.scrollToPage(i)
                return@LaunchedEffect
            }
        }

        val left = (currentIndex - 5).coerceAtLeast(0)
        val right = (currentIndex + 5).coerceAtMost(items.itemCount - 1)
        for (i in left..right) {
            val item = items[i] as? PhotoLibraryUIModel.MediaImpl

            if (item?.item?.id == editId()) {
                state.scrollToPage(i)
                break
            }
        }
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

            setTagMediaId(mediaItem.id)
        }
    }

    LaunchedEffect(items.itemCount) {
        snapshotFlow { items.itemCount }.collectLatest {
            delay(PhotoGridConstants.LOADING_TIME_SHORT.milliseconds)
            if (items.itemCount == 0) launch(Dispatchers.Main) {
                navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
            }
        }
    }

    BackHandler(
        enabled = isOpenWithDefaultView
    ) {
        (context as Activity).finish()
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = retainSinglePhotoScrollState(isOpenWithView = false)
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    val draggableState = rememberDismissSinglePhotoState {
        scrollState.privacyMode
    }

    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = { mediaItem },
                visible = appBarsVisible.value,
                showInfoDialog = { showInfoDialog },
                privacyMode = { scrollState.privacyMode },
                isOpenWithDefaultView = isOpenWithDefaultView,
                showTags = true,
                showTagDialog = { showTagDialog },
                topBarDetailsFormat = topBarDetailsFormat,
                expandInfoDialog = {
                    coroutineScope.launch {
                        showTagDialog = false
                        showInfoDialog = true
                        delay(100.milliseconds)
                        sheetState.show()
                    }
                },
                expandTagDialog = {
                    coroutineScope.launch {
                        showInfoDialog = false
                        delay(50.milliseconds)
                        showTagDialog = !showTagDialog
                    }
                },
                modifier = Modifier
                    .barScaleModifier(draggableState)
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()

            BottomBar(
                visible = appBarsVisible.value,
                currentItem = { mediaItem },
                privacyMode = scrollState.privacyMode,
                isCustom = album is AlbumType.Custom || album is AlbumType.Cloud,
                confirmToDelete = confirmToDelete,
                doNotTrash = doNotTrash,
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
                                    uri = mediaItem.uri,
                                    dateTaken = mediaItem.dateTaken,
                                    album = album
                                )
                            )
                        } else {
                            navController.navigate(
                                Screens.VideoEditor(
                                    uri = mediaItem.uri,
                                    album = album
                                )
                            )
                        }
                    }
                },
                process = process,
                modifier = Modifier
                    .barScaleModifier(draggableState)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        var mediaData by remember { mutableStateOf(MediaData.Empty) }

        if (showInfoDialog) {
            // use mediaItem as key since we need to refresh this when the date/name/wtv changes not just index
            LaunchedEffect(mediaItem) {
                if (items.itemCount <= 0) return@LaunchedEffect

                val item = items[currentIndex]

                item as PhotoLibraryUIModel.MediaImpl

                mediaData = getExifData(context, item.item)
            }

            SinglePhotoInfoDialog(
                mediaItem = { mediaItem },
                mediaData = { mediaData },
                showMoveCopyOptions = true,
                sheetState = sheetState,
                privacyMode = { scrollState.privacyMode },
                album = { album },
                dismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInfoDialog = false
                    }
                },
                togglePrivacyMode = scrollState::togglePrivacyMode,
                allowedAlbumsFor = allowedAlbumsFor,
                process = process
            )
        }

        AnimatedMediaTagManager(
            showTagDialog = showTagDialog,
            appBarsVisible = appBarsVisible.value,
            tags = tags,
            selectedTags = selectedTags,
            onTagAdd = onTagAdd,
            onTagClick = onTagClick,
            onTagDelete = onTagDelete,
            onClose = {
                showTagDialog = false
            }
        )

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

            Box(
                modifier = Modifier
                    .anchoredDraggable(
                        state = draggableState.state,
                        orientation = Orientation.Vertical,
                        flingBehavior = draggableState.flingBehavior
                    )
            ) {
                HorizontalImageList(
                    items = items,
                    state = state,
                    window = window,
                    appBarsVisible = appBarsVisible,
                    scrollState = scrollState,
                    blurViews = { blurViews },
                    useBlackBackground = { useBlackBackground },
                    useCache = { useCache },
                    useTapToNav = useTapToNav,
                    swipeDownProgress = {
                        draggableState.progress
                    }
                )
            }
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
    confirmToDelete: Boolean,
    doNotTrash: Boolean,
    modifier: Modifier = Modifier,
    showEditingView: () -> Unit,
    process: (context: Context, action: GenericFileManager.Action) -> Any?
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
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
            modifier = modifier
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
                                    LavenderSnackbarEvent.MessageEvent(
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
                                    LavenderSnackbarEvent.MessageEvent(
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
                            if (!currentItem().isCloud) {
                                dirPermissionManager.start(
                                    directories = setOf(currentItem().absolutePath.parent())
                                )
                            } else {
                                showEditingView()
                            }
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
                        val item = currentItem()
                        process(
                            context,
                            GenericFileManager.Action.Share(
                                list = listOf(
                                    SelectionManager.SelectedItem(
                                        id = item.id,
                                        uri = item.uri,
                                        immichUrl = item.immichUrl,
                                        isImage = item.type == MediaType.Image,
                                        parentPath = item.parentPath
                                    )
                                )
                            )
                        )
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(id = R.string.media_share)
                    )
                }

                val filePermissionManager = rememberFilePermissionManager(
                    onGranted = {
                        val item = currentItem()
                        process(
                            context,
                            GenericFileManager.Action.Secure(
                                list = listOf(
                                    SelectionManager.SelectedItem(
                                        id = item.id,
                                        uri = item.uri,
                                        immichUrl = item.immichUrl,
                                        isImage = item.type == MediaType.Image,
                                        parentPath = item.parentPath
                                    )
                                )
                            )
                        )
                    }
                )

                val dirPermissionManager = rememberDirectoryPermissionManager(
                    onGranted = {
                        filePermissionManager.get(
                            uris = listOf(currentItem().uri.toUri())
                        )
                    }
                )

                var showMoveToSecureFolderDialog by remember { mutableStateOf(false) }
                if (showMoveToSecureFolderDialog) {
                    ConfirmationDialog(
                        title = stringResource(id = R.string.media_secure_confirm),
                        confirmButtonLabel = stringResource(id = R.string.media_secure),
                        action = {
                            dirPermissionManager.start(
                                directories = setOf(currentItem().absolutePath.parent())
                            )
                        },
                        onDismiss = {
                            showMoveToSecureFolderDialog = false
                        }
                    )
                }

                val motionPhoto = rememberMotionPhoto(uri = currentItem().uri.toUri())
                IconButton(
                    onClick = {
                        showMoveToSecureFolderDialog = true
                    },
                    enabled = !motionPhoto.isMotionPhoto.value && !privacyMode && !currentItem().isCloud
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.secure_folder),
                        contentDescription = stringResource(id = R.string.media_secure)
                    )
                }

                fun setFavourite(favourite: Boolean): PendingIntent? {
                    val item = currentItem()

                    return process(
                        context,
                        GenericFileManager.Action.Favourite(
                            list = listOf(
                                SelectionManager.SelectedItem(
                                    id = item.id,
                                    uri = item.uri,
                                    immichUrl = item.immichUrl,
                                    isImage = item.type == MediaType.Image,
                                    parentPath = item.parentPath
                                )
                            ),
                            favourite = favourite
                        )
                    ) as? PendingIntent?
                }

                val favState = if (currentItem().isCloud) {
                    rememberCloudFavouritesState(
                        media = currentItem(),
                        setFavourite = { setFavourite(it) }
                    )
                } else {
                    rememberLocalFavouritesState(
                        media = currentItem(),
                        setFavourite = { setFavourite(it) }
                    )
                }

                val vibratorManager = rememberVibratorManager()
                val isFavourited by favState.state.collectAsStateWithLifecycle()

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            vibratorManager.vibrateShort()

                            favState.favourite(favourite = !isFavourited)
                        }
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = if (isFavourited) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = stringResource(id = R.string.favourites_add_this)
                    )
                }

                // TODO: look into possibly sharing permission managers?
                val trashFilePermissionManager = rememberFilePermissionManager(
                    onGranted = {
                        val item = currentItem()
                        val list = listOf(
                            SelectionManager.SelectedItem(
                                id = item.id,
                                uri = item.uri,
                                immichUrl = item.immichUrl,
                                isImage = item.type == MediaType.Image,
                                parentPath = item.parentPath
                            )
                        )

                        process(
                            context,
                            if (doNotTrash && !isCustom) {
                                GenericFileManager.Action.Delete(list = list)
                            } else {
                                GenericFileManager.Action.Trash(
                                    list = list,
                                    trashed = true
                                )
                            }
                        )
                    }
                )

                var showDeleteDialog by remember { mutableStateOf(false) }
                if (showDeleteDialog) {
                    ConfirmationDialog(
                        title = stringResource(
                            id =
                                when {
                                    isCustom -> R.string.custom_album_remove_media_desc

                                    doNotTrash && !isCustom -> R.string.media_delete_permanently_confirm

                                    else -> R.string.media_delete_confirm
                                }
                        ),
                        confirmButtonLabel = stringResource(
                            id =
                                when {
                                    isCustom -> R.string.custom_album_remove_media

                                    else -> R.string.media_delete
                                }
                        ),
                        action = {
                            val item = currentItem()
                            val list = listOf(
                                SelectionManager.SelectedItem(
                                    id = item.id,
                                    uri = item.uri,
                                    immichUrl = item.immichUrl,
                                    isImage = item.type == MediaType.Image,
                                    parentPath = item.parentPath
                                )
                            )

                            if (item.isCloud) {
                                process(
                                    context,
                                    GenericFileManager.Action.Trash(
                                        list = list,
                                        trashed = true
                                    )
                                )
                            } else {
                                trashFilePermissionManager.get(
                                    uris = listOf(currentItem().uri.toUri())
                                )
                            }
                        },
                        onDismiss = {
                            showDeleteDialog = false
                        }
                    )
                }

                IconButton(
                    onClick = {
                        if (confirmToDelete) {
                            showDeleteDialog = true
                        } else {
                            val item = currentItem()
                            val list = listOf(
                                SelectionManager.SelectedItem(
                                    id = item.id,
                                    uri = item.uri,
                                    immichUrl = item.immichUrl,
                                    isImage = item.type == MediaType.Image,
                                    parentPath = item.parentPath
                                )
                            )

                            if (item.isCloud) {
                                process(
                                    context,
                                    GenericFileManager.Action.Trash(
                                        list = list,
                                        trashed = true
                                    )
                                )
                            } else {
                                trashFilePermissionManager.get(
                                    uris = listOf(currentItem().uri.toUri())
                                )
                            }
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


