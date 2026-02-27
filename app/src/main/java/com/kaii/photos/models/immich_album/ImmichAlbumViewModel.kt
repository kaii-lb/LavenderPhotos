package com.kaii.photos.models.immich_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.ImmichRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ImmichAlbumViewModel(
    albumInfo: AlbumInfo,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    apiClient: ApiClient,
    context: Context
) : ViewModel() {
    private val repo = ImmichRepository(
        albumInfo = albumInfo,
        info = info,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format,
        apiClient = apiClient,
        context = context
    )

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(5000)
            }
        }
    }

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    fun refresh() = repo.refresh()

    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }
}