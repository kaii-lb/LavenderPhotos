package com.kaii.photos.models.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datastore.AlbumType

@Suppress("UNCHECKED_CAST")
class EditorViewModelFactory(
    private val context: Context,
    private val album: AlbumType
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == EditorViewModel::class.java) {
            return EditorViewModel(context, album) as T
        }
        throw IllegalArgumentException("${EditorViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${EditorViewModel::class.simpleName}!! This should never happen!!")
    }
}
