@file:OptIn(ExperimentalUuidApi::class)

package com.kaii.photos.datastore.state

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
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
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.signature
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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
    private val apiClient: ApiClient,
    private val context: Context,
    albumsFlow: Flow<List<AlbumType>>,
    albumGroupsFlow: Flow<List<AlbumGroup>>,
    albumsOrderFlow: Flow<List<String>>,
    info: Flow<ImmichBasicInfo>,
    sortModeFlow: Flow<MediaItemSortMode>,
    albumSortModeFlow: Flow<AlbumSortMode>,
    allAlbumsFlow: Flow<Boolean>,
    private val updateAlbums: (added: List<AlbumType>, updated: List<AlbumType.Cloud>, removed: List<String>) -> Unit
) {
    @Immutable
    sealed interface Album {
        val id: String
        val name: String
        val date: Long
        val pinned: Boolean

        @Immutable
        data class Single(
            val info: Info,
            override val id: String,
            override val name: String,
            val summary: String?,
            override val date: Long,
            override val pinned: Boolean
        ) : Album

        @Immutable
        data class Group(
            override val id: String,
            override val name: String,
            override val date: Long,
            override val pinned: Boolean,
            val info: ImmutableList<Info>
        ) : Album
    }

    @Immutable
    data class Info(
        val album: AlbumType,
        val thumbnail: Thumbnail
    ) {
        @Immutable
        data class Thumbnail(
            val uri: String,
            val signature: ObjectKey,
            val albumId: String,
            val date: Long,
            val isGif: Boolean
        )
    }

    private data class Params(
        val sortMode: MediaItemSortMode,
        val albumSortMode: AlbumSortMode,
        val albums: List<AlbumType>,
        val groups: List<AlbumGroup>,
        val order: List<String>
    )

    private var immichInfo = ImmichBasicInfo.Empty

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()
    private val customDao = MediaDatabase.getInstance(context).customDao()

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    private val _singleAlbums = MutableStateFlow(emptyList<Album.Single>())
    val singleAlbums = _singleAlbums.asStateFlow()

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
            launch {
                info.collect {
                    immichInfo = it
                }
            }

            launch {
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

            launch(Dispatchers.IO) {
                allAlbumsFlow.distinctUntilChanged().collectLatest {
                    if (it) {
                        val mediaDao = MediaDatabase.getInstance(context).mediaDao()

                        mediaDao
                            .getAllAlbums()
                            .distinctUntilChanged()
                            .collectLatest { list ->
                                updateAlbums(
                                    list.fastMapNotNull { album ->
                                        AlbumType.Folder(
                                            id = Uuid.random().toString(),
                                            name = album.filename(),
                                            paths = setOf(album),
                                            pinned = false,
                                            immichId = ""
                                        )
                                    },
                                    emptyList(),
                                    albumsFlow.first().fastMapNotNull { album ->
                                        album.id.takeIf {
                                            val empty = album is AlbumType.Folder && mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths) == null

                                            empty || album.name.isBlank()
                                        }
                                    }
                                )
                            }
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
        val albumsClient = AlbumsClient(
            client = apiClient,
            endpoint = immichInfo.endpoint,
            auth = immichInfo.auth
        )

        val allAlbums = albumsClient.getAll() ?: return@withContext

        val albumIds = allAlbums.fastMap { it.id }
        val removedOrImmichIdChanged = _albums.value
            .flatMap { album ->
                if (album is Album.Single) listOf(album.info)
                else (album as Album.Group).info
            }
            .fastMapNotNull { album ->
                album.album.id.takeIf {
                    album.album is AlbumType.Cloud && album.album.immichId !in albumIds
                }
            }

        val updated = _albums.value
            .filterIsInstance<Album.Single>()
            .fastMapNotNull { album ->
                val match = allAlbums.find { it.id == album.id }

                val changed = album.info.album.takeIf { it.name != match?.albumName } as? AlbumType.Cloud

                match?.albumName?.let { changed?.copy(name = it) }
            }

        updateAlbums(
            allAlbums.fastMapNotNull { album ->
                AlbumType.Cloud(
                    id = album.id,
                    name = album.albumName,
                    pinned = false
                )
            },
            updated,
            removedOrImmichIdChanged
        )
    }

    private suspend fun update() = withContext(Dispatchers.IO) {
        val result = mutableListOf<Album>()
        val albums = params.albums.toMutableList()
        val singleAlbums = mutableListOf<Album.Single>()

        params.groups.forEach { group ->
            val info = albums.filter { it.id in group.albumIds }.map { album ->
                albums.remove(album)

                val thumbnail = getThumbnail(album)

                val info = Info(
                    album = album,
                    thumbnail = thumbnail
                )

                singleAlbums.add(
                    Album.Single(
                        info = info,
                        id = album.id,
                        name = album.name,
                        summary = null,
                        date = thumbnail.date,
                        pinned = album.pinned
                    )
                )

                info
            }

            result.add(
                Album.Group(
                    id = group.id,
                    name = group.name,
                    date = info.minByOrNull { it.thumbnail.date }?.thumbnail?.date ?: 0L,
                    pinned = group.pinned,
                    info = info.sortedByDescending { it.thumbnail.date }.toImmutableList()
                )
            )
        }

        albums.forEach { album ->
            val thumbnail = getThumbnail(album)

            val info = Album.Single(
                id = album.id,
                name = album.name,
                summary = null,
                date = thumbnail.date,
                pinned = album.pinned,
                info = Info(
                    album = album,
                    thumbnail = thumbnail
                )
            )

            result.add(info)
            singleAlbums.add(info)
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

        // annotate items with the same name using their parent path
        val duplicateNames = singleAlbums
            .groupBy {
                it.name
            }.filter {
                it.value.size > 1
            }.values.flatten()

        singleAlbums.removeAll(duplicateNames)

        singleAlbums.addAll(
            duplicateNames.map {
                it.copy(
                    summary =
                        (it.info.album as? AlbumType.Folder)?.paths?.first()?.parent()
                )
            }
        )

        val sortedSingleAlbums = when (params.albumSortMode) {
            AlbumSortMode.LastModified -> {
                singleAlbums.sortedBy { it.date }
            }

            AlbumSortMode.LastModifiedDesc -> {
                singleAlbums.sortedByDescending { it.date }
            }

            AlbumSortMode.Alphabetically -> {
                singleAlbums.sortedBy { it.name }
            }

            AlbumSortMode.AlphabeticallyDesc -> {
                singleAlbums.sortedByDescending { it.name }
            }

            else -> {
                val lut = singleAlbums.associateBy { it.id }

                params.order.mapNotNull { lut[it] } + singleAlbums.filter { it.id !in lut.keys }
            }
        }

        _singleAlbums.value = sortedSingleAlbums.toMutableList().let { list ->
            if (params.albumSortMode != AlbumSortMode.Custom) {
                val pinned = list.filter { it.pinned }
                list.removeAll(pinned)
                list.addAll(0, pinned)
            }

            list
        }
    }

    private suspend fun getThumbnail(album: AlbumType): Info.Thumbnail {
        val media =
            if (album is AlbumType.Folder) {
                if (params.sortMode.isDateModified) {
                    mediaDao.getThumbnailForAlbumDateModified(paths = album.paths)
                } else {
                    mediaDao.getThumbnailForAlbumDateTaken(paths = album.paths)
                } ?: MediaStoreData.dummyItem
            } else {
                if (params.sortMode.isDateModified) {
                    customDao.getThumbnailForAlbumDateModified(album = album.id)
                } else {
                    customDao.getThumbnailForAlbumDateTaken(album = album.id)
                } ?: MediaStoreData.dummyItem
            }


        return Info.Thumbnail(
            uri = media.uri,
            date = if (params.sortMode.isDateModified) media.dateModified else media.dateTaken,
            signature = media.signature(),
            albumId = album.id,
            isGif = media.displayName.endsWith(".gif")
        )
    }
}

fun createAlbumGridState(
    context: Context,
    coroutineScope: CoroutineScope,
    apiClient: ApiClient
) = AlbumGridState(
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
    updateAlbums = { added, updated, removed ->
        val settings = context.appModule.settings.albums
        settings.add(added)
        settings.removeAll(removed)

        updated.forEach { album ->
            settings.edit(
                id = album.id,
                newInfo = album
            )
        }
    }
)