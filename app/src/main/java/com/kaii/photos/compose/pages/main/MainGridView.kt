package com.kaii.photos.compose.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.media.PhotoGrid
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel

@Composable
fun MainGridView(
    items:  LazyPagingItems<PhotoLibraryUIModel>,
    album: () -> AlbumType.Folder,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean,
    columnSize: () -> Int,
    openVideosExternally: () -> Boolean,
    cacheThumbnails: () -> Boolean,
    thumbnailSize: () -> Int,
    useRoundedCorners: () -> Boolean,
    vibrateOnClick: () -> Boolean,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties = ViewProperties.Main
) {
    PhotoGrid(
        pagingItems = items,
        album = album,
        selectionManager = selectionManager,
        viewProperties = viewProperties,
        isMainPage = true,
        isMediaPicker = isMediaPicker,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        vibrateOnClick = vibrateOnClick,
        modifier = modifier
    )
}