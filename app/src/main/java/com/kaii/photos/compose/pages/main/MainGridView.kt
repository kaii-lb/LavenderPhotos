package com.kaii.photos.compose.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel

@Composable
fun MainGridView(
    viewModel: MultiAlbumViewModel,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    isMediaPicker: Boolean
) {
    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    PhotoGrid(
        pagingItems = items,
        albumInfo = albumInfo,
        selectedItemsList = selectedItemsList,
        viewProperties = ViewProperties.Main,
        // isMainPage = true, // TODO: figure out why this causes the grid to reset to top
        isMediaPicker = isMediaPicker
    )
}