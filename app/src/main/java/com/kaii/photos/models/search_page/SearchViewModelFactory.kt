package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(
    private val context: Context,
    private val info: ImmichBasicInfo,
    private val sortMode: MediaItemSortMode,
    private val format: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SearchViewModel::class.java) {
            return SearchViewModel(context, info, sortMode, format) as T
        }
        throw IllegalArgumentException("${SearchViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${SearchViewModel::class.simpleName}!! This should never happen!!")
    }
}
