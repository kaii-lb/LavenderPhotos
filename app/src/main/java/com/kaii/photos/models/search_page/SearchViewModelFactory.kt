package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(
    private val context: Context,
    private val sortMode: MediaItemSortMode,
    private val displayDateFormat: DisplayDateFormat
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SearchViewModel::class.java) {
            return SearchViewModel(context, sortMode, displayDateFormat) as T
        }
        throw IllegalArgumentException("SearchViewModel: Cannot cast ${modelClass.simpleName} as ${SearchViewModel::class.java.simpleName}!! This should never happen!!")
    }
}
