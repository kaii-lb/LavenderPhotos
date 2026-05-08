package com.kaii.photos.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumCreationInfo
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumCreationState
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumsGetAllState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ImmichBackupOptionsStateImpl {
    val query: StateFlow<String>
    val assetCount: Flow<Int>
    val albums: Flow<List<AlbumGridState.Album.Single>>

    fun selectedCount(): Int
    fun selected(id: String): Boolean
    fun toggle(id: String)
    fun search(query: String)
    suspend fun confirm(context: Context): Boolean
}

class ImmichBackupOptionsState(
    mediaDao: MediaDao,
    scope: CoroutineScope,
    albumsFlow: StateFlow<List<AlbumGridState.Album.Single>>,
    private val settings: SettingsAlbumsListImpl,
    private val apiClient: ApiClient,
    private val info: Flow<ImmichBasicInfo>
) : ImmichBackupOptionsStateImpl {
    private val selectedAlbumIds = mutableStateListOf<String>()
    private val queryFlow = MutableStateFlow("")

    override val query = queryFlow.asStateFlow()
    override val assetCount = mediaDao.immichMediaCount()

    override val albums = albumsFlow.combine(queryFlow) { list, query ->
        list.filter { album ->
            album.name.contains(query, true) && album.info.album !is AlbumType.Cloud
        }
    }

    init {
        scope.launch {
            albumsFlow.first()
                .filter {
                    it.info.album.immichId != null && it.info.album !is AlbumType.Cloud
                }
                .forEach {
                    selectedAlbumIds.add(it.id)
                }
        }
    }

    override fun selectedCount() = selectedAlbumIds.size

    override fun selected(id: String) = selectedAlbumIds.contains(id)

    override fun toggle(id: String) {
        if (selectedAlbumIds.contains(id)) selectedAlbumIds.remove(id)
        else selectedAlbumIds.add(id)
    }

    override fun search(query: String) {
        queryFlow.value = query
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun confirm(context: Context) = withContext(Dispatchers.IO) {

        val info = info.first()
        val albumsClient = AlbumsClient(
            baseUrl = info.endpoint,
            client = apiClient
        )

        val albums = settings.get().first().filter { it !is AlbumType.Cloud }
        val local = albums.filter { it.id in selectedAlbumIds }
        val cloud = albumsClient.getAll(accessToken = info.accessToken).let {
            (it as? AlbumsGetAllState.Retrieved)?.albums
        } ?: return@withContext false

        local.forEach { album ->
            val exists = cloud.any {
                it.id == album.immichId
            }

            if (exists) return@forEach

            var immichId = cloud.find {
                it.albumName == album.name
            }?.id

            if (immichId == null) {
                val response = albumsClient.createAlbum(
                    info = AlbumCreationInfo(
                        albumName = album.name,
                        albumUsers = emptyList(),
                        assetIds = emptyList(),
                        description = ""
                    ),
                    accessToken = info.accessToken
                )

                immichId = (response as? AlbumCreationState.Created)?.album?.id ?: return@forEach
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

        cloud.forEach { cloudAlbum ->
            val album = albums.find {
                it.immichId == cloudAlbum.id
            }

            if (album == null || album.id in selectedAlbumIds) return@forEach

            albumsClient.delete(
                id = Uuid.parse(cloudAlbum.id),
                accessToken = info.accessToken
            )

            settings.edit(
                id = album.id,
                newInfo = when (album) {
                    is AlbumType.Folder -> album.copy(immichId = null)
                    is AlbumType.Custom -> album.copy(immichId = null)
                    else -> throw IllegalStateException("Cannot operate on a cloud album!")
                }
            )
        }

        CloudSyncWorker.immediateEnqueue(context)

        return@withContext true
    }
}

@Composable
fun rememberImmichBackupOptionsState(): ImmichBackupOptionsState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember {
        val appModule = context.appModule

        ImmichBackupOptionsState(
            mediaDao = MediaDatabase.getInstance(context).mediaDao(),
            scope = scope,
            albumsFlow = appModule.albumGridState.singleAlbums,
            settings = appModule.settings.albums,
            apiClient = appModule.apiClient,
            info = appModule.settings.immich.getImmichBasicInfo()
        )
    }
}