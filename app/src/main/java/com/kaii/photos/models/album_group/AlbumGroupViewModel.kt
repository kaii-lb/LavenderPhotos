package com.kaii.photos.models.album_group

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AlbumGroupViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val albumColumnSize = settings.lookAndFeel.getAlbumColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ImmichBasicInfo.Empty
    )

    val sortMode = settings.albums.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumSortMode.LastModifiedDesc
    )

    val groups = settings.albums.getGroups().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
}