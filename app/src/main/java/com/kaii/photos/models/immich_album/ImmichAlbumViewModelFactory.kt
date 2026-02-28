package com.kaii.photos.models.immich_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient

@Suppress("UNCHECKED_CAST")
class ImmichAlbumViewModelFactory(
    private val albumInfo: AlbumInfo,
    private val info: ImmichBasicInfo,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat,
    private val apiClient: ApiClient,
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ImmichAlbumViewModel::class.java) {
            return ImmichAlbumViewModel(albumInfo, info, sortMode, format, apiClient, context) as T
        }
        throw IllegalArgumentException("${ImmichAlbumViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ImmichAlbumViewModel::class.simpleName}!! This should never happen!!")
    }
}
