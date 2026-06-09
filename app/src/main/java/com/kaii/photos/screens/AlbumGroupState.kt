package com.kaii.photos.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.state.AlbumGridState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumGroupState(
    id: String,
    albumGridState: AlbumGridState,
    groups: Flow<List<AlbumGroup>>,
    scope: CoroutineScope,
    sortMode: StateFlow<AlbumSortMode>
) {
    private var singleAlbums = emptyList<AlbumGridState.Album.Single>()
    private var sortMode = AlbumSortMode.LastModifiedDesc

    var group by mutableStateOf<AlbumGroup?>(null)
        private set

    private val _albums = MutableStateFlow(emptyList<AlbumGridState.Album.Single>())
    val albums = _albums.asStateFlow()

    init {
        scope.launch {
            launch {
                groups.collect { groups ->
                    group = groups.find {
                        it.id == id
                    }
                    refresh()
                }
            }

            launch {
                albumGridState.singleAlbums.collect {
                    singleAlbums = it
                    refresh()
                }
            }

            launch {
                sortMode.collect {
                    this@AlbumGroupState.sortMode = it
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        if (group == null) {
            _albums.value = emptyList()
            return
        }

        _albums.value = singleAlbums.filter {
            it.id in group!!.albumIds
        }.let { inGroup ->
            when (sortMode) {
                AlbumSortMode.LastModified -> inGroup.sortedBy { it.date }
                AlbumSortMode.LastModifiedDesc -> inGroup.sortedByDescending { it.date }
                AlbumSortMode.Alphabetically -> inGroup.sortedBy { it.name }
                AlbumSortMode.AlphabeticallyDesc -> inGroup.sortedByDescending { it.name }
                AlbumSortMode.Custom -> inGroup
            }.toMutableList().apply {
                val pinned = filter { it.pinned }
                removeAll(pinned)
                addAll(0, pinned)
            }
        }
    }
}