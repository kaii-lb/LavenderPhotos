package com.kaii.photos.models.immich_album

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.serialization.albums.AlbumGetState
import com.kaii.lavender.immichintegration.serialization.assets.AssetType
import com.kaii.lavender.immichintegration.state_managers.AlbumsStateManager
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// private const val TAG = "com.kaii.photos.models.ImmichAlbumViewModel"

@OptIn(ExperimentalUuidApi::class)
class ImmichAlbumViewModel(
    private var immichId: String,
    var info: ImmichBasicInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat,
    private val apiClient: ApiClient
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

    private val _mediaFlow = MutableStateFlow<List<MediaStoreData>>(emptyList())
    val mediaFlow = _mediaFlow.asStateFlow()

    private val _hasFiles = MutableStateFlow(true)
    val hasFiles = _hasFiles.asStateFlow()

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
                            uri = "${info.endpoint}/api/assets/${asset.id}/original".toUri(),
                            dateTaken = Instant.parse(asset.fileCreatedAt).epochSeconds,
                            dateModified = Instant.parse(asset.fileModifiedAt).epochSeconds,
                            type = if (asset.type == AssetType.Image) MediaType.Image else MediaType.Video,
                            absolutePath = asset.originalFileName,
                            displayName = asset.originalFileName,
                            mimeType = asset.originalMimeType,
                            immichInfo = ImmichInfo(
                                thumbnail = "${info.endpoint}/api/assets/${asset.id}/thumbnail",
                                original = "${info.endpoint}/api/assets/${asset.id}/original",
                                accessToken = info.accessToken
                            )
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

        _mediaFlow.value =
            groupPhotosBy(
                media = media,
                sortBy = sortMode,
                displayDateFormat = displayDateFormat,
                context = context
            )
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
                displayDateFormat = displayDateFormat
            )
        }
    }

    fun reinitDataSource(
        context: Context,
        info: ImmichBasicInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        if (info == this.info && sortMode == this.sortMode && displayDateFormat == this.displayDateFormat) return

        this.info = info
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

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
                displayDateFormat = this.displayDateFormat
            )

        refresh(context)
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

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
        this.displayDateFormat = displayDateFormat

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