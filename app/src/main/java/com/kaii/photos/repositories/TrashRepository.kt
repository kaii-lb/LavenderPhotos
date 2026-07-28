package com.kaii.photos.repositories

import android.content.Intent
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.TrashDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TrashRepository(
    private val fileManager: LocalFileManager,
    private val dataSource: TrashDataSource,
    scope: CoroutineScope,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) : Trash, Delete, Share, ExtractExif, RenameFile {
    class Factory @Inject constructor(
        private val fileManager: LocalFileManager,
        private val dataSource: TrashDataSource,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl,
        private val lookAndFeel: SettingsLookAndFeelImpl
    ) {
        fun create(
            scope: CoroutineScope
        ): TrashRepository =
            TrashRepository(
                fileManager = fileManager,
                dataSource = dataSource,
                scope = scope,
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode(),
                format = lookAndFeel.getDisplayDateFormat()
            )
    }

    private data class Params(
        val items: List<MediaStoreData>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private fun getMediaDataFlow() = dataSource.loadMediaStoreData().flowOn(Dispatchers.IO)

    private val items = MutableStateFlow(emptyList<MediaStoreData>())

    private val params = combine(info, sortMode, format, items) { info, sortMode, format, items ->
        Params(
            items = items,
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    init {
        scope.launch(Dispatchers.IO) {
            getMediaDataFlow().collectLatest { media ->
                items.value = media
            }
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
            pagingSourceFactory = { ListPagingSource(media = params.items) }
        ).flow.mapToMedia(
            auth = params.info.auth,
            endpoint = params.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = if (params.sortMode.isDisabled) MediaItemSortMode.DisabledLastModified else MediaItemSortMode.DateModified,
            format = params.format
        )
    }.cachedIn(scope)

    fun cancel() = dataSource.cancel()

    suspend fun deleteAll() = fileManager.deleteFiles(
        files = dataSource.query().fastMap {
            FileOperationItemMetadata(
                id = it.id,
                uri = it.uri,
                immichUrl = it.immichUrl,
                isImage = it.type == MediaType.Image,
                absolutePath = it.absolutePath
            )
        },
        albumId = "",
        existingTaskId = null,
        immichId = null
    )

    suspend fun getItemsForDate(
        timestamp: Long,
        sortMode: MediaItemSortMode
    ) = withContext(Dispatchers.IO) {
        val allTrashItems = dataSource.query()

        allTrashItems.filter { item ->
            val key = when {
                sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken()
                sortMode.isDateModified -> item.getDateModifiedDay()
                else -> item.getDateTakenDay()
            }

            key in timestamp..(timestamp + 86400)
        }.associate { item ->
            item.id to SelectionManager.SelectedItem(
                id = item.id,
                uri = item.uri,
                immichUrl = item.immichUrl,
                isImage = item.type == MediaType.Image,
                absolutePath = item.absolutePath
            )
        }.toMap()
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.deleteFiles(files, albumId, immichId, existingTaskId)

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.trashFiles(files, isTrashed, albumId, immichId, existingTaskId)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> = fileManager.shareFiles(files)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = fileManager.getExifData(file)

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = fileManager.renameFile(file, newName)
}