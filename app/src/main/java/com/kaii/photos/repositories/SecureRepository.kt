package com.kaii.photos.repositories

import android.content.Context
import android.os.FileObserver
import androidx.core.content.FileProvider
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.loading.SecuredListPagingSource
import com.kaii.photos.models.loading.mapToSecuredMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class SecureRepository(
    private val scope: CoroutineScope,
    private val info: ImmichBasicInfo,
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private val appContext = context.applicationContext
    private val dao = MediaDatabase.getInstance(appContext).securedItemEntityDao()

    private data class TrashFlowParams(
        val items: List<PhotoLibraryUIModel.SecuredMedia>,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val accessToken: String
    )

    private val secureFolder = File(appContext.appSecureFolderDir)
    private var job: Job? = null

    private val fileObserver =
        object : FileObserver(File(appContext.appSecureFolderDir), CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                // doesn't matter what event type just refresh
                if (path != null) {
                    _fileList.value = secureFolder.listFiles()

                    load(context = appContext)
                }
            }
        }

    private val _fileList = MutableStateFlow(secureFolder.listFiles())

    private val params = MutableStateFlow(
        TrashFlowParams(
            items = emptyList(),
            sortMode = sortMode,
            format = format,
            accessToken = ""
        )
    )

    init {
        scope.launch(Dispatchers.IO) {
            load(context = context)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { SecuredListPagingSource(media = params.items) }
        ).flow.mapToSecuredMedia(accessToken = params.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = if (params.sortMode.isDisabled) MediaItemSortMode.DisabledLastModified else MediaItemSortMode.DateModified,
            format = params.format
        )
    }.cachedIn(scope)

    fun attachFileObserver() {
        fileObserver.startWatching()
    }

    fun detachFileObserver() {
        fileObserver.stopWatching()
    }

    private fun load(context: Context): Job {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            val snapshot = _fileList.value?.sortedBy { it.lastModified() } ?: return@launch

            val mediaStoreData = params.value.items.toMutableList()

            snapshot.forEach { file ->
                // if file is already processed, skip processing it
                if (mediaStoreData.any { it.item.absolutePath == file.absolutePath }) {
                    return@forEach
                }

                val mimeType = Files.probeContentType(Path(file.absolutePath))

                val type =
                    if (mimeType.lowercase().contains("image")) MediaType.Image
                    else if (mimeType.lowercase().contains("video")) MediaType.Video
                    else return@forEach

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
                    accessToken = info.accessToken,
                    bytes = decryptedBytes?.plus(originalPath.encodeToByteArray())
                )

                mediaStoreData.add(securedItem)
            }

            params.value = params.value.copy(
                items = mediaStoreData.sortedByDescending {
                    it.item.dateModified
                }
            )
        }

        return job!!
    }
}