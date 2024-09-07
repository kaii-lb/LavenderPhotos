package com.kaii.photos.models.main_activity

import androidx.lifecycle.ViewModel
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.MainScreenViewType

class MainDataSharingModel() : ViewModel() {
    companion object {
        const val IGNORE_THIS_PATH = "com.kaii.photos.please_ignore_this_path"
    }

	private val _selectedMedia = MutableStateFlow<MediaStoreData?>(null)
    val selectedMediaData: Flow<MediaStoreData?> = _selectedMedia.asStateFlow()

    private val _selectedAlbumDir = MutableStateFlow<String?>(null)
    val selectedAlbumDir: Flow<String?> = _selectedAlbumDir.asStateFlow()

    private val _groupedMedia = MutableStateFlow<List<MediaStoreData>?>(null)
    val groupedMedia: Flow<List<MediaStoreData>?> = _groupedMedia.asStateFlow()

    fun setSelectedMediaData(newMediaStoreData: MediaStoreData?) {
        _selectedMedia.value = newMediaStoreData
    }

    fun setSelectedAlbumDir(newAlbumDir: String?) {
        _selectedAlbumDir.value = newAlbumDir
    }
    fun setGroupedMedia(media: List<MediaStoreData>?) {
        _groupedMedia.value = media
    }
}
