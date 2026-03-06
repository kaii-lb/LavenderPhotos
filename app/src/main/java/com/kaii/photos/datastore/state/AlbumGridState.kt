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
import com.kaii.photos.datastore.AlbumGroup
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AlbumGridState(
    private val scope: CoroutineScope,
    private val albumsFlow: Flow<List<AlbumType>>,
    private val albumGroupsFlow: Flow<List<AlbumGroup>>,
    private val albumsOrderFlow: Flow<List<String>>,
    private val apiClient: ApiClient,
    private val info: Flow<ImmichBasicInfo>,
    context: Context,
    sortModeFlow: Flow<MediaItemSortMode>,
    albumSortModeFlow: Flow<AlbumSortMode>,
    allAlbumsFlow: Flow<Boolean>,
    private val checkLoggedIn: suspend () -> Boolean,
    private val updateAlbums: (added: List<AlbumType>, removed: List<String>) -> Unit
) {
    sealed interface Album {
        val id: String
        val name: String
        val date: Long
        val pinned: Boolean

        data class Single(
            val info: Info,
            override val id: String,
            override val name: String,
            override val date: Long,
            override val pinned: Boolean
        ) : Album

        data class Group(
            override val id: String,
            override val name: String,
            override val date: Long,
            override val pinned: Boolean,
            val info: List<Info>
        ) : Album
    }

    data class Info(
        val album: AlbumType,
        val thumbnail: Thumbnail
    ) {
        data class Thumbnail(
            val uri: String,
            val signature: ObjectKey,
            val albumId: String,
            val date: Long
        )
    }

    private data class Params(
        val sortMode: MediaItemSortMode,
        val albumSortMode: AlbumSortMode,
        val albums: List<AlbumType>,
        val groups: List<AlbumGroup>,
        val order: List<String>
    )

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()
    private val customDao = MediaDatabase.getInstance(context).customDao()

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    private var job: Job? = null
    private var params = Params(
        sortMode = MediaItemSortMode.DateTaken,
        albumSortMode = AlbumSortMode.LastModifiedDesc,
        albums = emptyList(),
        groups = emptyList(),
        order = emptyList()
    )

    init {
        scope.launch(Dispatchers.IO) {
            combine(
                flow = sortModeFlow,
                flow2 = albumSortModeFlow,
                flow3 = albumsFlow,
                flow4 = albumGroupsFlow,
                flow5 = albumsOrderFlow
            ) { sortMode, albumSortMode, albums, groups, order ->
                Params(sortMode, albumSortMode, albums, groups, order)
            }.collectLatest {
                params = it
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
        job?.cancel()
        job = scope.launch {
            update()
            updateImmich()
        }
    }

    private suspend fun updateImmich() = withContext(Dispatchers.IO) {
        if (!checkLoggedIn()) return@withContext

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
                val removedOrImmichIdChanged = _albums.value
                    .flatMap { album ->
                        if (album is Album.Single) listOf(album.info)
                        else (album as Album.Group).info
                    }
                    .fastMapNotNull { album ->
                        (album.album as? AlbumType.Cloud)?.id.takeIf {
                            album.album as AlbumType.Cloud

                            album.album.immichId !in albumIds
                        }
                    }

                updateAlbums(
                    state.albums.fastMapNotNull { album ->
                        AlbumType.Cloud(
                            id = album.id,
                            name = album.albumName,
                            pinned = false
                        )
                    },
                    removedOrImmichIdChanged
                )
            }
        }
    }

    private suspend fun update() = withContext(Dispatchers.IO) {
        val result = mutableListOf<Album>()
        val albums = params.albums.toMutableList()

        params.groups.forEach { group ->
            val info = albums.filter { it.id in group.albumIds }.map { album ->
                albums.remove(album)

                val thumbnail =
                    if (album is AlbumType.Folder) {
                        val media = if (params.sortMode.isDateModified) {
                            mediaDao.getThumbnailForAlbumDateModified(paths = album.paths)
                        } else {
                            mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths)
                        } ?: MediaStoreData.dummyItem

                        Info.Thumbnail(
                            uri = media.uri,
                            date = if (params.sortMode.isDateModified) media.dateModified else media.dateTaken,
                            signature = media.signature(),
                            albumId = album.id
                        )
                    } else {
                        val media = if (params.sortMode.isDateModified) {
                            customDao.getThumbnailForAlbumDateModified(album = album.id)
                        } else {
                            customDao.getThumbnailForAlbumDateTaken(album = album.id)
                        } ?: MediaStoreData.dummyItem

                        Info.Thumbnail(
                            uri = media.uri,
                            date = if (params.sortMode.isDateModified) media.dateModified else media.dateTaken,
                            signature = media.signature(),
                            albumId = album.id
                        )
                    }

                Info(
                    album = album,
                    thumbnail = thumbnail
                )
            }

            result.add(
                Album.Group(
                    id = group.id,
                    name = group.name,
                    date = info.minByOrNull { it.thumbnail.date }?.thumbnail?.date ?: 0L,
                    pinned = group.pinned,
                    info = info.sortedByDescending { it.thumbnail.date }
                )
            )
        }

        albums.forEach { album ->
            val thumbnail =
                if (album is AlbumType.Folder) {
                    val media = if (params.sortMode.isDateModified) {
                        mediaDao.getThumbnailForAlbumDateModified(paths = album.paths)
                    } else {
                        mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths)
                    } ?: MediaStoreData.dummyItem

                    Info.Thumbnail(
                        uri = media.uri,
                        date = if (params.sortMode.isDateModified) media.dateModified else media.dateTaken,
                        signature = media.signature(),
                        albumId = album.id
                    )
                } else {
                    val media = if (params.sortMode.isDateModified) {
                        customDao.getThumbnailForAlbumDateModified(album = album.id)
                    } else {
                        customDao.getThumbnailForAlbumDateTaken(album = album.id)
                    } ?: MediaStoreData.dummyItem

                    Info.Thumbnail(
                        uri = media.uri,
                        date = if (params.sortMode.isDateModified) media.dateModified else media.dateTaken,
                        signature = media.signature(),
                        albumId = album.id
                    )
                }

            result.add(
                Album.Single(
                    id = album.id,
                    name = album.name,
                    date = thumbnail.date,
                    pinned = album.pinned,
                    info = Info(
                        album = album,
                        thumbnail = thumbnail
                    )
                )
            )
        }

        val sorted = when (params.albumSortMode) {
            AlbumSortMode.LastModified -> {
                result.sortedBy { it.date }
            }

            AlbumSortMode.LastModifiedDesc -> {
                result.sortedByDescending { it.date }
            }

            AlbumSortMode.Alphabetically -> {
                result.sortedBy { it.name }
            }

            AlbumSortMode.AlphabeticallyDesc -> {
                result.sortedByDescending { it.name }
            }

            else -> {
                val lut = result.associateBy { it.id }

                params.order.mapNotNull { lut[it] } + result.filter { it.id !in lut.keys }
            }
        }

        _albums.value = sorted.toMutableList().let { list ->
            if (params.albumSortMode != AlbumSortMode.Custom) {
                val pinned = list.filter { it.pinned }
                list.removeAll(pinned)
                list.addAll(0, pinned)
            }

            list
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
            albumGroupsFlow = context.appModule.settings.albums.getGroups(),
            albumsOrderFlow = context.appModule.settings.albums.getOrder(),
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