package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
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
import com.kaii.photos.compose.modifiers.singlePhotoBottomBarProperties
import com.kaii.photos.compose.modifiers.singlePhotoProperties
import com.kaii.photos.compose.modifiers.singlePhotoTopBarProperties
import com.kaii.photos.compose.side_effects.FileOperationProgressEffect
import com.kaii.photos.compose.side_effects.SharePhotoEffect
import com.kaii.photos.compose.widgets.tags.AnimatedMediaTagManager
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.scrolling.retainSinglePhotoScrollState
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.toFileOperationMetadata
import com.kaii.photos.models.CustomAlbumViewModel
import com.kaii.photos.models.FavouritesViewModel
import com.kaii.photos.models.ImmichAlbumViewModel
import com.kaii.photos.models.MainGridViewModel
import com.kaii.photos.models.MultiAlbumViewModel
import com.kaii.photos.models.SearchViewModel
import com.kaii.photos.models.tag_page.TagViewModel
import com.kaii.photos.models.tag_page.TagViewModelFactory
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.permissions.files.rememberDynamicActivityResultLauncher
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import com.kaii.photos.presentation.single_photos_views.rememberDismissSinglePhotoState
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        exifData = { exifData },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        exifData = { exifData },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        exifData = { exifData },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        exifData = { exifData },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        exifData = { exifData },
        selectedTags = { selectedTags },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
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
    val exifData by viewModel.exifData.collectAsStateWithLifecycle()

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
        shareFlow = viewModel.fileShareIntent,
        fileOperationProgress = viewModel.fileOperationProgress,
        useBlackBackground = { useBlackBackground },
        confirmToDelete = { confirmToDelete },
        doNotTrash = { doNotTrash },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { tapToNav },
        tags = { tags },
        selectedTags = { selectedTags },
        exifData = { exifData },
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        setTagMediaId = tagViewModel::setMediaId,
        runAction = viewModel::runAction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    shareFlow: Flow<Result<Intent, FileOperationError>>,
    fileOperationProgress: Flow<FileOperationProgress<Unit>>,
    useBlackBackground: () -> Boolean,
    confirmToDelete: () -> Boolean,
    doNotTrash: () -> Boolean,
    topBarDetailsFormat: TopBarDetailsFormat,
    blurViews: () -> Boolean,
    useCache: () -> Boolean,
    useTapToNav: () -> Boolean,
    exifData: () -> Result<Map<MediaData, String>, FileOperationError>,
    tags: () -> List<Tag>,
    selectedTags: () -> List<Tag>,
    onTagAdd: (name: String) -> Unit,
    onTagClick: (tag: Tag) -> Unit,
    onTagDelete: (tag: Tag) -> Unit,
    setTagMediaId: (id: Long) -> Unit,
    runAction: (action: FileOperationAction) -> Unit
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
        if (items.itemCount <= 0 || editId() == null) return@LaunchedEffect

        repeat(3) {
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
                    return@LaunchedEffect
                }
            }

            delay(500.milliseconds) // wait to allow mediastore to update
        }
    }

    val context = LocalContext.current
    val appBarsVisible = remember { mutableStateOf(true) }
    var mediaItem by remember { mutableStateOf(MediaStoreData.dummyItem) }

    LaunchedEffect(currentIndex, items, items.itemSnapshotList) {
        mediaItem =
            if (currentIndex in 0..<items.itemCount && items.itemCount != 0) {
                ((items[currentIndex] as? PhotoLibraryUIModel.MediaImpl))?.item ?: MediaStoreData.dummyItem
            } else {
                MediaStoreData.dummyItem
            }

        setTagMediaId(mediaItem.id)
    }

    LaunchedEffect(items.itemCount) {
        snapshotFlow { items.itemCount }.collectLatest {
            delay(PhotoGridConstants.LOADING_TIME_SHORT.milliseconds)
            if (items.itemCount == 0) launch(Dispatchers.Main) {
                navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
            }
        }
    }

    val dynamicActivityResultLauncher = rememberDynamicActivityResultLauncher()
    SharePhotoEffect(
        shareFlow = shareFlow,
        dynamicActivityResultLauncher = dynamicActivityResultLauncher,
        reShare = { files ->
            runAction(
                FileOperationAction.Share(
                    files = files
                )
            )
        }
    )

    FileOperationProgressEffect(
        operationFlow = fileOperationProgress,
        dynamicActivityResultLauncher = dynamicActivityResultLauncher,
        runAction = runAction
    )

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

    val (firstFR, secondFR, thirdFR) = remember { FocusRequester.createRefs() }

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
                    .singlePhotoTopBarProperties(
                        draggableState = draggableState,
                        firstFR = firstFR,
                        secondFR = secondFR
                    )
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()

            BottomBar(
                visible = appBarsVisible.value,
                currentItem = { mediaItem },
                privacyMode = scrollState.privacyMode,
                isCustom = album is AlbumType.Custom || album is AlbumType.Cloud,
                confirmToDelete = confirmToDelete(),
                doNotTrash = doNotTrash(),
                album = { album },
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
                runAction = runAction,
                modifier = Modifier
                    .singlePhotoBottomBarProperties(
                        draggableState = draggableState,
                        secondFR = secondFR,
                        thirdFR = thirdFR
                    )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        if (showInfoDialog) {
            // use mediaItem as key since we need to refresh this when the date/name/wtv changes not just index
            LaunchedEffect(mediaItem) {
                if (items.itemCount <= 0) return@LaunchedEffect

                val item = items[currentIndex]

                item as PhotoLibraryUIModel.MediaImpl

                runAction(
                    FileOperationAction.LoadExifData(
                        file = item.item.toFileOperationMetadata()
                    )
                )
            }

            SinglePhotoInfoDialog(
                mediaItem = { mediaItem },
                mediaData = exifData,
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
                runAction = runAction
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
                .background(if (useBlackBackground()) Color.Black else MaterialTheme.colorScheme.background)
                .fillMaxSize(),
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
                scrollState = scrollState,
                blurViews = blurViews,
                useBlackBackground = useBlackBackground,
                useCache = useCache,
                useTapToNav = useTapToNav,
                swipeDownProgress = {
                    draggableState.progress
                },
                modifier = Modifier
                    .singlePhotoProperties(
                        state = state,
                        draggableState = draggableState,
                        firstFR = firstFR,
                        secondFR = secondFR,
                        thirdFR = thirdFR,
                        isVideo = { mediaItem.type == MediaType.Video }
                    )
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
    confirmToDelete: Boolean,
    doNotTrash: Boolean,
    album: () -> AlbumType,
    modifier: Modifier = Modifier,
    showEditingView: () -> Unit,
    runAction: (action: FileOperationAction) -> Any?
) {
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
                        runAction(
                            FileOperationAction.Share(
                                files = listOf(
                                    FileOperationItemMetadata(
                                        id = item.id,
                                        uri = item.uri,
                                        absolutePath = item.absolutePath,
                                        immichUrl = item.immichUrl,
                                        isImage = item.type == MediaType.Image
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
                        runAction(
                            FileOperationAction.Secure(
                                files = listOf(
                                    FileOperationItemMetadata(
                                        id = item.id,
                                        uri = item.uri,
                                        absolutePath = item.absolutePath,
                                        immichUrl = item.immichUrl,
                                        isImage = item.type == MediaType.Image
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

                val vibratorManager = rememberVibratorManager()
                val isFavourited = remember(currentItem()) {
                    currentItem().favourited
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            vibratorManager.vibrateShort()

                            val item = currentItem()
                            runAction(
                                FileOperationAction.Favourite(
                                    files = listOf(
                                        FileOperationItemMetadata(
                                            id = item.id,
                                            uri = item.uri,
                                            absolutePath = item.absolutePath,
                                            immichUrl = item.immichUrl,
                                            isImage = item.type == MediaType.Image
                                        )
                                    ),
                                    isFavourite = !item.favourited,
                                    album = album()
                                )
                            )
                        }
                    },
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = if (isFavourited) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = stringResource(id = R.string.favourites_add_this)
                    )
                }

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
                            val files = listOf(
                                FileOperationItemMetadata(
                                    id = item.id,
                                    uri = item.uri,
                                    absolutePath = item.absolutePath,
                                    immichUrl = item.immichUrl,
                                    isImage = item.type == MediaType.Image
                                )
                            )

                            runAction(
                                if (doNotTrash && !isCustom) {
                                    FileOperationAction.Delete(
                                        files = files,
                                        album = album()
                                    )
                                } else {
                                    FileOperationAction.Trash(
                                        files = files,
                                        isTrashed = true,
                                        album = album()
                                    )
                                }
                            )
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
                                FileOperationItemMetadata(
                                    id = item.id,
                                    uri = item.uri,
                                    absolutePath = item.absolutePath,
                                    immichUrl = item.immichUrl,
                                    isImage = item.type == MediaType.Image
                                )
                            )

                            runAction(
                                FileOperationAction.Trash(
                                    files = list,
                                    isTrashed = true,
                                    album = album()
                                )
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


