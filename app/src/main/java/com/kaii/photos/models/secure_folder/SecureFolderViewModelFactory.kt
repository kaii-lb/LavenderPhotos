package com.kaii.photos.models.secure_folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.models.multi_album.DisplayDateFormat

@Suppress("UNCHECKED_CAST")
class SecureFolderViewModelFactory(
    private val context: Context,
    private val sortMode: MediaItemSortMode,
    private val displayDateFormat: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SecureFolderViewModel::class.java) {
            return SecureFolderViewModel(context, sortMode, displayDateFormat) as T
        }
        throw IllegalArgumentException("SecureFolderViewModelFactory: Cannot cast ${modelClass.simpleName} as ${SecureFolderViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
