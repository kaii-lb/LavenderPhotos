package com.kaii.photos.models.main_activity

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainDataSharingModel() : ViewModel() {
	private val _selectedMediaUri = MutableStateFlow<MediaStoreData?>(null)
    val selectedMediaUri: Flow<MediaStoreData?> = _selectedMediaUri.asStateFlow()

    fun setSelectedMediaUri(newMediaStoreData: MediaStoreData?) {
        _selectedMediaUri.value = newMediaStoreData
    }
}
