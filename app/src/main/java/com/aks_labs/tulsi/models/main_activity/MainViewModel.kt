package com.aks_labs.tulsi.models.main_activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks_labs.tulsi.datastore.PhotoGrid
import com.aks_labs.tulsi.datastore.Settings
import com.aks_labs.tulsi.helpers.Updater
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import com.aks_labs.tulsi.models.multi_album.groupPhotosBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MAIN_VIEW_MODEL"

class MainViewModel(context: Context) : ViewModel() {
    private val _groupedMedia = MutableStateFlow<List<MediaStoreData>?>(null)
    val groupedMedia: Flow<List<MediaStoreData>?> = _groupedMedia.asStateFlow()

    val permissionQueue = mutableStateListOf<String>()

    val settings = Settings(context, viewModelScope)

    val updater = Updater(context = context, coroutineScope = viewModelScope)

    // Grid view mode state
    private val _isGridViewMode = MutableStateFlow(true) // Default to grid view
    val isGridViewMode = _isGridViewMode

    // Initialize grid view mode from preferences
    init {
        viewModelScope.launch {
            settings.PhotoGrid.getGridViewMode().collect { isGridView ->
                _isGridViewMode.value = isGridView
            }
        }
    }

    // Toggle grid view mode
    fun toggleGridViewMode() {
        val newMode = !_isGridViewMode.value

        // Update the state immediately for UI responsiveness
        _isGridViewMode.value = newMode

        // Also update the grouped media if available
        _groupedMedia.value?.let { currentMedia ->
            val filteredMedia = currentMedia.filter { it.type != MediaType.Section }
            if (filteredMedia.isNotEmpty()) {
                // Import needed for this to work
                val regroupedMedia = com.aks_labs.tulsi.models.multi_album.groupPhotosBy(
                    filteredMedia,
                    com.aks_labs.tulsi.helpers.MediaItemSortMode.DateTaken,
                    newMode
                )
                _groupedMedia.value = regroupedMedia
            }
        }

        // Save the preference in the background
        viewModelScope.launch {
            settings.PhotoGrid.setGridViewMode(newMode)
        }
    }

    // Set grid view mode
    fun setGridViewMode(isGridView: Boolean) {
        if (_isGridViewMode.value == isGridView) return

        // Update the state immediately for UI responsiveness
        _isGridViewMode.value = isGridView

        // Also update the grouped media if available
        _groupedMedia.value?.let { currentMedia ->
            val filteredMedia = currentMedia.filter { it.type != MediaType.Section }
            if (filteredMedia.isNotEmpty()) {
                // Import needed for this to work
                val regroupedMedia = com.aks_labs.tulsi.models.multi_album.groupPhotosBy(
                    filteredMedia,
                    com.aks_labs.tulsi.helpers.MediaItemSortMode.DateTaken,
                    isGridView
                )
                _groupedMedia.value = regroupedMedia
            }
        }

        // Save the preference in the background
        viewModelScope.launch {
            settings.PhotoGrid.setGridViewMode(isGridView)
        }
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
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.MANAGE_MEDIA
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_MEDIA
                )
            } else {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

        permList.forEach { perm ->
            val granted = when (perm) {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                    Environment.isExternalStorageManager()
                }

                Manifest.permission.MANAGE_MEDIA -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaStore.canManageMedia(context)
                    else false
                }

                else -> {
                    context.checkSelfPermission(
                        perm
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }

            if (!granted && !permissionQueue.contains(perm)) permissionQueue.add(perm)
            else permissionQueue.remove(perm)

            Log.d(TAG, "Permission $perm has been granted $granted")
        }

        permissionQueue.forEach {
            Log.d(TAG, "Permission queue has item $it")
        }
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if (!isGranted && !permissionQueue.contains(permission)) permissionQueue.add(permission)
        else if (isGranted) permissionQueue.remove(permission)

        permissionQueue.forEach { Log.d(TAG, "PERMISSION DENIED $it") }
    }

    fun checkCanPass(): Boolean {
        val manageMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionQueue.all { it == Manifest.permission.MANAGE_MEDIA }
        } else {
            false
        }

        permissionQueue.forEach {
            Log.d(TAG, "Can pass permission queue has item $it")
        }

        return permissionQueue.isEmpty() || manageMedia
    }

    /** launch tasks on the mainViewModel scope */
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend () -> Unit
    ) = viewModelScope.launch(dispatcher) {
        block()
    }
}


