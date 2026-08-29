package com.kaii.photos.widgets.popup_chooser_state

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.datastore.state.AlbumGridState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumGroupPopupChooserState(
    override val state: LazyListState,
    albumsFlow: Flow<List<AlbumGridState.Album.Single>>,
    coroutineScope: CoroutineScope,
    filter: (album: AlbumGridState.Album.Single, query: String, filter: Filters) -> Boolean
) : PopUpAlbumChooserState {
    enum class Filters(
        @param:StringRes val label: Int
    ) {
        None(label = R.string.album_group_filter_none),
        InThisGroup(label = R.string.album_group_filter_in_this_groups),
        NotInAGroup(label = R.string.album_group_filter_not_in_a_group),
        InOtherGroups(label = R.string.album_group_filter_in_other_groups)
    }

    private val _filter = MutableStateFlow(Filters.None)
    val filter = _filter.asStateFlow()

    private val _selectedAlbums = mutableStateListOf<String>()
    override val selectedAlbums = snapshotFlow { _selectedAlbums.toList() }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    private val _albumsList = MutableStateFlow(emptyList<AlbumGridState.Album.Single>())
    override val albumsList = _albumsList.asStateFlow()

    private val _query = MutableStateFlow("")
    override val query = _query.asStateFlow()

    init {
        coroutineScope.launch {
            combine(albumsFlow, _query, _filter) { albums, query, filter ->
                Triple(albums, query, filter)
            }.collectLatest { (albums, query, filter) ->
                _albumsList.value = albums.filter { single ->
                    filter(single, query, filter)
                }

                if (_albumsList.value.isNotEmpty()) state.scrollToItem(0)
            }
        }
    }

    override fun search(query: String) {
        _query.value = query
    }

    override fun addAll(albums: Collection<String>) {
        _selectedAlbums.addAll(albums)
    }

    override fun clear() {
        _selectedAlbums.clear()
    }

    override fun toggle(album: String) {
        if (_selectedAlbums.contains(album)) {
            _selectedAlbums.remove(album)
        } else {
            _selectedAlbums.add(album)
        }
    }

    override fun isSelected(album: String) = _selectedAlbums.contains(album)

    fun applyFilter(filter: Filters) {
        _filter.value = filter
    }
}

@Composable
fun rememberAlbumGroupPopUpAlbumChooserState(
    filter: (album: AlbumGridState.Album.Single, query: String, filter: AlbumGroupPopupChooserState.Filters) -> Boolean
): AlbumGroupPopupChooserState {
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AlbumGroupPopupChooserState(
            state = state,
            albumsFlow = PhotosApplication.appModule.albumGridState.singleAlbums,
            coroutineScope = coroutineScope,
            filter = filter
        )
    }
}