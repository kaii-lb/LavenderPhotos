package com.kaii.photos.compose.grids

import android.view.Window
import android.view.WindowManager
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SecureFolderViewBottomAppBar
import com.kaii.photos.compose.app_bars.SecureFolderViewTopAppBar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.secure_folder.SecureFolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// private const val TAG = "com.kaii.photos.compose.grids.LockedFolderView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderView(
    window: Window,
    viewModel: SecureFolderViewModel
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val isGettingPermissions = rememberSaveable {
        mutableStateOf(false)
    }

    DisposableEffect(lifecycleState) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        // if not navigating to single photo view
                        if (navController.currentBackStackEntry?.destination?.hasRoute(Screens.SecureFolder.SinglePhoto::class) != true
                            && !isGettingPermissions.value
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) context.appModule.scope.launch(Dispatchers.IO) {
                                File(context.appSecureVideoCacheDir).listFiles()?.forEach {
                                    it.delete()
                                }
                            }

                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            navController.popBackStack(route = Screens.MainPages.MainGrid.GridView::class, inclusive = false)
                        }
                    }

                    else -> {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberSelectionManager(pagingItems = items)

    Scaffold(
        topBar = {
            SecureFolderViewTopAppBar(selectionManager = selectionManager) {
                navController.popBackStack()
            }
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
                SecureFolderViewBottomAppBar(
                    selectionManager = selectionManager,
                    isGettingPermissions = isGettingPermissions
                )
            }
        },
        modifier = Modifier
            .fillMaxSize(1f),
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

        Column(
            modifier = Modifier
                .padding(
                    start = safeDrawingPadding.first,
                    top = padding.calculateTopPadding(),
                    end = safeDrawingPadding.second,
                    bottom = 0.dp
                )
                .fillMaxSize(1f)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
            val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
            val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
            val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
            val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()

            PhotoGrid(
                pagingItems = items,
                albumInfo = AlbumInfo.Empty,
                selectionManager = selectionManager,
                viewProperties = ViewProperties.SecureFolder,
                columnSize = columnSize,
                openVideosExternally = openVideosExternally,
                cacheThumbnails = cacheThumbnails,
                thumbnailSize = thumbnailSize,
                useRoundedCorners = useRoundedCorners,
            )
        }
    }
}

