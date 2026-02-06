package com.kaii.photos.models.trash_bin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class TrashViewModelFactory(
    private val context: Context,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TrashViewModel::class.java) {
            return TrashViewModel(context, sortMode, format) as T
        }
        throw IllegalArgumentException("TrashViewModel: Cannot cast ${modelClass.simpleName} as ${TrashViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
