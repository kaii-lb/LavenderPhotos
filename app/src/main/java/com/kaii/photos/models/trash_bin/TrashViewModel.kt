package com.kaii.photos.models.trash_bin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.TrashRepository

class TrashViewModel(
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = TrashRepository(
        scope = viewModelScope,
        info = info,
        context = context,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    override fun onCleared() {
        super.onCleared()
        repo.cancel()
    }

    fun cancel() = repo.cancel()

    fun deleteAll() = repo.deleteAll()
}
