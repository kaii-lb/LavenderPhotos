package com.kaii.photos.compose.grids

import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.TrashedPhotoGridViewBottomBar
import com.kaii.photos.compose.app_bars.TrashedPhotoGridViewTopBar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.TrashBin
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridView(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val trashViewModel: TrashViewModel = viewModel(
        factory = TrashViewModelFactory(
            context = context,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    )

    val mediaStoreData =
        trashViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
    var hasFiles by remember { mutableStateOf(true) }

    LaunchedEffect(mediaStoreData.value) {
        groupedMedia.value = mediaStoreData.value

        delay(PhotoGridConstants.LOADING_TIME)
        hasFiles = groupedMedia.value.isNotEmpty()
    }

    var triedDeletingAlready by rememberSaveable { mutableStateOf(false) }
    val autoDeleteInterval by mainViewModel.settings.TrashBin.getAutoDeleteInterval()
        .collectAsStateWithLifecycle(initialValue = 0)

    val runAutoDeleteAction = remember { mutableStateOf(false) }
    var mediaToBeAutoDeleted by remember { mutableStateOf(emptyList<Uri>()) }

    LaunchedEffect(runAutoDeleteAction.value) {
        permanentlyDeletePhotoList(context, mediaToBeAutoDeleted)

        triedDeletingAlready = true
        runAutoDeleteAction.value = false
    }

    LaunchedEffect(groupedMedia.value, autoDeleteInterval) {
        if (groupedMedia.value.isEmpty() || triedDeletingAlready || autoDeleteInterval == 0) return@LaunchedEffect

        val currentDate = System.currentTimeMillis()

        mediaToBeAutoDeleted = groupedMedia.value
            .filter { it.type != MediaType.Section }
            .filter { media ->
                val dateDeletedMillis =
                    currentDate - (media.dateModified * 1000) // dateModified is in seconds
                val dateDeletedDays = (dateDeletedMillis / (1000 * 60 * 60 * 24)).days

                dateDeletedDays > autoDeleteInterval.days
            }
            .map {
                it.uri
            }

        runAutoDeleteAction.value = true
    }

    BackHandler(
        enabled = selectedItemsList.isNotEmpty()
    ) {
        selectedItemsList.clear()
    }

    val navController = LocalNavController.current
    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        trashViewModel.cancelMediaSource()
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            TrashedPhotoGridViewTopBar(
                selectedItemsList = selectedItemsList,
                groupedMedia = groupedMedia.value
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
                TrashedPhotoGridViewBottomBar(selectedItemsList = selectedItemsList)
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
                groupedMedia = groupedMedia,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Trash,
                hasFiles = hasFiles
            )
        }
    }
}
