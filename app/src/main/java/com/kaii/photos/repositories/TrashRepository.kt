package com.kaii.photos.repositories

import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.data.datasources.trash.LocalTrashDataSource
import com.kaii.photos.data.datasources.trash.NetworkTrashDataSource
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.di.HybridFileManagerFactory
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.file_management.managers.traits.ClearExif
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.mediastore.toFileOperationMetadata
import com.kaii.photos.mediastore.toSelectedItem
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject

class TrashRepository(
    private val fileManager: HybridFileManager,
    private val localDataSource: LocalTrashDataSource,
    private val networkDataSource: NetworkTrashDataSource,
    private val scope: CoroutineScope,
    dateFormatter: LocalizedDateFormatter,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>
) : Trash, Delete, Share, ExtractExif, RenameFile, ClearExif {
    class Factory @Inject constructor(
        private val fileManagerFactory: HybridFileManagerFactory,
        private val localFileManager: LocalFileManager,
        private val localDataSource: LocalTrashDataSource,
        private val networkDataSource: NetworkTrashDataSource,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope
        ): TrashRepository =
            TrashRepository(
                fileManager = fileManagerFactory.create(
                    other = localFileManager
                ),
                localDataSource = localDataSource,
                networkDataSource = networkDataSource,
                scope = scope,
                dateFormatter = dateFormatter,
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode()
            )
    }

    private data class Params(
        val items: List<MediaStoreData>,
        override val sortMode: MediaItemSortMode,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, info)

    private val items = MutableStateFlow(emptyList<MediaStoreData>())
    private val timeZone = TimeZone.getDefault()

    private val params = combine(info, sortMode, items) { info, sortMode, items ->
        Params(
            items = items,
            sortMode = sortMode,
            info = info
        )
    }

    init {
        start()
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
            dateFormatter = dateFormatter
        )
    }.cachedIn(scope)

    fun cancel() {
        localDataSource.cancel()
        networkDataSource.cancel()
    }

    fun start() {
        val local = localDataSource.start()
        val network = networkDataSource.start()

        scope.launch {
            combine(local, network) { local, network ->
                Pair(local, network)
            }.flowOn(Dispatchers.IO).collectLatest { (local, network) ->
                items.value = (local + network).sortedByDescending {
                    it.dateModified
                }
            }
        }
    }

    suspend fun getAllFiles() = withContext(Dispatchers.IO) {
        val local = localDataSource.query()
        val network = networkDataSource.query()

        (local + network).fastMap {
            it.toFileOperationMetadata()
        }
    }

    suspend fun getItemsForDate(
        timestamp: Long,
        sortMode: MediaItemSortMode
    ) = withContext(Dispatchers.IO) {
        items.value.filter { item ->
            val key = when {
                sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken(timeZone)
                sortMode.isDateModified -> item.getDateModifiedDay(timeZone)
                else -> item.getDateTakenDay(timeZone)
            }

            key in timestamp..(timestamp + 86400)
        }.associate { item ->
            item.id to item.toSelectedItem()
        }.toMap()
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ) = fileManager.deleteFiles(files, "trash", immichId).onEach { progress ->
        if (progress is FileOperationProgress.Finished && progress.result is Result.Success) {
            val ids = files.fastMap { it.id }

            items.value = items.value.filter {
                it.id !in ids
            }
        }
    }

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?
    ) = fileManager.trashFiles(files, isTrashed, albumId, immichId).onEach { progress ->
        if (progress is FileOperationProgress.Finished && progress.result is Result.Success) {
            val ids = files.fastMap { it.id }

            items.value = items.value.filter {
                it.id !in ids
            }
        }
    }

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.shareFiles(files)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ) = fileManager.getExifData(file)

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ) = fileManager.renameFile(file, newName)

    override suspend fun clearExifData(
        absolutePath: String
    ) = fileManager.clearExifData(absolutePath)
}