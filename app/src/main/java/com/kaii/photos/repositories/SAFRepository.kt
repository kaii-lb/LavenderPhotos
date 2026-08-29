package com.kaii.photos.repositories

import android.content.Intent
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.data.datasources.SAFDatasource
import com.kaii.photos.database.daos.SAFDao
import com.kaii.photos.database.transactions.TransactionRunner
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.SAFFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SAFRepository(
    private val scope: CoroutineScope,
    private val datasource: SAFDatasource,
    private val fileManager: SAFFileManager,
    private val treeUri: String,
    private val safDao: SAFDao,
    private val transactionRunner: TransactionRunner,
    showNested: Boolean,
    dateFormatter: LocalizedDateFormatter,
    sortMode: Flow<MediaItemSortMode>,
    info: Flow<ImmichBasicInfo>
) : CountAndSize, Share, ExtractExif {
    class Factory @Inject constructor(
        private val safDao: SAFDao,
        private val fileManager: SAFFileManager,
        private val datasourceFactory: SAFDatasource.Factory,
        private val transactionRunner: TransactionRunner,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope,
            album: AlbumType.SAFFolder
        ): SAFRepository =
            SAFRepository(
                scope = scope,
                datasource = datasourceFactory.create(album),
                fileManager = fileManager,
                treeUri = album.base64TreeUri,
                showNested = album.showNested,
                safDao = safDao,
                transactionRunner = transactionRunner,
                dateFormatter = dateFormatter,
                sortMode = photoGrid.getSortMode(),
                info = immich.getImmichBasicInfo()
            )
    }

    data class QueryParams(
        override val sortMode: MediaItemSortMode,
        override val info: ImmichBasicInfo,
        val showNested: Boolean
    ) : RoomQueryParams(sortMode, info)

    private val showNestedFlow = MutableStateFlow(showNested)

    private val params = combine(info, sortMode, showNestedFlow) { info, sortMode, showNested ->
        QueryParams(
            sortMode = sortMode,
            info = info,
            showNested = showNested
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = QueryParams(
            sortMode = MediaItemSortMode.DateTaken,
            info = ImmichBasicInfo.Empty,
            showNested = false
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (params.showNested) {
                    if (params.sortMode.isDateModified) safDao.getPagedMediaDateModifiedByPathPrefix(treeUri)
                    else safDao.getPagedMediaDateTakenByPathPrefix(treeUri)
                } else {
                    if (params.sortMode.isDateModified) safDao.getPagedMediaDateModified(treeUri)
                    else safDao.getPagedMediaDateTaken(treeUri)
                }
            }
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

    suspend fun refresh() = withContext(Dispatchers.IO) {
        datasource.fetch().collect { items ->
            val existingIds = safDao.getAllIdsIn(treeUri).toSet()
            val incomingIds = items.map { it.id }.toSet()
            val deleted = existingIds - incomingIds

            transactionRunner.run {
                if (items.isNotEmpty()) {
                    items.chunked(500).forEach { chunk ->
                        safDao.upsertAll(items = chunk)
                    }
                }

                if (deleted.isNotEmpty()) {
                    deleted.chunked(500).forEach { chunk ->
                        safDao.deleteAll(ids = chunk)
                    }
                }
            }
        }
    }

    init {
        scope.launch {
            delay(5.seconds)

            while (true) {
                refresh()
                delay(15.seconds)
            }
        }
    }

    fun updateShowNested(value: Boolean) {
        showNestedFlow.value = value
    }

    suspend fun mediaInDateRange(
        timestamp: Long,
        treeUri: String,
        dateModified: Boolean
    ) = withContext(Dispatchers.IO) {
        if (showNestedFlow.value) safDao.mediaInDateRangeNested(timestamp, treeUri, dateModified)
        else safDao.mediaInDateRange(timestamp, treeUri, dateModified)
    }

    suspend fun deleteAlbum() = withContext(Dispatchers.IO) {
        safDao.getAllIdsIn(treeUri).chunked(500).forEach { ids ->
            safDao.deleteAll(ids)
        }
    }

    override suspend fun getMediaCount(album: AlbumType) = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType) = fileManager.getMediaSize(album)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = fileManager.shareFiles(files)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ) = fileManager.getExifData(file)
}