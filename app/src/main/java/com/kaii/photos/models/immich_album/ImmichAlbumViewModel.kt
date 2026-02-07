package com.kaii.photos.models.immich_album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.repositories.ImmichRepository
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ImmichAlbumViewModel(
    private val immichId: String,
    private val info: ImmichBasicInfo,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat,
    private val apiClient: ApiClient
) : ViewModel() {
    private val repo = ImmichRepository(
        immichId = immichId,
        info = info,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format,
        apiClient = apiClient
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun refresh() = repo.refresh()
}