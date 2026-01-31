package com.kaii.photos.compose.immich

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.lavender.immichintegration.state_managers.LocalApiClient
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModelFactory
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ImmichAlbumPage(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val apiClient = LocalApiClient.current

    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

    val immichInfo by mainViewModel.settings.Immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

    if (immichInfo == ImmichBasicInfo.Empty) return

    val immichViewModel: ImmichAlbumViewModel = viewModel(
        factory = ImmichAlbumViewModelFactory(
            immichId = albumInfo.immichId,
            info = immichInfo,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat,
            apiClient = apiClient
        )
    )

    val mediaStoreData = immichViewModel.mediaFlow.collectAsStateWithLifecycle()
    val hasFiles by immichViewModel.hasFiles.collectAsStateWithLifecycle()

    LaunchedEffect(immichInfo) {
        immichViewModel.update(
            context = context,
            immichId = albumInfo.immichId,
            info = immichInfo
        )

        immichViewModel.refresh(
            context = context,
            refetch = true
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            // TODO
            // val navController = LocalNavController.current
            // SingleAlbumViewTopBar(
            //     albumInfo = albumInfo,
            //     pagingItems = mediaStoreData,
            //     selectedItemsList = selectedItemsList,
            //     showDialog = remember { mutableStateOf(false) },
            //     isMediaPicker = false, // TODO:
            //     onBackClick = {
            //         navController.popBackStack()
            //     }
            // )
        },
        bottomBar = {
            // TODO:
            // AnimatedVisibility(
            //     visible = selectedItemsList.isNotEmpty(),
            //     enter = fadeIn() + slideInHorizontally(
            //         animationSpec = AnimationConstants.expressiveSpring()
            //     ),
            //     exit = fadeOut() + slideOutHorizontally(
            //         animationSpec = AnimationConstants.expressiveTween()
            //     )
            // ) {
            //
            // }
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
            // TODO
            // PhotoGrid(
            //     pagingItems = mediaStoreData,
            //     albumInfo = albumInfo,
            //     selectedItemsList = selectedItemsList,
            //     viewProperties = ViewProperties.Immich,
            //     isMediaPicker = false, // TODO:
            //     hasFiles = hasFiles
            // )
        }
    }
}