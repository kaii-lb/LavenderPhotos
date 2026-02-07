package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.repositories.MediaRepository

// private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"



class MultiAlbumViewModel(
    context: Context,
    albumInfo: AlbumInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = MediaRepository(
        context = context,
        albumInfo = albumInfo,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun update(
        album: AlbumInfo? = null,
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null
    ) = repo.update(album, sortMode, format)
}

