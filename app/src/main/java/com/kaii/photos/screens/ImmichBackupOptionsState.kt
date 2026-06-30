package com.kaii.photos.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kaii.photos.PhotosApplication
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.appCloudFolderDir
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.Album
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumCreateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

abstract class ImmichBackupOptionsStateImpl {
    abstract val query: StateFlow<String>
    abstract val assetCount: StateFlow<Int>
    abstract val albums: StateFlow<List<AlbumGridState.Album.Single>>
    abstract var immichInfo: ImmichBasicInfo
        protected set

    var isLoading by mutableStateOf(false)
        protected set
    var hasUnsavedChanges by mutableStateOf(false)
        protected set

    abstract fun selectedCount(): Int
    abstract fun selected(id: String): Boolean
    abstract fun toggle(id: String)
    abstract fun search(query: String)
    abstract suspend fun confirm(context: Context): Boolean
    abstract suspend fun refresh()
}

class ImmichBackupOptionsState(
    mediaDao: MediaDao,
    scope: CoroutineScope,
    albumsFlow: StateFlow<List<AlbumGridState.Album.Single>>,
    info: Flow<ImmichBasicInfo>,
    private val settings: SettingsAlbumsListImpl,
    private val apiClient: ApiClient
) : ImmichBackupOptionsStateImpl() {
    private val selectedAlbumIds = mutableStateListOf<String>()
    private val _query = MutableStateFlow("")
    private var albumTypes = emptyList<AlbumType>()

    override var immichInfo by mutableStateOf(ImmichBasicInfo.Empty)

    override val query = _query.asStateFlow()
    override val assetCount = mediaDao.immichMediaCount().stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    override val albums = albumsFlow.combine(_query) { list, query ->
        list.filter { album ->
            album.name.contains(query, true)
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        scope.launch {
            launch {
                refresh()
            }

            launch {
                info.collect {
                    immichInfo = it
                    refresh()
                }
            }

            launch {
                settings.get().collect {
                    albumTypes = it
                }
            }
        }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        hasUnsavedChanges = false
        isLoading = true

        val albumsClient = AlbumsClient(
            endpoint = immichInfo.endpoint,
            auth = immichInfo.auth,
            client = apiClient
        )

        val cloud = albumsClient.getAll()?.map { it.id }

        if (cloud == null) {
            isLoading = false
            return@withContext
        }

        albumTypes
            .filter { album ->
                album !is AlbumType.Cloud
            }
            .forEach { album ->
                val contained = selectedAlbumIds.contains(album.id)

                if (album.immichId in cloud) {
                    if (!contained) selectedAlbumIds.add(album.id)
                } else {
                    if (contained) selectedAlbumIds.remove(album.id)

                    settings.edit(
                        id = album.id,
                        newInfo = when (album) {
                            is AlbumType.Folder -> album.copy(immichId = null)
                            is AlbumType.Custom -> album.copy(immichId = null)
                            else -> throw IllegalStateException("Cannot operate on a cloud album!")
                        }
                    )
                }
            }

        delay(1000.milliseconds) // eye candy

        isLoading = false
    }

    override fun selectedCount() = selectedAlbumIds.size

    override fun selected(id: String) = selectedAlbumIds.contains(id)

    override fun toggle(id: String) {
        hasUnsavedChanges = true

        if (selectedAlbumIds.contains(id)) selectedAlbumIds.remove(id)
        else selectedAlbumIds.add(id)
    }

    override fun search(query: String) {
        _query.value = query
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun confirm(context: Context) = withContext(Dispatchers.IO) {
        isLoading = true

        val albumsClient = AlbumsClient(
            endpoint = immichInfo.endpoint,
            auth = immichInfo.auth,
            client = apiClient
        )

        val local = albumTypes.filter { it.id in selectedAlbumIds }
        val cloud = albumsClient.getAll()

        if (cloud == null) {
            isLoading = false
            return@withContext false
        }

        local.forEach { album ->
            if (album !is AlbumType.Cloud) {
                val exists = cloud.any {
                    it.id == album.immichId
                }

                if (exists) return@forEach

                linkToCloud(
                    album = album,
                    cloud = cloud,
                    client = albumsClient
                )
            } else {
                makeLocal(album = album)
            }
        }

        val nonCloud = albumTypes.filter { it !is AlbumType.Cloud }
        cloud.forEach { cloudAlbum ->
            val album = nonCloud.find {
                it.immichId == cloudAlbum.id
            }

            if (album == null || album.id in selectedAlbumIds) return@forEach

            if (album !is AlbumType.Folder || !album.wasCloud) {
                albumsClient.delete(
                    id = Uuid.parse(cloudAlbum.id)
                )
            }

            if (album is AlbumType.Folder && album.wasCloud) {
                File(album.paths.first()).deleteRecursively()
                settings.remove(album.id)
            } else {
                settings.edit(
                    id = album.id,
                    newInfo = when (album) {
                        is AlbumType.Folder -> album.copy(immichId = null)
                        is AlbumType.Custom -> album.copy(immichId = null)
                        else -> throw IllegalStateException("Cannot operate on a cloud album!")
                    }
                )
            }
        }

        CloudSyncWorker.immediateEnqueue(context = context, albumId = null)

        delay(1000.milliseconds) // eye candy

        hasUnsavedChanges = false
        isLoading = false

        return@withContext true
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun linkToCloud(
        album: AlbumType,
        cloud: List<Album>,
        client: AlbumsClient
    ) {
        var immichId = cloud.find {
            it.albumName == album.name
        }?.id

        if (immichId == null) {
            val response = client.createAlbum(
                info = AlbumCreateRequest(
                    albumName = album.name,
                    albumUsers = emptyList(),
                    assetIds = emptyList(),
                    description = ""
                )
            )

            immichId = response?.id ?: return
        }

        settings.edit(
            id = album.id,
            newInfo = when (album) {
                is AlbumType.Folder -> album.copy(immichId = immichId)
                is AlbumType.Custom -> album.copy(immichId = immichId)
                else -> throw IllegalStateException("Cannot operate on a cloud album!")
            }
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun makeLocal(
        album: AlbumType.Cloud
    ) {
        val dir = File(appCloudFolderDir, album.name + "-" + album.immichId.take(5))
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val newAlbum = AlbumType.Folder(
            id = Uuid.random().toString(),
            name = album.name,
            pinned = album.pinned,
            immichId = album.immichId,
            paths = setOf(dir.absolutePath),
            wasCloud = true
        )

        albumTypes.filter { album ->
            album is AlbumType.Folder && album.paths == setOf(dir.absolutePath)
        }.let { matches ->
            settings.removeAll(
                albumIds = matches.map { it.id }
            )
        }.join()

        settings.edit(
            id = album.id,
            newInfo = newAlbum,
            overwriteId = true
        ).join()

        selectedAlbumIds.remove(album.id)
        selectedAlbumIds.add(newAlbum.id)
    }
}

@Composable
fun rememberImmichBackupOptionsState(): ImmichBackupOptionsState {
    val scope = rememberCoroutineScope()

    return remember {
        val appModule = PhotosApplication.appModule

        ImmichBackupOptionsState(
            mediaDao = appModule.db.mediaDao(),
            scope = scope,
            albumsFlow = appModule.albumGridState.singleAlbums,
            settings = appModule.settings.albums,
            apiClient = appModule.apiClient,
            info = appModule.settings.immich.getImmichBasicInfo()
        )
    }
}