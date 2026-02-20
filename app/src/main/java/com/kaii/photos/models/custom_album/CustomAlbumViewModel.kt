package com.kaii.photos.models.custom_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.CustomRepository

class CustomAlbumViewModel(
    private val albumInfo: AlbumInfo,
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = CustomRepository(
        context = context,
        albumInfo = albumInfo,
        scope = viewModelScope,
        info = info,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun remove(items: Set<MediaStoreData>) = repo.remove(items, albumInfo.id)
    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }
}