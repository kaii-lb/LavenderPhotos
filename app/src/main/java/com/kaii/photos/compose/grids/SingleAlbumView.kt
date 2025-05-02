package com.kaii.photos.compose.grids

import android.util.Log
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.SingleAlbumViewBottomBar
import com.kaii.photos.compose.SingleAlbumViewTopBar
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.dialogs.SingleAlbumDialog
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SINGLE_ALBUM_VIEW"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    viewModel: MultiAlbumViewModel
) {
    val navController = LocalNavController.current
    BackHandler(
        enabled = selectedItemsList.size == 0
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    val contentResolver = LocalContext.current.contentResolver
    LaunchedEffect(mediaStoreData) {
        if (albumInfo.isCustomAlbum) {
            withContext(Dispatchers.IO) {
                val cursor = contentResolver.query(
                    LavenderContentProvider.CONTENT_URI,
                    arrayOf(
                        LavenderMediaColumns.ID,
                        LavenderMediaColumns.URI
                    ),
                    "TRUE",
                    null,
                    null
                )

                cursor?.use {
                    val data = mutableListOf<MediaStoreData>()

                    while (cursor.moveToNext()) {
                        val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
                        val uri = cursor.getString(uriCol).toUri()

                        val mediaItem = contentResolver.getMediaStoreDataFromUri(uri = uri)

                        Log.d(TAG, "Media: " + mediaItem.toString())
                        if (mediaItem != null) data.add(mediaItem)
                    }

                    groupedMedia.value = groupPhotosBy(data)
                }
            }
        } else {
            groupedMedia.value = mediaStoreData
        }
    }

    val showDialog = remember { mutableStateOf(false) }
    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.size > 0
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

    val context = LocalContext.current
    LaunchedEffect(albumInfo) {
        if (viewModel.albums.toSet() != albumInfo.paths.toSet()) {
            viewModel.reinitDataSource(
                context = context,
                albumsList = albumInfo.paths
            )
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
                // dir = viewModel.albums.first(), // TODO: make it handle multiple dirs
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                currentView = currentView
            ) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            SingleAlbumViewBottomBar(
                selectedItemsList = selectedItemsList
            )
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
                albums = viewModel.albums,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Album,
                shouldPadUp = true
            )

            SingleAlbumDialog(showDialog, albumInfo, navController, selectedItemsList)
        }
    }
}


