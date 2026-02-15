package com.kaii.photos.models.secure_folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.SecureRepository

class SecureFolderViewModel(
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = SecureRepository(
        context = context,
        info = info,
        scope = viewModelScope,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    init {
        repo.attachFileObserver()
    }

    override fun onCleared() {
        super.onCleared()
        repo.detachFileObserver()
    }
}