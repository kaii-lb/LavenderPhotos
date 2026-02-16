package com.kaii.photos.datastore.state

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AlbumGridState(
    context: Context,
    albumsFlow: Flow<List<AlbumInfo>>,
    sortModeFlow: Flow<MediaItemSortMode>,
    albumSortModeFlow: Flow<AlbumSortMode>,
    private val scope: CoroutineScope
) {
    data class Album(
        val info: AlbumInfo,
        val thumbnail: String,
        val date: Long,
        val signature: ObjectKey
    )

    private val dao = MediaDatabase.getInstance(context).mediaDao()

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            albumsFlow.combine(sortModeFlow) { albums, sortMode ->
                Pair(albums, sortMode)
            }.combine(albumSortModeFlow) { pair, sortMode ->
                Triple(pair.first,  pair.second, sortMode)
            }.distinctUntilChanged().collectLatest { (albums, mediaSortMode, albumSortMode) ->
                refresh(
                    context = context,
                    albums = albums,
                    mediaSortMode = mediaSortMode,
                    albumSortMode = albumSortMode
                )
            }
        }
    }

    private fun refresh(
        context: Context,
        albums: List<AlbumInfo>,
        mediaSortMode: MediaItemSortMode,
        albumSortMode: AlbumSortMode
    ) = scope.launch(Dispatchers.IO) {
        _albums.value = albums.fastMap { album ->
            val cancellationSignal = CancellationSignal()

            val media = if (!album.isCustomAlbum) {
                if (mediaSortMode.isDateModified) {
                    dao.getThumbnailForAlbumDateModified(paths = album.paths)
                } else {
                    dao.getThumbnailForAlbumDateTaken(paths = album.paths)
                } ?: MediaStoreData.dummyItem
            } else {
                val datasource = CustomAlbumDataSource(
                    context = context,
                    parentId = album.id,
                    sortMode = mediaSortMode,
                    cancellationSignal = cancellationSignal
                )

                datasource.query().getOrElse(0) { MediaStoreData.dummyItem }
            }

            cancellationSignal.cancel()

            Album(
                info = album,
                thumbnail = media.uri,
                date = if (mediaSortMode.isDateModified) media.dateModified else media.dateTaken,
                signature = media.signature()
            )
        }.let { list ->
            val sorted = when (albumSortMode) {
                AlbumSortMode.LastModified -> {
                    list.sortedBy { it.date }
                }

                AlbumSortMode.LastModifiedDesc -> {
                    list.sortedByDescending { it.date }
                }

                AlbumSortMode.Alphabetically -> {
                    list.sortedBy { it.info.name }
                }

                AlbumSortMode.AlphabeticallyDesc -> {
                    list.sortedByDescending { it.info.name }
                }

                else -> {
                    list
                }
            }

            sorted.toMutableList().let { list ->
                val pinned = list.filter { it.info.isPinned }
                list.removeAll(pinned)
                list.addAll(0, pinned)
                list
            }
        }
    }
}

@Composable
fun rememberAlbumGridState(): AlbumGridState {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AlbumGridState(
            context = context,
            albumsFlow = mainViewModel.settings.albums.get(),
            sortModeFlow = mainViewModel.sortMode,
            albumSortModeFlow = mainViewModel.settings.albums.getAlbumSortMode(),
            scope = coroutineScope
        )
    }
}