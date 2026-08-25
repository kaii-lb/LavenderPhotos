package com.kaii.photos.widgets.popup_chooser_state

import androidx.compose.foundation.lazy.LazyListState
import com.kaii.photos.datastore.state.AlbumGridState
import kotlinx.coroutines.flow.StateFlow

interface PopUpAlbumChooserState {
    val state: LazyListState
    val selectedAlbums: StateFlow<List<String>>
    val albumsList: StateFlow<List<AlbumGridState.Album.Single>>
    val query: StateFlow<String>

    fun search(query: String)
    fun addAll(albums: Collection<String>)
    fun clear()
    fun toggle(album: String)
    fun isSelected(album: String): Boolean
}
