package com.kaii.photos.models.secure_folder

import android.content.Context
import android.os.FileObserver
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.loading.SecuredListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

private const val TAG = "com.kaii.photos.models.LockedFolderViewModel"

private data class QueryParams(
    val securedItems: List<PhotoLibraryUIModel.SecuredMedia>,
    val separators: Boolean
)

class SecureFolderViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val secureFolder = File(context.appSecureFolderDir)
    private var job: Job? = null

    private val fileObserver =
        object : FileObserver(File(context.appSecureFolderDir), CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                // doesn't matter what event type just refresh
                if (path != null) {
                    _fileList.value = secureFolder.listFiles()
                    Log.d(TAG, "File path changed: $path")

                    load(context = context)
                }
            }
        }

    private val _fileList = MutableStateFlow(secureFolder.listFiles())

    private val params = MutableStateFlow(
        value = QueryParams(
            securedItems = emptyList(),
            separators = true
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { (media, separators) ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
            ),
            pagingSourceFactory = { SecuredListPagingSource(media = media) }
        ).flow.mapToMedia(sortMode = sortMode, format = format, separators = separators).cachedIn(viewModelScope)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 30.seconds.inWholeMilliseconds),
        initialValue = PagingData.from(emptyList<PhotoLibraryUIModel>())
    )

    init {
        load(context = context)
    }

    fun attachFileObserver() {
        fileObserver.startWatching()
    }

    fun stopFileObserver() {
        fileObserver.stopWatching()
    }

    fun setSeparators(value: Boolean) {
        params.value = params.value.copy(separators = value)
    }

    private fun load(context: Context): Job {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            val dao = MediaDatabase.getInstance(context).securedItemEntityDao()
            val settings = SettingsImmichImpl(context = context, viewModelScope = viewModelScope)
            val accessToken = settings.getImmichBasicInfo().first().accessToken

            val snapshot = _fileList.value?.sortedBy { it.lastModified() } ?: return@launch

            val mediaStoreData = params.value.securedItems.toMutableList()

            snapshot.forEach { file ->
                // if file is already processed, skip processing it
                if (mediaStoreData.any { it.item.absolutePath == file.absolutePath }) {
                    return@forEach
                }

                val mimeType = Files.probeContentType(Path(file.absolutePath))

                val type =
                    if (mimeType.lowercase().contains("image")) MediaType.Image
                    else if (mimeType.lowercase().contains("video")) MediaType.Video
                    else MediaType.Section

                if (type == MediaType.Section) {
                    return@forEach
                }

                val decryptedBytes =
                    run {
                        val iv = dao.getIvFromSecuredPath(file.absolutePath)
                        val thumbnailIv = dao.getIvFromSecuredPath(
                            securedPath = getSecuredCacheImageForFile(file = file, context = context).absolutePath
                        )

                        if (iv != null && thumbnailIv != null) iv + thumbnailIv else null
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
                    ).toString(),
                    mimeType = mimeType,
                    dateModified = file.lastModified() / 1000,
                    dateTaken = file.lastModified() / 1000,
                    displayName = file.name,
                    absolutePath = file.absolutePath,
                    parentPath = file.absolutePath.parent(),
                    size = 0L,
                    immichUrl = null, // TODO
                    immichThumbnail = null,
                    hash = null,
                    customId = null,
                    favourited = false
                )

                val securedItem = PhotoLibraryUIModel.SecuredMedia(
                    item = item,
                    accessToken = accessToken,
                    bytes = decryptedBytes?.plus(originalPath.encodeToByteArray())
                )

                mediaStoreData.add(securedItem)
            }

            params.value = params.value.copy(securedItems = mediaStoreData)
        }

        return job!!
    }
}