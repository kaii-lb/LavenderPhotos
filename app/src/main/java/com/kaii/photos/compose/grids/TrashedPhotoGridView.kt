package com.kaii.photos.compose.grids

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.TrashedPhotoGridViewBottomBar
import com.kaii.photos.compose.TrashedPhotoGridViewTopBar
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.datastore.TrashBin
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridView(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<MainScreenViewType>
) {
    val context = LocalContext.current
    val trashViewModel: TrashViewModel = viewModel(
        factory = TrashViewModelFactory(
            context
        )
    )

    val mediaStoreData = trashViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

    LaunchedEffect(mediaStoreData.value) {
        groupedMedia.value = mediaStoreData.value
        mainViewModel.setGroupedMedia(mediaStoreData.value)
    }

    var triedDeletingAlready by rememberSaveable { mutableStateOf(false) }
    val autoDeleteInterval by mainViewModel.settings.TrashBin.getAutoDeleteInterval().collectAsStateWithLifecycle(initialValue = 0)

    val runAutoDeleteAction = remember { mutableStateOf(false) }
    var mediaToBeAutoDeleted by remember { mutableStateOf(emptyList<Uri>()) }

    LaunchedEffect (runAutoDeleteAction.value) {
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
                val dateDeletedMillis = currentDate - (media.dateModified * 1000) // dateModified is in seconds
                val dateDeletedDays = (dateDeletedMillis / (1000 * 60 * 60 * 24)).days

                dateDeletedDays > autoDeleteInterval.days
            }
            .map {
                it.uri
            }

        runAutoDeleteAction.value = true
    }

    BackHandler(
        enabled = selectedItemsList.size > 0
    ) {
        selectedItemsList.clear()
    }

    val navController = LocalNavController.current
    BackHandler(
        enabled = selectedItemsList.size == 0
    ) {
        trashViewModel.cancelMediaSource()
        navController.popBackStack()
    }

    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.Hidden,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    LaunchedEffect(key1 = showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            TrashedPhotoGridViewTopBar(
                selectedItemsList = selectedItemsList,
                groupedMedia = groupedMedia.value,
                currentView = currentView
            ) {
                trashViewModel.cancelMediaSource()
                navController.popBackStack()
            }
        },
        sheetContent = {
            TrashedPhotoGridViewBottomBar(selectedItemsList = selectedItemsList)
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                path = null,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Trash,
                shouldPadUp = true
            )
        }
    }
}
