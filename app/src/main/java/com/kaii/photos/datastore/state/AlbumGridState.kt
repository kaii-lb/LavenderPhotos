package com.kaii.photos.datastore.state

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.mediastore.signature
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumsGetAllState
import io.github.kaii_lb.lavender.immichintegration.state_managers.AllAlbumsState
import io.github.kaii_lb.lavender.immichintegration.state_managers.LocalApiClient
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.rememberLoginState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlbumGridState(
    private val scope: CoroutineScope,
    private val albumsFlow: Flow<List<AlbumInfo>>,
    private val apiClient: ApiClient,
    private val info: Flow<ImmichBasicInfo>,
    context: Context,
    sortModeFlow: Flow<MediaItemSortMode>,
    albumSortModeFlow: Flow<AlbumSortMode>,
    allAlbumsFlow: Flow<Boolean>,
    private val checkLoggedIn: suspend () -> Boolean,
    private val updateAlbums: (added: List<AlbumInfo>, removed: List<Int>) -> Unit
) {
    data class Album(
        val info: AlbumInfo,
        val thumbnail: String,
        val date: Long,
        val signature: ObjectKey
    )

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()
    private val customDao = MediaDatabase.getInstance(context).customDao()

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            albumsFlow.combine(sortModeFlow) { albums, sortMode ->
                Pair(albums, sortMode)
            }.combine(albumSortModeFlow) { pair, sortMode ->
                Triple(pair.first, pair.second, sortMode)
            }.distinctUntilChanged().collectLatest { (albums, mediaSortMode, albumSortMode) ->
                refresh(
                    albums = albums,
                    mediaSortMode = mediaSortMode,
                    albumSortMode = albumSortMode
                )
                refreshImmichAlbums()
            }
        }

        scope.launch(Dispatchers.IO) {
            allAlbumsFlow.distinctUntilChanged().collectLatest {
                if (it) {
                    val mediaDao = MediaDatabase.getInstance(context).mediaDao()

                    mediaDao
                        .getAllAlbums()
                        .distinctUntilChanged()
                        .collectLatest { list ->
                            updateAlbums(
                                list.fastMap { album ->
                                    AlbumInfo(
                                        id = album.hashCode(),
                                        name = album.filename(),
                                        paths = setOf(album)
                                    )
                                },
                                albumsFlow.first().fastMapNotNull { album ->
                                    album.id.takeIf {
                                        !album.isCustomAlbum && mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths) == null
                                    }
                                }
                            )
                        }
                }
            }
        }
    }

    private suspend fun refreshImmichAlbums() {
        if (!checkLoggedIn()) return

        val immichInfo = info.first()
        val albumState = AllAlbumsState(
            baseUrl = immichInfo.endpoint,
            apiClient = apiClient,
            coroutineScope = scope
        )

        albumState.load(immichInfo.accessToken).join()

        albumState.state.value.let { state ->
            if (state is AlbumsGetAllState.Retrieved) {
                val albumIds = state.albums.fastMap { it.id }
                val removedOrImmichIdChanged = _albums.value.fastMapNotNull { album ->
                    album.info.id.takeIf { album.info.immichId !in albumIds && album.info.isCustomAlbum && album.info.immichId.isNotBlank() }
                }

                updateAlbums(
                    state.albums.fastMap { album ->
                        AlbumInfo(
                            id = Random.nextInt(),
                            name = album.albumName,
                            paths = emptySet(),
                            isCustomAlbum = true,
                            immichId = album.id
                        )
                    },
                    removedOrImmichIdChanged
                )
            }
        }
    }

    private fun refresh(
        albums: List<AlbumInfo>,
        mediaSortMode: MediaItemSortMode,
        albumSortMode: AlbumSortMode
    ) = scope.launch(Dispatchers.IO) {
        _albums.value = albums.fastMap { album ->
            val cancellationSignal = CancellationSignal()

            val media = if (!album.isCustomAlbum) {
                if (mediaSortMode.isDateModified) {
                    mediaDao.getThumbnailForAlbumDateModified(paths = album.paths)
                } else {
                    mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths)
                } ?: MediaStoreData.dummyItem
            } else {
                if (mediaSortMode.isDateModified) {
                    customDao.getThumbnailForAlbumDateModified(album = album.id)
                } else {
                    customDao.getThumbnailForAlbumDateTaken(album = album.id)
                } ?: MediaStoreData.dummyItem
            }

            cancellationSignal.cancel()

            Album(
                info = album,
                thumbnail = media.immichThumbnail?.takeIf { album.immichId.isNotBlank() && it.isNotBlank() } ?: media.uri,
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
    val apiClient = LocalApiClient.current
    val coroutineScope = rememberCoroutineScope()

    val info by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
    val loginState = rememberLoginState(baseUrl = info.endpoint)


    return remember(loginState) {
        AlbumGridState(
            scope = coroutineScope,
            context = context,
            albumsFlow = mainViewModel.settings.albums.get(),
            sortModeFlow = mainViewModel.sortMode,
            albumSortModeFlow = mainViewModel.settings.albums.getAlbumSortMode(),
            allAlbumsFlow = mainViewModel.settings.albums.getAutoDetect(),
            info = mainViewModel.settings.immich.getImmichBasicInfo(),
            apiClient = apiClient,
            checkLoggedIn = {
                loginState.refresh(
                    accessToken = info.accessToken,
                    pfpSavePath = context.profilePicture,
                    previousPfpUrl = context.profilePicture
                ).join()

                loginState.state.value is LoginState.LoggedIn
            },
            updateAlbums = { added, removed ->
                mainViewModel.settings.albums.add(added)
                mainViewModel.settings.albums.removeAll(removed)
            }
        )
    }
}