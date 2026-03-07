package com.kaii.photos.compose.pages.main

import androidx.compose.runtime.Composable
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.multi_album.MultiAlbumViewModel

@Composable
fun MainGridView(
    viewModel: MultiAlbumViewModel,
    album: AlbumType.Folder,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean,
    columnSize: Int,
    openVideosExternally: Boolean,
    cacheThumbnails: Boolean,
    thumbnailSize: Int,
    useRoundedCorners: Boolean,
) {
    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    PhotoGrid(
        pagingItems = items,
        album = album,
        selectionManager = selectionManager,
        viewProperties = ViewProperties.Main,
        isMainPage = true,
        isMediaPicker = isMediaPicker,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners
    )
}