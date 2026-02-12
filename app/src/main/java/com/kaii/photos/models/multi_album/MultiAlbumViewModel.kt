package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.MediaRepository

class MultiAlbumViewModel(
    context: Context,
    albumInfo: AlbumInfo,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = MediaRepository(
        context = context,
        initialAlbumInfo = albumInfo,
        info = info,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun update(
        album: AlbumInfo? = null,
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null
    ) = repo.update(album, sortMode, format, accessToken)
}

