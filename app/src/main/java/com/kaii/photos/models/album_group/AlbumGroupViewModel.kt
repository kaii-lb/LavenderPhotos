package com.kaii.photos.models.album_group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.screens.AlbumGroupState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumGroupViewModel(
    id: String,
    albumGridState: AlbumGridState = PhotosApplication.appModule.albumGridState,
    private val settings: Settings = PhotosApplication.appModule.settings
) : ViewModel() {
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

    private val state = AlbumGroupState(
        id = id,
        albumGridState = albumGridState,
        groups = groups,
        sortMode = sortMode,
        scope = viewModelScope
    )

    val group: AlbumGroup?
        get() = state.group

    val albums = state.albums

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

    fun setGroupAlbums(
        id: String,
        albumIds: List<String>
    ) {
        viewModelScope.launch {
            settings.albums.editGroup(
                id = id,
                albumIds = albumIds
            )
        }
    }
}