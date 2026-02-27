package com.kaii.photos.compose.pages.main

import androidx.compose.runtime.Composable
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.multi_album.MultiAlbumViewModel

@Composable
fun MainGridView(
    viewModel: MultiAlbumViewModel,
    albumInfo: AlbumInfo,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean
) {
    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    PhotoGrid(
        pagingItems = items,
        albumInfo = albumInfo,
        selectionManager = selectionManager,
        viewProperties = ViewProperties.Main,
        isMainPage = true,
        isMediaPicker = isMediaPicker
    )
}