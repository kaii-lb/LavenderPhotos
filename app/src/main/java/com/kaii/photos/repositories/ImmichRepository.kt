package com.kaii.photos.repositories

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.toExifData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.immichDurationToSecondsOrNull
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumGetState
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImmichRepository(
    private val album: AlbumType,
    private val scope: CoroutineScope,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>,
    apiClient: ApiClient,
    context: Context
) {
    private data class Params(
        val endpoint: String,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val accessToken: String
    ) : RoomQueryParams(sortMode, format, accessToken)

    private val appContext = context.applicationContext
    private val db = MediaDatabase.getInstance(appContext)

    private var albumsClient by mutableStateOf(
        AlbumsClient(
            baseUrl = "",
            client = apiClient
        )
    )

    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        Params(
            endpoint = info.endpoint,
            accessToken = info.accessToken,
            sortMode = sortMode,
            format = format
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Params(
            endpoint = "",
            accessToken = "",
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = {
                if (params.sortMode.isDateModified) db.customDao().getPagedMediaWithExifDateModified(album = album.id)
                else db.customDao().getPagedMediaWithExifDateTaken(album = album.id)
            }
        ).flow.mapToMedia(accessToken = params.accessToken, is24Hr = DateFormat.is24HourFormat(appContext))
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    init {
        scope.launch {
            params.collectLatest {
                albumsClient =
                    AlbumsClient(
                        baseUrl = it.endpoint,
                        client = apiClient
                    )

                refresh()
            }
        }
    }

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        val snapshot = params.value

        val info = albumsClient.get(
            id = Uuid.parse(album.immichId!!),
            accessToken = snapshot.accessToken,
            withoutAssets = false
        )

        val state = info?.let { AlbumGetState.Retrieved(it) } ?: AlbumGetState.Failed

        if (state is AlbumGetState.Retrieved) {
            val items =
                state.album.assets.fastMap { asset ->
                    MediaStoreData(
                        id = Uuid.parse(asset.id).toLongs { a, _ -> a },
                        uri = "${snapshot.endpoint}/api/assets/${asset.id}/original",
                        dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                        dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                        type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                        absolutePath = "",
                        parentPath = "",
                        displayName = asset.originalFileName,
                        mimeType = asset.originalMimeType,
                        immichUrl = "${snapshot.endpoint}/api/assets/${asset.id}/original",
                        hash = asset.checksum,
                        size = asset.exifInfo?.fileSizeInByte ?: 0L,
                        favourited = asset.isFavorite,
                        duration = asset.duration.immichDurationToSecondsOrNull()
                    )
                }

            val mediaIds = db.customDao().getAllIdsIn(album = album.id).toSet()
            val orphans = db.customDao().getOrphanImmichItems().toSet()
            val added = items.fastMap { it.id }.toSet() - mediaIds
            val deleted = mediaIds - items.fastMap { it.id }.toSet()

            db.withTransaction {
                db.mediaDao().upsertAll(items = items.filter { it.id !in mediaIds })

                db.customDao().deleteAll(ids = deleted, album = album.id)
                db.customDao().upsertAll(items = added.map { CustomItem(id = it, album = album.id) })

                db.exifDataDao().upsertAll(
                    items = state.album.assets.fastMapNotNull {
                        it.exifInfo?.toExifData(
                            mediaId = Uuid.parse(it.id).toLongs { a, _ -> a }
                        )
                    }
                )

                db.mediaDao().deleteAll(orphans.map { it.id }.toSet())
            }
        }
    }

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        return@withContext db.customDao().countMediaInAlbum(album = album.id)
    }

    suspend fun getMediaSize(): Long = withContext(Dispatchers.IO) {
        return@withContext db.customDao().mediaSize(album = album.id)
    }
}