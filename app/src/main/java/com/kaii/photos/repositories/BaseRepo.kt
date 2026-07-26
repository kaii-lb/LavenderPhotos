package com.kaii.photos.repositories

import androidx.paging.PagingData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.traits.Copy
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Favourite
import com.kaii.photos.file_management.managers.traits.Move
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import kotlinx.coroutines.flow.Flow

open class RoomQueryParams(
    open val sortMode: MediaItemSortMode,
    open val format: DisplayDateFormat,
    open val info: ImmichBasicInfo
)

interface BaseRepo : Copy, Move, Trash, Delete, Share, Favourite, ExtractExif {
    val mediaFlow: Flow<PagingData<PhotoLibraryUIModel>>
    val gridMediaFlow: Flow<PagingData<PhotoLibraryUIModel>>
}