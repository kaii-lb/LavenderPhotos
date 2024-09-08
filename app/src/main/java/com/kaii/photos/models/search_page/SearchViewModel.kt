package com.kaii.photos.models.search_page

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.MediaSearchType
import com.kaii.photos.mediastore.AlbumStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.SearchStoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class SearchViewModel(context: Context, searchFor: String) : ViewModel() {
    private val mediaStoreDataSource = SearchStoreDataSource(context, searchFor)

    private val _uiState: MutableStateFlow<List<MediaStoreData>> = MutableStateFlow(emptyList())
    val mediaStoreData: StateFlow<List<MediaStoreData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
                _uiState.value = it
            }
        }
    }

    fun setMediaStoreData(context: Context, searchFor: String) {
    	val mediaStoreDataSource = SearchStoreDataSource(context, searchFor)	
    	viewModelScope.launch {
            mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
                _uiState.value = it
            }
        }
    }
}
