package com.kaii.photos.models.album_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.mediastore.AlbumStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AlbumsViewModel(context: Context, paths: List<String>) : ViewModel() {
    private val mediaStoreDataSource = AlbumStoreDataSource(context, paths)

    private val _uiState: MutableStateFlow<LinkedHashMap<String, MediaStoreData>> = MutableStateFlow(LinkedHashMap())
    val mediaStoreData: StateFlow<LinkedHashMap<String, MediaStoreData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
                _uiState.value = it
            }
        }
    }
}
