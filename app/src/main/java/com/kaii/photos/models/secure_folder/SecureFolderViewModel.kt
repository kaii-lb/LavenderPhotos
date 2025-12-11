package com.kaii.photos.models.secure_folder

import android.content.Context
import android.os.FileObserver
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

private const val TAG = "com.kaii.photos.models.LockedFolderViewModel"

class SecureFolderViewModel(context: Context, sortMode: MediaItemSortMode, displayDateFormat: DisplayDateFormat) : ViewModel() {
    private val secureFolder = File(context.appSecureFolderDir)
    private val dao = MediaDatabase.getInstance(context).securedItemEntityDao()
    private val mediaStoreData = mutableListOf<MediaStoreData>()
    private var job: Job? = null

    private val fileObserver =
        object : FileObserver(File(context.appSecureFolderDir), CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                // doesn't matter what event type just refresh
                if (path != null) {
                    _fileList.value = secureFolder.listFiles()
                    Log.d(TAG, "File path changed: $path")

                    load(
                        context = context,
                        sortMode = sortMode,
                        displayDateFormat = displayDateFormat
                    )
                }
            }
        }

    private val _fileList = MutableStateFlow(secureFolder.listFiles())
    val fileList = _fileList.asStateFlow()

    private val _groupedMedia = MutableStateFlow(emptyList<MediaStoreData>())
    val groupedMedia = _groupedMedia.asStateFlow()

    init {
        load(
            context = context,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    }

    fun attachFileObserver() {
        fileObserver.startWatching()
    }
    fun stopFileObserver() {
        fileObserver.stopWatching()
    }

    private fun load(
        context: Context,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ): Job {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            val snapshot = _fileList.value ?: return@launch
            val paths = snapshot.map { it.absolutePath }

            // remove deleted/moved media
            mediaStoreData.removeIf {
                it.absolutePath !in paths
            }

            snapshot.forEach { file ->
                // if file is already processed, skip processing it
                if (mediaStoreData.any { it.absolutePath == file.absolutePath }) {
                    return@forEach
                }

                val mimeType = Files.probeContentType(Path(file.absolutePath))

                val type =
                    if (mimeType.lowercase().contains("image")) MediaType.Image
                    else if (mimeType.lowercase().contains("video")) MediaType.Video
                    else MediaType.Section

                val decryptedBytes =
                    run {
                        val iv = dao.getIvFromSecuredPath(file.absolutePath)
                        val thumbnailIv = dao.getIvFromSecuredPath(
                            getSecuredCacheImageForFile(file = file, context = context).absolutePath
                        )

                        if (iv != null && thumbnailIv != null) iv + thumbnailIv else ByteArray(32)
                    }

                val originalPath =
                    dao.getOriginalPathFromSecuredPath(file.absolutePath) ?: context.appRestoredFilesDir

                val item = MediaStoreData(
                    type = type,
                    id = file.hashCode() * file.length() * file.lastModified(),
                    uri = FileProvider.getUriForFile(
                        context,
                        LAVENDER_FILE_PROVIDER_AUTHORITY,
                        file
                    ),
                    mimeType = mimeType,
                    dateModified = file.lastModified() / 1000,
                    dateTaken = file.lastModified() / 1000,
                    displayName = file.name,
                    absolutePath = file.absolutePath,
                    bytes = decryptedBytes + originalPath.encodeToByteArray()
                )

                mediaStoreData.add(item)
            }

            _groupedMedia.value = groupPhotosBy(
                media = mediaStoreData,
                sortBy = if (sortMode == MediaItemSortMode.Disabled) sortMode else MediaItemSortMode.LastModified,
                displayDateFormat = displayDateFormat,
                context = context
            )

            delay(PhotoGridConstants.LOADING_TIME)
        }

        return job!!
    }
}