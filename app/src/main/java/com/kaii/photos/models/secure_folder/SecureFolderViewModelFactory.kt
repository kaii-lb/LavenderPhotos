package com.kaii.photos.models.secure_folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class SecureFolderViewModelFactory(
    private val context: Context,
    private val info: ImmichBasicInfo,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SecureFolderViewModel::class.java) {
            return SecureFolderViewModel(context, info, sortMode, format) as T
        }
        throw IllegalArgumentException("${SecureFolderViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${SecureFolderViewModel::class.simpleName}!! This should never happen!!")
    }
}
