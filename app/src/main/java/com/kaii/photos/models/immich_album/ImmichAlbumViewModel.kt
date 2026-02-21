package com.kaii.photos.models.immich_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.ImmichRepository
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

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun refresh() = repo.refresh()
}