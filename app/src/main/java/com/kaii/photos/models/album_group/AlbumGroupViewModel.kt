package com.kaii.photos.models.album_group

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun editGroup(
        id: String,
        name: String,
        pinned: Boolean
    ) {
        viewModelScope.launch {
            settings.albums.editGroup(id, name, pinned)
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            settings.albums.removeGroup(id)
        }
    }

    fun deleteAlbum(
        album: AlbumGridState.Album.Single,
        group: AlbumGroup
    ) {
        viewModelScope.launch {
            settings.albums.editGroup(
                id = group.id,
                albumIds = group.albumIds
                    .filter {
                        it != album.id
                    }
            )
        }
    }

    fun toggleAlbumPin(
        album: AlbumGridState.Album.Single
    ) {
        viewModelScope.launch {
            settings.albums.edit(
                id = album.id,
                newInfo = when (album.info.album) {
                    is AlbumType.Folder -> album.info.album.copy(pinned = !album.pinned)
                    is AlbumType.Custom -> album.info.album.copy(pinned = !album.pinned)
                    is AlbumType.Cloud -> album.info.album.copy(pinned = !album.pinned)
                    else -> AlbumType.PlaceHolder
                }
            )
        }
    }

    fun setAlbumSortMode(sortMode: AlbumSortMode) = settings.albums.setSortMode(sortMode)
    fun setAlbumOrder(list: List<String>) = settings.albums.setOrder(list)
}