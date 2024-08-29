package com.kaii.photos.models.album_grid

import android.content.Context
import android.net.Uri
import android.text.format.DateFormat.format
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.mediastore.AlbumStoreDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaStoreDataSource
import com.kaii.photos.mediastore.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AlbumsViewModel(context: Context, paths: List<String>) : ViewModel() {
    private val mediaStoreDataSource = AlbumStoreDataSource(context, paths)

    private val _uiState: MutableStateFlow<LinkedHashMap<String, MediaStoreData>> = MutableStateFlow(LinkedHashMap<String, MediaStoreData>())
    val mediaStoreData: StateFlow<LinkedHashMap<String, MediaStoreData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).collect {
                _uiState.value = it
            }
        }
    }
}
