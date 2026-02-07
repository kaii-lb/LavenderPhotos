package com.kaii.photos.models.immich_album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class ImmichAlbumViewModelFactory(
    private val immichId: String,
    private val info: ImmichBasicInfo,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat,
    private val apiClient: ApiClient
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichAlbumViewModel::class.java) {
            return ImmichAlbumViewModel(immichId, info, sortMode, format, apiClient) as T
        }
        throw IllegalArgumentException("${ImmichAlbumViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichAlbumViewModel::class.simpleName}!! This should never happen!!")
    }
}
