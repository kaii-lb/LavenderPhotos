package com.kaii.photos.compose.grids

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
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.FavouritesViewBottomAppBar
import com.kaii.photos.compose.app_bars.FavouritesViewTopAppBar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.grid_management.rememberFavSelectionManager
import com.kaii.photos.models.favourites_grid.FavouritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesGridView(
    viewModel: FavouritesViewModel,
    incomingIntent: Intent? = null
) {
    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberFavSelectionManager()

    val navController = LocalNavController.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize(1f)
            .windowInsetsPadding(
                WindowInsets.navigationBars
            ),
        topBar = {
            FavouritesViewTopAppBar(selectionManager = selectionManager) {
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
                FavouritesViewBottomAppBar(
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
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                pagingItems = pagingItems,
                albumInfo = AlbumInfo.Empty,
                selectionManager = selectionManager,
                viewProperties = ViewProperties.Favourites,
                isMediaPicker = incomingIntent != null
            )
        }
    }
}

