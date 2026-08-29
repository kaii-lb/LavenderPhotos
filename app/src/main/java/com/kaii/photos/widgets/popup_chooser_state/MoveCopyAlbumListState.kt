package com.kaii.photos.widgets.popup_chooser_state

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
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

class MoveCopyAlbumListState(
    override val state: LazyListState,
    val immichInfo: Flow<ImmichBasicInfo>,
    albumsFlow: Flow<List<AlbumGridState.Album.Single>>,
    coroutineScope: CoroutineScope
) : PopUpAlbumChooserState {
    enum class Filters(
        @param:StringRes val label: Int
    ) {
        None(label = R.string.album_group_filter_none),
        Folder(label = R.string.albums_folder),
        Album(label = R.string.albums_custom),
        CloudAlbum(label = R.string.albums_cloud)
    }

    data class Params(
        val albums: List<AlbumGridState.Album.Single>,
        val query: String,
        val filters: Filters,
        val isMoving: Boolean
    )

    private var currentAlbum: AlbumType = AlbumType.PlaceHolder
    var isMoving by mutableStateOf(false)

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
            val movingFlow = snapshotFlow { isMoving }

            combine(albumsFlow, _query, _filter, movingFlow) { albums, query, filter, isMoving ->
                Params(albums, query, filter, isMoving)
            }.collectLatest { (albums, query, filter, isMoving) ->
                _albumsList.value = albums.filter { single ->
                    val album = single.info.album
                    val nameMatch = album.name.contains(query, true)
                    val moveMatch = if (isMoving) album.id != currentAlbum.id else true

                    val filterMatch = when (filter) {
                        Filters.None -> true
                        Filters.Folder -> album is AlbumType.Folder
                        Filters.Album -> album is AlbumType.Custom
                        Filters.CloudAlbum -> album is AlbumType.Cloud
                    }

                    // SAFFolders are not supported for moving/copying operations
                    nameMatch && filterMatch && moveMatch && album !is AlbumType.SAFFolder
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
        _query.value = ""
        _selectedAlbums.clear()
        state.requestScrollToItem(0)
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

    fun setCurrentAlbum(album: AlbumType) {
        currentAlbum = album
    }
}

@Composable
fun rememberMoveCopyAlbumListState(): MoveCopyAlbumListState {
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    return remember {
        MoveCopyAlbumListState(
            state = state,
            immichInfo = PhotosApplication.appModule.settings.immich.getImmichBasicInfo(),
            albumsFlow = PhotosApplication.appModule.albumGridState.singleAlbums,
            coroutineScope = coroutineScope
        )
    }
}