package com.kaii.photos.repositories

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.serialization.albums.AlbumGetState
import com.kaii.lavender.immichintegration.serialization.assets.AssetType
import com.kaii.lavender.immichintegration.state_managers.AlbumsStateManager
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImmichRepository(
    private val immichId: String,
    private val info: ImmichBasicInfo,
    private val scope: CoroutineScope,
    private val sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    apiClient: ApiClient
) {
    private val albumState = mutableStateOf(
        AlbumsStateManager(
            baseUrl = info.endpoint,
            coroutineScope = scope,
            apiClient = apiClient
        )
    )

    private val immichItems = MutableStateFlow<List<MediaStoreData>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = immichItems.flatMapLatest { media ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = { ListPagingSource(media = media) }
        ).flow.mapToMedia(accessToken = info.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = mediaFlow.mapToSeparatedMedia(
        sortMode = sortMode,
        format = format
    ).cachedIn(scope)

    init {
        refresh()
    }

    fun refresh() = scope.launch(Dispatchers.IO) { refetch() }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun refetch() {
        albumState.value.getInfo(
            id = Uuid.parse(immichId),
            accessToken = info.accessToken
        ) { state ->
            if (state is AlbumGetState.Retrieved) {
                immichItems.value =
                    state.album.assets.fastMap { asset ->
                        MediaStoreData(
                            id = Uuid.parse(asset.id).toLongs { a, _ -> a },
                            uri = "${info.endpoint}/api/assets/${asset.id}/original",
                            dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                            dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                            type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                            absolutePath = "",
                            parentPath = "",
                            displayName = asset.originalFileName,
                            mimeType = asset.originalMimeType,
                            immichUrl = "${info.endpoint}/api/assets/${asset.id}/original",
                            immichThumbnail = "${info.endpoint}/api/assets/${asset.id}/thumbnail",
                            hash = "", // TODO
                            size = 0L, // TODO
                            customId = null,
                            favourited = asset.isFavorite
                        )
                    }.sortedByDescending {
                        if (sortMode.isDateModified) it.dateModified
                        else it.dateTaken
                    }
            } else {
                immichItems.value = emptyList()
            }
        }.join()
    }
}