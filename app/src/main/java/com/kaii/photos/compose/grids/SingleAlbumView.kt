package com.kaii.photos.compose.grids

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SingleAlbumViewBottomBar
import com.kaii.photos.compose.app_bars.SingleAlbumViewTopBar
import com.kaii.photos.compose.dialogs.SingleAlbumDialog
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.io.path.Path

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    viewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    LaunchedEffect(mediaStoreData) {
        groupedMedia.value = mediaStoreData
    }

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        currentView = currentView,
        navController = navController,
        incomingIntent = incomingIntent
    ) {
        if (viewModel.albumInfo.paths.toSet() != albumInfo.paths.toSet()) {
            viewModel.reinitDataSource(
                context = context,
                album = albumInfo
            )
        }
    }
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    customViewModel: CustomAlbumViewModel,
    multiViewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        customViewModel.cancelMediaFlow()
        multiViewModel.cancelMediaFlow()

        navController.popBackStack()
    }

    val customMediaStoreData by customViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val multiMediaStoreData by multiViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(customMediaStoreData + multiMediaStoreData) }

    LaunchedEffect(customMediaStoreData, multiMediaStoreData) {
        groupedMedia.value = (customMediaStoreData + multiMediaStoreData).distinctBy { it.uri }
    }

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        currentView = currentView,
        navController = navController,
        incomingIntent = incomingIntent
    ) {
        if (customViewModel.albumInfo != albumInfo) {
            customViewModel.reinitDataSource(
                context = context,
                album = albumInfo
            )
        }

        if (multiViewModel.albumInfo != albumInfo) {
            multiViewModel.reinitDataSource(
                context = context,
                album = albumInfo
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    groupedMedia: MutableState<List<MediaStoreData>>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    navController: NavHostController,
    currentView: MutableState<BottomBarTab>,
    incomingIntent: Intent?,
    reinitDataSource: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.Hidden,
    )

    LaunchedEffect(key1 = showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    LaunchedEffect(albumInfo) {
        reinitDataSource()
    }

    var hasFiles by remember { mutableStateOf(true) }
    LaunchedEffect(groupedMedia.value) {
        withContext(Dispatchers.IO) {
            hasFiles = if (!albumInfo.isCustomAlbum) {
                var result: Boolean? = null

                albumInfo.paths.any { path ->
                    val basePath = path.toBasePath()

                    result = Path(path).checkHasFiles(basePath = basePath)
                    result == true
                }

                result == true
            } else groupedMedia.value.isNotEmpty()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                media = groupedMedia.value,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                currentView = currentView,
                isMediaPicker = incomingIntent != null
            ) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            SingleAlbumViewBottomBar(
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                incomingIntent = incomingIntent
            )
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape,
        sheetContainerColor = if (incomingIntent != null) Color.Transparent else BottomSheetDefaults.ContainerColor
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
            val media = remember { mutableStateOf(emptyList<MediaStoreData>())}

            LaunchedEffect(groupedMedia.value.size) {
                if (groupedMedia.value.isNotEmpty()) {
                    media.value = groupedMedia.value.take(20)
                    delay(1000)
                    media.value = groupedMedia.value
                }
            }

            PhotoGrid(
                groupedMedia = media,
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Album,
                shouldPadUp = true,
                isMediaPicker = incomingIntent != null,
                hasFiles = hasFiles
            )

            SingleAlbumDialog(
                showDialog = showDialog,
                album = albumInfo,
                navController = navController,
                selectedItemsList = selectedItemsList,
                itemCount = groupedMedia.value.size
            )
        }
    }
}


