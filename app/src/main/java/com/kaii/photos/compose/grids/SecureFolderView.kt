package com.kaii.photos.compose.grids

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SecureFolderViewBottomAppBar
import com.kaii.photos.compose.app_bars.SecureFolderViewTopAppBar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.secure_folder.SecureFolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// private const val TAG = "com.kaii.photos.compose.grids.LockedFolderView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderView(
    window: Window,
    viewModel: SecureFolderViewModel
) {
    val context = LocalContext.current

    val selectedItemsList = remember { SnapshotStateList<PhotoLibraryUIModel>() }
    val navController = LocalNavController.current

    val lifecycleOwner = LocalLifecycleOwner.current
    var lastLifecycleState by rememberSaveable {
        mutableStateOf(Lifecycle.State.STARTED)
    }
    var hideSecureFolder by rememberSaveable {
        mutableStateOf(false)
    }
    val isGettingPermissions = rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler {
        viewModel.stopFileObserver()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        viewModel.attachFileObserver()
    }

    LaunchedEffect(hideSecureFolder, lastLifecycleState) {
        if (hideSecureFolder
            && navController.currentBackStackEntry?.destination?.hasRoute(Screens.SecureFolder.SinglePhoto::class) == false
        ) {
            viewModel.stopFileObserver()
            navController.navigate(MultiScreenViewType.MainScreen.name)
        }

        if (lastLifecycleState == Lifecycle.State.DESTROYED) {
            withContext(Dispatchers.IO) {
                File(context.appSecureVideoCacheDir).listFiles()?.forEach {
                    it.delete()
                }
            }
        }
    }

    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState()
    DisposableEffect(lifecycleState.value, isGettingPermissions.value) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        if (navController.currentBackStackEntry?.destination?.hasRoute(Screens.SecureFolder.SinglePhoto::class) == false
                            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                            && !isGettingPermissions.value
                        ) {
                            lastLifecycleState = Lifecycle.State.DESTROYED
                        }
                    }

                    Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START, Lifecycle.Event.ON_CREATE -> {
                        if (lastLifecycleState == Lifecycle.State.DESTROYED && navController.currentBackStackEntry != null && !isGettingPermissions.value) {
                            lastLifecycleState = Lifecycle.State.STARTED
                            viewModel.attachFileObserver()

                            hideSecureFolder = true
                        }
                    }

                    else -> {}
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    if (hideSecureFolder) return

    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    val hasFiles by remember { derivedStateOf {
        items.itemSnapshotList.isNotEmpty()
    }}

    Scaffold(
        topBar = {
            SecureFolderViewTopAppBar(
                pagingItems = items,
                selectedItemsList = selectedItemsList
            ) {
                navController.popBackStack()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedItemsList.isNotEmpty(),
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                SecureFolderViewBottomAppBar(
                    pagingItems = items,
                    selectedItemsList = selectedItemsList,
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
            PhotoGrid(
                pagingItems = items,
                albumInfo = AlbumInfo.Empty,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.SecureFolder,
                hasFiles = hasFiles
            )
        }
    }
}

