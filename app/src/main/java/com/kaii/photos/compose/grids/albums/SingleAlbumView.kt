package com.kaii.photos.compose.grids.albums

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.album_view.SingleAlbumViewBottomBar
import com.kaii.photos.compose.app_bars.album_view.SingleAlbumViewTopBar
import com.kaii.photos.compose.dialogs.AlbumInfoDialog
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.compose.widgets.tags.AnimatedMediaTagManager
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.rememberCustomSelectionManager
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.tag_page.TagViewModel
import com.kaii.photos.models.tag_page.TagViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SingleAlbumView(
    album: AlbumType.Folder,
    viewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val navController = LocalNavController.current
    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.find { it.id == album.id } as? AlbumType.Folder
        }
    }

    LaunchedEffect(dynamicAlbum) {
        delay(1000)
        if (dynamicAlbum == null) navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
    }

    if (dynamicAlbum == null) return

    LaunchedEffect(dynamicAlbum) {
        viewModel.changePaths(album = dynamicAlbum!!)
    }

    val selectionManager = rememberSelectionManager(paths = dynamicAlbum!!.paths)
    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        selectionManager.selection.collectLatest { selectedItems ->
            tagViewModel.setMediaIds(
                ids = selectedItems.fastMap { it.id }
            )
        }
    }

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()
    val autoDetectAlbums by viewModel.autoDetectAlbums.collectAsStateWithLifecycle()

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        album = { dynamicAlbum!! },
        albums = { allAlbums },
        autoDetectAlbums = { autoDetectAlbums },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.Album,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        tags = tags,
        selectedTags = selectedTags,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize,
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        editAlbum = viewModel::editAlbum,
        removeAlbum = viewModel::removeAlbum
    )
}

@Composable
fun SingleAlbumView(
    album: AlbumType.Custom,
    viewModel: CustomAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val navController = LocalNavController.current
    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.find { it.id == album.id } as? AlbumType.Custom
        }
    }

    LaunchedEffect(dynamicAlbum) {
        delay(1000)
        if (dynamicAlbum == null) navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
    }

    if (dynamicAlbum == null) return

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberCustomSelectionManager(albumId = album.id)

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()
    val autoDetectAlbums by viewModel.autoDetectAlbums.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        selectionManager.selection.collectLatest { selectedItems ->
            tagViewModel.setMediaIds(
                ids = selectedItems.fastMap { it.id }
            )
        }
    }

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        album = { dynamicAlbum!! },
        albums = { allAlbums },
        autoDetectAlbums = { autoDetectAlbums },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.CustomAlbum,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        tags = tags,
        selectedTags = selectedTags,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize,
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        editAlbum = viewModel::editAlbum,
        removeAlbum = viewModel::removeAlbum
    )
}

@Composable
fun SingleAlbumView(
    album: AlbumType.Cloud,
    viewModel: ImmichAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val navController = LocalNavController.current
    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.find { it.id == album.id } as? AlbumType.Cloud
        }
    }

    LaunchedEffect(dynamicAlbum) {
        delay(1000)
        if (dynamicAlbum == null) navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
    }

    if (dynamicAlbum == null) return

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberCustomSelectionManager(albumId = album.id)

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()
    val autoDetectAlbums by viewModel.autoDetectAlbums.collectAsStateWithLifecycle()

    val tagViewModel = viewModel<TagViewModel>(
        factory = TagViewModelFactory(
            context = LocalContext.current
        )
    )

    val tags by tagViewModel.tags.collectAsStateWithLifecycle()
    val selectedTags by tagViewModel.appliedTags.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        selectionManager.selection.collectLatest { selectedItems ->
            tagViewModel.setMediaIds(
                ids = selectedItems.fastMap { it.id }
            )
        }
    }

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        album = { dynamicAlbum!! },
        albums = { allAlbums },
        autoDetectAlbums = { autoDetectAlbums },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.Immich,
        tags = tags,
        selectedTags = selectedTags,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize,
        onTagAdd = tagViewModel::insertTag,
        onTagClick = tagViewModel::toggleTag,
        onTagDelete = tagViewModel::deleteTag,
        editAlbum = viewModel::editAlbum,
        removeAlbum = viewModel::removeAlbum
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    album: () -> AlbumType,
    albums: () -> List<AlbumType>,
    autoDetectAlbums: () -> Boolean,
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    viewProperties: ViewProperties,
    columnSize: Int,
    openVideosExternally: Boolean,
    cacheThumbnails: Boolean,
    thumbnailSize: Int,
    useRoundedCorners: Boolean,
    confirmToDelete: Boolean,
    doNotTrash: Boolean,
    preserveDate: Boolean,
    modifier: Modifier = Modifier,
    tags: List<Tag>,
    selectedTags: List<Tag>,
    mediaCount: suspend () -> Int,
    albumSize: suspend () -> String,
    onTagAdd: (name: String) -> Unit,
    onTagClick: (tag: Tag) -> Unit,
    onTagDelete: (tag: Tag) -> Unit,
    editAlbum: (id: String, newInfo: AlbumType) -> Unit,
    removeAlbum: (id: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        val vibratorManager = rememberVibratorManager()
        AlbumInfoDialog(
            albumInfo = album,
            albums = albums,
            autoDetectAlbums = autoDetectAlbums,
            sheetState = sheetState,
            itemCount = mediaCount,
            albumSize = albumSize,
            toggleSelectionMode = {
                vibratorManager.vibrateShort()
                selectionManager.enterSelectMode()
                coroutineScope.launch {
                    sheetState.hide()
                    showInfoDialog = false
                }
            },
            editAlbum = editAlbum,
            removeAlbum = {
                coroutineScope.launch {
                    sheetState.hide()
                    removeAlbum(it)
                    showInfoDialog = false
                }
            },
            dismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showInfoDialog = false
                }
            }
        )
    }

    var showTagDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = album,
                selectionManager = selectionManager,
                isMediaPicker = incomingIntent != null,
                showTagDialog = showTagDialog,
                setShowTagDialog = { showTagDialog = it },
                showDialog = {
                    coroutineScope.launch {
                        showInfoDialog = true
                        delay(50)
                        sheetState.expand()
                    }
                }
            )
        },
        bottomBar = {
            val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

            AnimatedVisibility(
                visible = isSelecting,
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                SingleAlbumViewBottomBar(
                    albumInfo = album,
                    selectionManager = selectionManager,
                    incomingIntent = incomingIntent,
                    confirmToDelete = confirmToDelete,
                    doNotTrash = doNotTrash,
                    preserveDate = preserveDate
                )
            }
        }
    ) { padding ->
        AnimatedMediaTagManager(
            showTagDialog = showTagDialog,
            padding = padding,
            tags = tags,
            selectedTags = selectedTags,
            onTagAdd = onTagAdd,
            onTagClick = onTagClick,
            onTagDelete = onTagDelete,
            onClose = {
                showTagDialog = false
            }
        )

        val isLandscape by rememberDeviceOrientation()
        val safeDrawingPadding = if (isLandscape) {
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

            val layoutDirection = LocalLayoutDirection.current
            val left = safeDrawing.calculateStartPadding(layoutDirection)
            val right = safeDrawing.calculateEndPadding(layoutDirection)

            Pair(left, right)
        } else {
            Pair(0.dp, 0.dp)
        }

        Column(
            modifier = Modifier
                .padding(
                    start = safeDrawingPadding.first,
                    top = padding.calculateTopPadding(),
                    end = safeDrawingPadding.second,
                    bottom = 0.dp
                )
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                pagingItems = pagingItems,
                album = album(),
                selectionManager = selectionManager,
                viewProperties = viewProperties,
                isMediaPicker = incomingIntent != null,
                columnSize = columnSize,
                openVideosExternally = openVideosExternally,
                cacheThumbnails = cacheThumbnails,
                thumbnailSize = thumbnailSize,
                useRoundedCorners = useRoundedCorners,
            )
        }
    }
}


