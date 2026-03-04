@file:OptIn(ExperimentalUuidApi::class)

package com.kaii.photos.datastore.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AlbumGridState(
    private val scope: CoroutineScope,
    private val albumsFlow: Flow<List<AlbumType>>,
    private val apiClient: ApiClient,
    private val info: Flow<ImmichBasicInfo>,
    context: Context,
    sortModeFlow: Flow<MediaItemSortMode>,
    albumSortModeFlow: Flow<AlbumSortMode>,
    allAlbumsFlow: Flow<Boolean>,
    private val checkLoggedIn: suspend () -> Boolean,
    private val updateAlbums: (added: List<AlbumType>, removed: List<String>) -> Unit
) {
    data class Album(
        val info: AlbumType,
        val thumbnail: Thumbnail,
        val date: Long
    ) {
        data class Thumbnail(
            val uri: String,
            val signature: ObjectKey,
            val id: String,
            val date: Long
        )
    }

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()
    private val customDao = MediaDatabase.getInstance(context).customDao()

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    private var internalAlbums = emptyList<AlbumType>()
    private var internalSortMode = MediaItemSortMode.DateTaken
    private var internalAlbumSortMode = AlbumSortMode.LastModifiedDesc

    init {
        scope.launch(Dispatchers.IO) {
            albumsFlow.combine(sortModeFlow) { albums, sortMode ->
                Pair(albums, sortMode)
            }.combine(albumSortModeFlow) { pair, sortMode ->
                Triple(pair.first, pair.second, sortMode)
            }.distinctUntilChanged().collectLatest { (albums, mediaSortMode, albumSortMode) ->
                internalAlbums = albums
                internalSortMode = mediaSortMode
                internalAlbumSortMode = albumSortMode

                refresh()
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
                                    AlbumType.Folder(
                                        id = Uuid.random().toString(),
                                        name = album.filename(),
                                        paths = setOf(album),
                                        pinned = false,
                                        groupId = null,
                                        immichId = ""
                                    )
                                },
                                albumsFlow.first().fastMapNotNull { album ->
                                    album.id.takeIf {
                                        album is AlbumType.Folder
                                                && mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths) == null
                                    }
                                }
                            )
                        }
                }
            }
        }
    }

    fun refresh() {
        scope.launch {
            update()
            updateImmich()
        }
    }

    private suspend fun updateImmich() {
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
                    (album.info as? AlbumType.Cloud)?.id.takeIf {
                        album.info as AlbumType.Cloud

                        album.info.immichId !in albumIds
                    }
                }

                updateAlbums(
                    state.albums.fastMapNotNull { album ->
                        AlbumType.Cloud(
                            id = album.id,
                            name = album.albumName,
                            pinned = false,
                            groupId = null
                        )
                    },
                    removedOrImmichIdChanged
                )
            }
        }
    }

    private fun update() = scope.launch(Dispatchers.IO) {
        _albums.value = internalAlbums.fastMap { album ->
            val thumbnail =
                if (album is AlbumType.Folder) {
                    val media = if (internalSortMode.isDateModified) {
                        mediaDao.getThumbnailForAlbumDateModified(paths = album.paths)
                    } else {
                        mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths)
                    } ?: MediaStoreData.dummyItem

                    Pair(media, album.id)
                } else {
                    val media = if (internalSortMode.isDateModified) {
                        customDao.getThumbnailForAlbumDateModified(album = album.id)
                    } else {
                        customDao.getThumbnailForAlbumDateTaken(album = album.id)
                    } ?: MediaStoreData.dummyItem

                    Pair(media, album.id)
                }

            Album(
                info = album,
                thumbnail =
                    Album.Thumbnail(
                        uri = thumbnail.first.uri,
                        signature = thumbnail.first.signature(),
                        id = thumbnail.second,
                        date = if (internalSortMode.isDateModified) thumbnail.first.dateModified else thumbnail.first.dateTaken
                    ),
                date = if (internalSortMode.isDateModified) thumbnail.first.dateModified else thumbnail.first.dateTaken
            )
        }.let { list ->
            val sorted = when (internalAlbumSortMode) {
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
                val pinned = list.filter { it.info.pinned }
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
    val apiClient = LocalApiClient.current
    val coroutineScope = rememberCoroutineScope()

    val info by context.appModule.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
    val loginState = rememberLoginState(baseUrl = info.endpoint)

    return remember(loginState, info) {
        AlbumGridState(
            scope = coroutineScope,
            context = context,
            albumsFlow = context.appModule.settings.albums.get(),
            sortModeFlow = context.appModule.settings.photoGrid.getSortMode(),
            albumSortModeFlow = context.appModule.settings.albums.getSortMode(),
            allAlbumsFlow = context.appModule.settings.albums.getAutoDetect(),
            info = context.appModule.settings.immich.getImmichBasicInfo(),
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
                context.appModule.settings.albums.add(added)
                context.appModule.settings.albums.removeAll(removed)
            }
        )
    }
}