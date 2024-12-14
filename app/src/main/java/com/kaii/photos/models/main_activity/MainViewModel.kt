package com.kaii.photos.models.main_activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
	private val _selectedMedia = MutableStateFlow<MediaStoreData?>(null)
    val selectedMediaData: Flow<MediaStoreData?> = _selectedMedia.asStateFlow()

    private val _selectedAlbumDir = MutableStateFlow<String?>(null)
    val selectedAlbumDir: Flow<String?> = _selectedAlbumDir.asStateFlow()

    private val _groupedMedia = MutableStateFlow<List<MediaStoreData>?>(null)
    val groupedMedia: Flow<List<MediaStoreData>?> = _groupedMedia.asStateFlow()

    val permissionQueue = mutableStateListOf<String>()

    fun setSelectedMediaData(newMediaStoreData: MediaStoreData?) {
        _selectedMedia.value = newMediaStoreData
    }

    fun setSelectedAlbumDir(newAlbumDir: String?) {
        _selectedAlbumDir.value = newAlbumDir
    }
    fun setGroupedMedia(media: List<MediaStoreData>?) {
        _groupedMedia.value = media
    }

    fun startupPermissionCheck(context: Context) {
    	// READ_MEDIA_VIDEO isn't necessary as its bundled with READ_MEDIA_IMAGES
    	val permList =
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    			listOf(
		    		Manifest.permission.READ_MEDIA_IMAGES,
		    		Manifest.permission.MANAGE_EXTERNAL_STORAGE,
		    		Manifest.permission.MANAGE_MEDIA
    			)
    		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    			listOf(
		    		Manifest.permission.READ_EXTERNAL_STORAGE,
		    		Manifest.permission.MANAGE_EXTERNAL_STORAGE,
		    		Manifest.permission.MANAGE_MEDIA
    			)
    		} else {
    			listOf(
		    		Manifest.permission.READ_EXTERNAL_STORAGE,
		    		Manifest.permission.MANAGE_EXTERNAL_STORAGE
    			)
    		}

		val pid = android.os.Process.myPid()
		val uid = android.os.Process.myUid()
		permList.forEach { perm ->
			val granted =
				if (perm == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
					Environment.isExternalStorageManager()
				} else if (perm == Manifest.permission.MANAGE_MEDIA) {
					MediaStore.canManageMedia(context)
				} else {
					context.checkPermission(
					    perm,
					    pid,
					    uid
					) == PackageManager.PERMISSION_GRANTED
				}

			if (!granted) permissionQueue.add(perm)
			else permissionQueue.remove(perm)
		}
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
       	if (!isGranted && !permissionQueue.contains(permission)) permissionQueue.add(permission)
        else if (isGranted) permissionQueue.remove(permission)
    }

    fun checkCanPass() : Boolean {
    	return permissionQueue.isEmpty() || (permissionQueue.size == 1 && permissionQueue.first() == Manifest.permission.MANAGE_MEDIA)
    }
}
