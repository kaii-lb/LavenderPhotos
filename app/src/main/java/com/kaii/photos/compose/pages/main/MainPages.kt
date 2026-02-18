package com.kaii.photos.compose.pages.main

import android.content.Intent
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.MediaPickerConfirmButton
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.setupNextScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainPages(
    mainPhotosPaths: Set<String>,
    multiAlbumViewModel: MultiAlbumViewModel,
    searchViewModel: SearchViewModel,
    window: Window,
    incomingIntent: Intent?
) {
    val mainViewModel = LocalMainViewModel.current

    val defaultTab by mainViewModel.defaultTab.collectAsStateWithLifecycle()
    val tabList by mainViewModel.tabList.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = tabList.indexOf(defaultTab)
    ) { tabList.size }

    val exitImmediately by mainViewModel.settings.behaviour.getExitImmediately().collectAsStateWithLifecycle(initialValue = false)

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

    val scrollBehaviour = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom
    )

    var paths by remember { mutableStateOf(mainPhotosPaths) }
    val selectionManager = rememberSelectionManager(paths = paths)
    val albumGridState = rememberAlbumGridState()

    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            MainAppTopBar(
                alternate = isSelecting,
                selectionManager = selectionManager,
                pagerState = pagerState
            )
        },
        bottomBar = {
            if (incomingIntent == null) {
                MainAppBottomBar(
                    pagerState = pagerState,
                    selectionManager = selectionManager,
                    tabs = tabList,
                    defaultTab = defaultTab,
                    scrollBehaviour = scrollBehaviour
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(
                object : NestedScrollConnection {
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                        scrollBehaviour.onPostScroll(
                            consumed,
                            available,
                            source
                        )

                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        scrollBehaviour.onPreScroll(
                            available,
                            source
                        )

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                        scrollBehaviour.onPostFling(
                            consumed,
                            available
                        )

                    override suspend fun onPreFling(available: Velocity): Velocity =
                        scrollBehaviour.onPreFling(available)
                }
            )
    ) { padding ->
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

        Box {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isSelecting,
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
                        LaunchedEffect(Unit) {
                            setupNextScreen(window = window)
                            selectionManager.clear()
                            paths = tab.albumPaths

                            multiAlbumViewModel.update(
                                album = tab.toAlbumInfo()
                            )
                        }

                        MainGridView(
                            viewModel = multiAlbumViewModel,
                            albumInfo = tab.toAlbumInfo(),
                            selectionManager = selectionManager,
                            isMediaPicker = incomingIntent != null
                        )
                    }

                    tab == DefaultTabs.TabTypes.photos -> {
                        LaunchedEffect(Unit) {
                            setupNextScreen(window = window)
                            selectionManager.clear()
                            paths = mainPhotosPaths

                            multiAlbumViewModel.update(
                                album = tab.copy(albumPaths = mainPhotosPaths).toAlbumInfo()
                            )
                        }

                        MainGridView(
                            viewModel = multiAlbumViewModel,
                            selectionManager = selectionManager,
                            albumInfo = tab.copy(albumPaths = mainPhotosPaths).toAlbumInfo(),
                            isMediaPicker = incomingIntent != null
                        )
                    }

                    tab == DefaultTabs.TabTypes.secure -> {
                        SecureFolderEntryPage()
                    }

                    tab == DefaultTabs.TabTypes.albums -> {
                        AlbumsGridView(
                            gridState = albumGridState,
                            isMediaPicker = incomingIntent != null
                        )
                    }

                    tab == DefaultTabs.TabTypes.search -> {
                        LaunchedEffect(Unit) {
                            selectionManager.clear()
                            paths = emptySet()
                            setupNextScreen(window = window)
                        }

                        SearchPage(
                            searchViewModel = searchViewModel,
                            selectionManager = selectionManager,
                            isMediaPicker = incomingIntent != null
                        )
                    }
                }
            }

            if (incomingIntent != null) {
                val context = LocalContext.current
                val navController = LocalNavController.current

                val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())
                val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

                AnimatedContent(
                    targetState = isSelecting && navController.currentBackStackEntry?.destination?.hasRoute(Screens.MainPages::class) == true,
                    transitionSpec = {
                        getAppBarContentTransition(isSelecting)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) { state ->
                    if (!state) {
                        MainAppBottomBar(
                            pagerState = pagerState,
                            tabs = tabList.fastFilter { it != DefaultTabs.TabTypes.secure },
                            defaultTab = defaultTab,
                            scrollBehaviour = scrollBehaviour,
                            selectionManager = selectionManager
                        )
                    } else {
                        MediaPickerConfirmButton(
                            incomingIntent = incomingIntent,
                            uris = selectedItemsList.fastMap { it.toUri() },
                            contentResolver = context.contentResolver
                        )
                    }
                }
            }
        }
    }
}