package com.kaii.photos.models.tag_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class TagViewModelFactory(
    private val context: Context,
    private val mediaId: Long
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TagViewModel::class.java) {
            return TagViewModel(mediaId, context) as T
        }
        throw IllegalArgumentException("${TagViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${TagViewModel::class.simpleName}!! This should never happen!!")
    }
}
