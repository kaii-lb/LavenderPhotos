package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(
    private val context: Context,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == SearchViewModel::class.java) {
            return SearchViewModel(context) as T
        }
        throw IllegalArgumentException("${SearchViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${SearchViewModel::class.simpleName}!! This should never happen!!")
    }
}
