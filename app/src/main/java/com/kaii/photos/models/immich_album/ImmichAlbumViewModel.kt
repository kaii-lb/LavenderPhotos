package com.kaii.photos.models.immich_album

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.serialization.albums.AlbumGetState
import com.kaii.lavender.immichintegration.serialization.assets.AssetType
import com.kaii.lavender.immichintegration.state_managers.AlbumsStateManager
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// private const val TAG = "com.kaii.photos.models.ImmichAlbumViewModel"

@OptIn(ExperimentalUuidApi::class)
class ImmichAlbumViewModel(
    private var immichId: String,
    private var info: ImmichBasicInfo,
    private var sortMode: MediaItemSortMode,
    private var format: DisplayDateFormat,
    private val apiClient: ApiClient,
    private val separators: Boolean
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()

    private val albumState = mutableStateOf(
        AlbumsStateManager(
            baseUrl = info.endpoint,
            coroutineScope = viewModelScope,
            apiClient = apiClient
        )
    )

    private var media = emptyList<MediaStoreData>()

    private val immichItems = MutableStateFlow<List<MediaStoreData>>(emptyList())

    private val _hasFiles = MutableStateFlow(true)
    val hasFiles = _hasFiles.asStateFlow()

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
        ).flow.mapToMedia(sortMode = sortMode, format = format, separators = separators, accessToken = info.accessToken).cachedIn(viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 30.seconds.inWholeMilliseconds),
        initialValue = PagingData.from(emptyList<PhotoLibraryUIModel>())
    )

    private suspend fun refetch() {
        albumState.value.getInfo(
            id = Uuid.parse(immichId),
            accessToken = info.accessToken
        ) { state ->
            if (state is AlbumGetState.Retrieved) {
                _hasFiles.value = true

                media =
                    state.album.assets.map { asset ->
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
                    }
            } else {
                _hasFiles.value = false
                media = emptyList()
            }
        }.join()
    }

    fun refresh(
        context: Context,
        refetch: Boolean = false
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (refetch) refetch()

        immichItems.value = media
        // groupPhotosBy(
        //     media = media,
        //     sortBy = sortMode,
        //     displayDateFormat = displayDateFormat,
        //     context = context
        // ) // TODO
    }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
        cancellationSignal = CancellationSignal()
    }

    fun update(
        context: Context,
        immichId: String,
        info: ImmichBasicInfo
    ) {
        if (immichId != this.immichId) {
            this.immichId = immichId

            reinitDataSource(
                context = context,
                info = info,
                sortMode = sortMode,
                displayDateFormat = format
            )
        }
    }

    fun reinitDataSource(
        context: Context,
        info: ImmichBasicInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.format
    ) {
        if (info == this.info && sortMode == this.sortMode && displayDateFormat == this.format) return

        this.info = info
        this.sortMode = sortMode
        this.format = displayDateFormat

        cancelMediaFlow()
        albumState.value =
            initDataSource(
                info = info,
                sortMode = sortMode,
                displayDateFormat = displayDateFormat
            )

        refresh(context)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        cancelMediaFlow()
        albumState.value =
            initDataSource(
                info = this.info,
                sortMode = sortMode,
                displayDateFormat = this.format
            )

        refresh(context)
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.format == displayDateFormat) return

        this.format = displayDateFormat

        cancelMediaFlow()
        albumState.value =
            initDataSource(
                info = this.info,
                sortMode = this.sortMode,
                displayDateFormat = displayDateFormat
            )

        refresh(context)
    }

    private fun initDataSource(
        info: ImmichBasicInfo,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = run {
        this.info = info
        this.sortMode = sortMode
        this.format = displayDateFormat

        _hasFiles.value = true

        AlbumsStateManager(
            baseUrl = info.endpoint,
            coroutineScope = viewModelScope,
            apiClient = apiClient
        )
    }

    override fun onCleared() {
        super.onCleared()
        cancelMediaFlow()
    }
}