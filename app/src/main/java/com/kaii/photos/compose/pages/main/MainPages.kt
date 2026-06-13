package com.kaii.photos.compose.pages.main

import android.content.Intent
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberFloatingToolbarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.compose.widgets.tags.AnimatedMediaTagManager
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.tag_page.TagViewModel
import com.kaii.photos.models.tag_page.TagViewModelFactory
import com.kaii.photos.setupNextScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainPages(
    viewModel: MainGridViewModel,
    searchViewModel: SearchViewModel,
    deviceAlbums: () -> List<AlbumGridState.Album>,
    window: Window,
    incomingIntent: Intent?,
    refreshAlbums: suspend () -> Unit
) {
    val defaultTab by viewModel.defaultTab.collectAsStateWithLifecycle()

    val originalTabList by viewModel.tabList.collectAsStateWithLifecycle()
    val tabList by remember {
        derivedStateOf {
            originalTabList.fastFilter { it != DefaultTabs.TabTypes.secure }
        }
    }

    val immichInfo by viewModel.immichInfo.collectAsStateWithLifecycle()
    val exitImmediately by viewModel.exitImmediately.collectAsStateWithLifecycle()
    val mainPhotosPaths by viewModel.mainPhotosAlbums.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = if (tabList.indexOf(defaultTab) == -1) 0 else tabList.indexOf(defaultTab)
    ) { tabList.size }

    val coroutineScope = rememberCoroutineScope()
    val windowWidth = LocalWindowInfo.current.containerSize.width.toFloat()

    BackHandler(
        enabled = !exitImmediately && pagerState.settledPage != tabList.indexOf(defaultTab)
    ) {
        coroutineScope.launch {
            pagerState.animateScrollBy(
                value = windowWidth * -(pagerState.currentPage - tabList.indexOf(defaultTab)) - pagerState.currentPageOffsetFraction * windowWidth,
                animationSpec = AnimationConstants.defaultSpring()
            )
        }
    }

    val floatingBarState = rememberFloatingToolbarState()
    val scrollBehaviour = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
        state = floatingBarState
    )

    var paths by remember { mutableStateOf(mainPhotosPaths) }
    val selectionManager = rememberSelectionManager(paths = { paths })

    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)
    var showTagDialog by remember { mutableStateOf(false) }

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

    var delayOver by remember { mutableStateOf(false) }
    LaunchedEffect(isSelecting) {
        if (!isSelecting) {
            showTagDialog = false
        } else if (floatingBarState.offset != 0f) {
            delayOver = false
            delay(AnimationConstants.DURATION.toLong())
            floatingBarState.offset = 0f
            delayOver = true
        }
    }

    Scaffold(
        topBar = {
            val groups by viewModel.groups.collectAsStateWithLifecycle()
            val extraSecureFolderEntry by viewModel.extraSecureFolderNavEntry.collectAsStateWithLifecycle()

            MainAppTopBar(
                alternate = { isSelecting },
                selectionManager = selectionManager,
                immichInfo = { immichInfo },
                showAddAlbumButton = {
                    tabList.isNotEmpty() && tabList[pagerState.settledPage] == DefaultTabs.TabTypes.albums
                },
                extraSecureFolderEntry = { extraSecureFolderEntry },
                showTagDialog = { showTagDialog },
                isFromMediaPicker = incomingIntent != null,
                groups = { groups },
                setShowTagDialog = {
                    showTagDialog = true
                },
                addAlbum = viewModel::addAlbum,
                addGroup = viewModel::addGroup
            )
        },
        bottomBar = {
            LaunchedEffect(Unit) {
                floatingBarState.offset = 0f
                delay(AnimationConstants.DURATION.toLong())
                delayOver = true
            }

            AnimatedVisibility(
                visible = tabList.isNotEmpty() && incomingIntent == null && delayOver,
                enter = fadeIn() + slideInVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it },
                exit = fadeOut() + slideOutVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it }
            ) {
                val context = LocalContext.current
                MainAppBottomBar(
                    pagerState = pagerState,
                    selectionManager = selectionManager,
                    tabs = tabList,
                    defaultTab = { defaultTab },
                    scrollBehaviour = scrollBehaviour,
                    confirmToDelete = { confirmToDelete },
                    doNotTrash = { doNotTrash },
                    allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
                    process = { action ->
                        viewModel.runAction(
                            context = context,
                            action = action
                        )
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(
                object : NestedScrollConnection {
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                        if (isSelecting) Offset.Zero
                        else scrollBehaviour.onPostScroll(
                            consumed,
                            available,
                            source
                        )

                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        if (isSelecting) Offset.Zero
                        else scrollBehaviour.onPreScroll(
                            available,
                            source
                        )

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                        if (isSelecting) Velocity.Zero
                        else scrollBehaviour.onPostFling(
                            consumed,
                            available
                        )

                    override suspend fun onPreFling(available: Velocity): Velocity =
                        if (isSelecting) Velocity.Zero
                        else scrollBehaviour.onPreFling(available)
                }
            )
    ) { padding ->
        AnimatedMediaTagManager(
            showTagDialog = showTagDialog,
            padding = padding,
            tags = { tags },
            selectedTags = { selectedTags },
            onTagAdd = tagViewModel::insertTag,
            onTagClick = tagViewModel::toggleTag,
            onTagDelete = tagViewModel::deleteTag,
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

        var lastPage by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(pagerState) {
            coroutineScope.launch {
                snapshotFlow { pagerState.currentPage }.collectLatest {
                    setupNextScreen(window = window)
                    selectionManager.clear()

                    if (lastPage != tabList.indexOf(DefaultTabs.TabTypes.search)) {
                        searchViewModel.clear()
                    }

                    if (it == tabList.indexOf(DefaultTabs.TabTypes.albums)) {
                        refreshAlbums()
                    }

                    lastPage = it
                }
            }
        }

        LifecycleStartEffect(Unit) {
            var canRefresh = true

            coroutineScope.launch {
                while (canRefresh) {
                    refreshAlbums()
                    delay(5000)
                }
            }

            onStopOrDispose {
                canRefresh = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSelecting,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .padding(
                        start = safeDrawingPadding.first,
                        top = padding.calculateTopPadding(),
                        end = safeDrawingPadding.second,
                        bottom = 0.dp
                    )
            ) { index ->
                val tab = tabList[index]

                when {
                    tab.isCustom -> {
                        val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
                        val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
                        val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
                        val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
                        val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
                        val vibrateOnClick by viewModel.vibrateOnClick.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            paths = tab.albumPaths

                            viewModel.changeAlbum(album = tab.toAlbum())
                        }

                        MainGridView(
                            viewModel = viewModel,
                            album = { tab.toAlbum() },
                            selectionManager = selectionManager,
                            isMediaPicker = incomingIntent != null,
                            columnSize = { columnSize },
                            openVideosExternally = { openVideosExternally },
                            cacheThumbnails = { cacheThumbnails },
                            thumbnailSize = { thumbnailSize },
                            useRoundedCorners = { useRoundedCorners },
                            vibrateOnClick = { vibrateOnClick }
                        )
                    }

                    tab == DefaultTabs.TabTypes.photos -> {
                        val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
                        val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
                        val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
                        val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
                        val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
                        val vibrateOnClick by viewModel.vibrateOnClick.collectAsStateWithLifecycle()

                        LaunchedEffect(mainPhotosPaths) {
                            paths = mainPhotosPaths

                            viewModel.changeAlbum(album = tab.toAlbum(paths = mainPhotosPaths))
                        }

                        MainGridView(
                            viewModel = viewModel,
                            album = { tab.copy(albumPaths = mainPhotosPaths).toAlbum() },
                            selectionManager = selectionManager,
                            isMediaPicker = incomingIntent != null,
                            columnSize = { columnSize },
                            openVideosExternally = { openVideosExternally },
                            cacheThumbnails = { cacheThumbnails },
                            thumbnailSize = { thumbnailSize },
                            useRoundedCorners = { useRoundedCorners },
                            vibrateOnClick = { vibrateOnClick }
                        )
                    }

                    tab == DefaultTabs.TabTypes.secure -> {
                        SecureFolderEntryPage()
                    }

                    tab == DefaultTabs.TabTypes.albums -> {
                        val columnSize by viewModel.albumColumnSize.collectAsStateWithLifecycle()
                        val sortMode by viewModel.albumSortMode.collectAsStateWithLifecycle()
                        val migrateFav by viewModel.migrateFav.collectAsStateWithLifecycle()
                        val autoDetect by viewModel.autoDetect.collectAsStateWithLifecycle()

                        AlbumsGridView(
                            deviceAlbums = deviceAlbums,
                            sortMode = { sortMode },
                            tabList = { tabList },
                            columnSize = columnSize,
                            immichInfo = { immichInfo },
                            migrateFav = { migrateFav },
                            autoDetect = { autoDetect },
                            isMediaPicker = incomingIntent != null,
                            setAlbumSortMode = viewModel::setAlbumSortMode,
                            setAlbumOrder = viewModel::setAlbumOrder,
                            addAlbumToGroup = viewModel::addAlbumToGroup,
                            toggleAlbumPin = viewModel::toggleAlbumPin,
                            deleteAlbum = viewModel::deleteAlbum
                        )
                    }

                    tab == DefaultTabs.TabTypes.search -> {
                        LaunchedEffect(Unit) {
                            paths = emptySet()
                        }

                        SearchPage(
                            viewModel = searchViewModel,
                            selectionManager = selectionManager,
                            isMediaPicker = incomingIntent != null
                        )
                    }
                }
            }

            if (incomingIntent != null) {
                val context = LocalContext.current
                val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())
                val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

                AnimatedContent(
                    targetState = isSelecting,
                    transitionSpec = {
                        getAppBarContentTransition(isSelecting)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) { state ->
                    if (!state) {
                        AnimatedVisibility(
                            visible = tabList.isNotEmpty(),
                            enter = fadeIn() + slideInVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it },
                            exit = fadeOut() + slideOutVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it }
                        ) {
                            MainAppBottomBar(
                                pagerState = pagerState,
                                tabs = tabList,
                                defaultTab = { defaultTab },
                                scrollBehaviour = scrollBehaviour,
                                selectionManager = selectionManager,
                                confirmToDelete = { confirmToDelete },
                                doNotTrash = { doNotTrash },
                                allowedAlbumsFor = viewModel::allowedAlbumTypesFor,
                                process = { action ->
                                    viewModel.runAction(
                                        context = context,
                                        action = action
                                    )
                                }
                            )
                        }
                    } else {
                        MediaPickerConfirmButton(
                            incomingIntent = incomingIntent,
                            uris = selectedItemsList.fastMap { it.uri.toUri() },
                            contentResolver = context.contentResolver
                        )
                    }
                }
            }
        }
    }
}