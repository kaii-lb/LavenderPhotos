package com.kaii.photos.compose.grids

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.TrashedPhotoGridViewBottomBar
import com.kaii.photos.compose.app_bars.TrashedPhotoGridViewTopBar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.OnBackPressedEffect
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.trash_bin.TrashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridView(
    viewModel: TrashViewModel,
    incomingIntent: Intent? = null
) {
    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberSelectionManager(pagingItems = pagingItems)

    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)
    BackHandler(
        enabled = isSelecting
    ) {
        selectionManager.clear()
    }

    OnBackPressedEffect { destination ->
        if (destination.hasRoute(Screens.MainPages::class)) viewModel.cancel()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            val navController = LocalNavController.current
            TrashedPhotoGridViewTopBar(
                selectionManager = selectionManager,
                deleteAll = {
                    viewModel.deleteAll()
                },
                onBackClick = {
                    viewModel.cancel()
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelecting,
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                TrashedPhotoGridViewBottomBar(
                    selectionManager = selectionManager,
                    incomingIntent = incomingIntent
                )
            }
        }
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
                pagingItems = pagingItems,
                albumInfo = AlbumInfo.Empty,
                viewProperties = ViewProperties.Trash,
                selectionManager = selectionManager,
                isMediaPicker = incomingIntent != null
            )
        }
    }
}
