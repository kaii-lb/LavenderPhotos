@file:OptIn(ExperimentalUuidApi::class)

package com.kaii.photos.datastore.state

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.PhotosApplication
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.parent
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
    private val apiClient: ApiClient,
    private val context: Context,
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    scope: CoroutineScope,
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

    private val _albums = MutableStateFlow(emptyList<Album>())
    val albums = _albums.asStateFlow()

    private val _singleAlbums = MutableStateFlow(emptyList<Album.Single>())
    val singleAlbums = _singleAlbums.asStateFlow()

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
                            .collect { list ->
                                updateAlbums(
                                    list.fastMapNotNull { album ->
                                        AlbumType.Folder(
                                            id = Uuid.random().toString(),
                                            name = album.filename(),
                                            paths = setOf(album),
                                            pinned = false,
                                            immichId = null
                                        )
                                    },
                                    emptyList(),
                                    albumsFlow.first().fastMapNotNull { album ->
                                        album.id.takeIf {
                                            val empty = album is AlbumType.Folder
                                                    && album.immichId == null
                                                    && mediaDao.countInFolder(paths = album.paths.toList()) == 0

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

    fun getImmichInfo() = immichInfo

    suspend fun refresh() {
        coroutineScope {
            launch { update() }
            launch { updateImmich() }
        }
    }

    private suspend fun updateImmich() = withContext(Dispatchers.IO) {
        val albumsClient = AlbumsClient(
            client = apiClient,
            endpoint = immichInfo.endpoint,
            auth = immichInfo.auth
        )

        val allAlbums = albumsClient.getAll() ?: return@withContext

        val albumIds = allAlbums.fastMap { it.id }.toSet()
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
        val remainingAlbums = params.albums.associateByTo(mutableMapOf()) { it.id }
        val result = mutableListOf<Album>()
        val singleAlbums = mutableListOf<Album.Single>()

        val (folderAlbums, customAlbums) = params.albums.partition { it is AlbumType.Folder }
        val customThumbnails = customDao.getThumbnails(
            albumIds = customAlbums.map { it.id },
            sortMode = params.sortMode,
            albumSortMode = params.albumSortMode
        )

        @Suppress("UNCHECKED_CAST")
        val folderThumbnails = mediaDao.getFolderThumbnails(
            folders = folderAlbums as List<AlbumType.Folder>,
            sortMode = params.sortMode,
            albumSortMode = params.albumSortMode
        )

        // 1. Process Groups
        params.groups.forEach { group ->
            val infoList = group.albumIds.mapNotNull { id ->
                remainingAlbums.remove(id)?.let { album ->
                    val thumbnail =
                        if (album is AlbumType.Folder) {
                            folderThumbnails[album.id]!!
                        } else {
                            customThumbnails[album.id]!!
                        }

                    val info = Info(album, thumbnail)
                    singleAlbums.add(Album.Single(info, album.id, album.name, null, thumbnail.date, album.pinned))
                    info
                }
            }

            result.add(
                Album.Group(
                    id = group.id,
                    name = group.name,
                    date = infoList.minOfOrNull { it.thumbnail.date } ?: 0L,
                    pinned = group.pinned,
                    info = infoList.sortedByDescending { it.thumbnail.date }.toImmutableList()
                )
            )
        }

        remainingAlbums.values.forEach { album ->
            val thumbnail =
                if (album is AlbumType.Folder) {
                    folderThumbnails[album.id]!!
                } else {
                    customThumbnails[album.id]!!
                }

            val single = Album.Single(
                info = Info(
                    album = album,
                    thumbnail = thumbnail
                ),
                id = album.id,
                name = album.name,
                summary = null,
                date = thumbnail.date,
                pinned = album.pinned
            )

            result.add(single)
            singleAlbums.add(single)
        }

        val nameCounts = singleAlbums.groupingBy { it.name }.eachCount()
        val deduplicatedSingleAlbums = singleAlbums.map { album ->
            if ((nameCounts[album.name] ?: 0) > 1) {
                val parentPath = (album.info.album as? AlbumType.Folder)?.paths?.firstOrNull()?.parent()
                album.copy(summary = parentPath)
            } else {
                album
            }
        }

        _albums.value = sortAndPin(result, params.albumSortMode, params.order)
        _singleAlbums.value = sortAndPin(deduplicatedSingleAlbums, params.albumSortMode, params.order)
    }

    private fun <T : Album> sortAndPin(list: List<T>, mode: AlbumSortMode, order: List<String>): List<T> {
        val sorted = when (mode) {
            AlbumSortMode.LastModified -> list.sortedBy { it.date }
            AlbumSortMode.LastModifiedDesc -> list.sortedByDescending { it.date }
            AlbumSortMode.Alphabetically -> list.sortedBy { it.name }
            AlbumSortMode.AlphabeticallyDesc -> list.sortedByDescending { it.name }

            else -> {
                val lut = list.associateBy { it.id }
                val orderSet = order.toSet()

                val ordered = order.mapNotNull { lut[it] }
                val rest = list.filter { it.id !in orderSet }

                ordered + rest
            }
        }

        if (mode == AlbumSortMode.Custom) return sorted

        val (pinned, unpinned) = sorted.partition { it.pinned }
        return pinned + unpinned
    }
}

fun createAlbumGridState(
    context: Context,
    coroutineScope: CoroutineScope,
    apiClient: ApiClient
) = AlbumGridState(
    scope = coroutineScope,
    context = context,
    mediaDao = MediaDatabase.getInstance(context).mediaDao(),
    customDao = MediaDatabase.getInstance(context).customDao(),
    albumsFlow = PhotosApplication.appModule.settings.albums.get(),
    sortModeFlow = PhotosApplication.appModule.settings.photoGrid.getSortMode(),
    albumSortModeFlow = PhotosApplication.appModule.settings.albums.getSortMode(),
    allAlbumsFlow = PhotosApplication.appModule.settings.albums.getAutoDetect(),
    albumGroupsFlow = PhotosApplication.appModule.settings.albums.getGroups(),
    albumsOrderFlow = PhotosApplication.appModule.settings.albums.getOrder(),
    info = PhotosApplication.appModule.settings.immich.getImmichBasicInfo(),
    apiClient = apiClient,
    updateAlbums = { added, updated, removed ->
        val settings = PhotosApplication.appModule.settings.albums
        settings.removeAll(removed) // order is important here
        settings.add(added)

        updated.forEach { album ->
            settings.edit(
                id = album.id,
                newInfo = album
            )
        }
    }
)