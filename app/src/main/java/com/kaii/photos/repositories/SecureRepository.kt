package com.kaii.photos.repositories

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.FileObserver
import android.util.Log
import androidx.core.content.FileProvider
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.bumptech.glide.Glide
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.SecureFileManager
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Restore
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.SecureIvRecovery
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.paging.SecuredListPagingSource
import com.kaii.photos.helpers.paging.mapToSecuredMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.helpers.secureVideoThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.toSelectedItem
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.TimeZone
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.math.roundToLong

class SecureRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val fileManager: SecureFileManager,
    private val secureDao: SecuredMediaItemEntityDao,
    dateFormatter: LocalizedDateFormatter,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>
) : Delete, Share, ExtractExif, Restore {
    class Factory @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val secureDao: SecuredMediaItemEntityDao,
        private val fileManager: SecureFileManager,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope
        ): SecureRepository =
            SecureRepository(
                context = context,
                secureDao = secureDao,
                scope = scope,
                fileManager = fileManager,
                dateFormatter = dateFormatter,
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode()
            )
    }

    companion object {
        private val TAG = SecureRepository::class.qualifiedName

        suspend fun addEncryptedThumbnail(
            context: Context,
            thumbnail: Bitmap,
            file: File,
            dao: SecuredMediaItemEntityDao
        ) {
            val byteOutputStream = ByteArrayOutputStream()
            thumbnail.compress(
                Bitmap.CompressFormat.PNG,
                100,
                byteOutputStream
            )

            val (encrypted, thumbnailIv) = EncryptionManager.encryptBytes(byteOutputStream.toByteArray())

            val secureThumbnail = file.secureThumbnailImage(context)

            // delete old IVs since we are adding a new one
            dao.deleteEntityBySecuredPath(secureThumbnail.absolutePath)

            try {
                // use{} flushes and closes the stream; the old bare .let{} leaked the fd and a
                // never-flushed buffered stream could drop the trailing bytes. keep the size split:
                // some devices wouldn't write images under 8kb when going through a buffered stream
                secureThumbnail.outputStream().use { raw ->
                    if (encrypted.size <= 8 * 1024) raw.write(encrypted)
                    else raw.buffered().use { it.write(encrypted) }
                }
            } catch (e: IOException) {
                // bail before inserting the row: a row pointing at a missing/truncated thumbnail
                // passes verifyThumbnails (row != null && file.exists()) check and decodes to garbage
                Log.d(TAG, e.toString())
                e.printStackTrace()
                return
            }

            dao.insertEntity(
                SecuredItemEntity(
                    originalPath = file.absolutePath,
                    securedPath = secureThumbnail.absolutePath,
                    iv = thumbnailIv
                )
            )
        }
    }

    private data class Params(
        val items: List<PhotoLibraryUIModel.SecuredMedia>,
        override val sortMode: MediaItemSortMode,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, info)

    private val appContext = context.applicationContext
    private val secureFolder = File(appContext.appSecureFolderDir)
    private val timeZone = TimeZone.getDefault()

    private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(2)
    private val _fileList = MutableStateFlow(secureFolder.listFiles())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _hasItems = MutableStateFlow(false)
    val hasItems = _hasItems.asStateFlow()

    private val fileObserver =
        object : FileObserver(File(appContext.appSecureFolderDir), CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
            init {
                _fileList.value = secureFolder.listFiles()
                _hasItems.value = _fileList.value!!.isNotEmpty()
            }

            override fun onEvent(event: Int, path: String?) {
                // doesn't matter what event type just refresh
                _fileList.value = secureFolder.listFiles()
            }
        }

    private val items = MutableStateFlow(emptyList<PhotoLibraryUIModel.SecuredMedia>())
    private val params = combine(info, sortMode, items) { info, sortMode, items ->
        Params(
            items = items,
            sortMode = sortMode,
            info = info
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Params(
            items = emptyList(),
            sortMode = MediaItemSortMode.DateTaken,
            info = ImmichBasicInfo.Empty
        )
    )

    init {
        scope.launch {
            _fileList.collect {
                Log.d(TAG, "REQUESTING LOAD")
                _isLoading.value = true
                load(appContext)
                _isLoading.value = false
            }
        }

        scope.launch {
            verifyThumbnails(appContext)
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
        ).flow.mapToSecuredMedia(
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

    fun attachFileObserver() {
        fileObserver.startWatching()
    }

    fun detachFileObserver() {
        fileObserver.stopWatching()
    }

    fun getItemsForDate(
        timestamp: Long,
        sortMode: MediaItemSortMode
    ) =
        items.value.filter { media ->
            val item = media.item
            val key = when {
                sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken(timeZone)
                sortMode.isDateModified -> item.getDateModifiedDay(timeZone)
                else -> item.getDateTakenDay(timeZone)
            }

            key in timestamp..(timestamp + 86400)
        }.associate { media ->
            media.item.id to media.item.toSelectedItem()
        }.toMap()

    private suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val snapshot = _fileList.value?.sortedBy { it.lastModified() } ?: return@withContext

        val mediaStoreData = items.value.toMutableList()

        snapshot.forEach { file ->
            // self-healing skip: only keep an already-processed item if its cached thumbnail iv still
            // matches the db. if not (it was the zero not-ready sentinel, or the thumbnail was rebuilt
            // after a cacheDir eviction with a fresh iv) drop it and rebuild below. previously this
            // unconditionally skipped, freezing a broken thumbnail until app restart
            val existingIndex = mediaStoreData.indexOfFirst { it.item.absolutePath == file.absolutePath }
            if (existingIndex != -1) {
                val dbThumbnailIv = secureDao.getIvFromSecuredPath(file.secureThumbnailImage(context).absolutePath)
                val cachedThumbnailIv = mediaStoreData[existingIndex].bytes
                    ?.takeIf { it.size >= 32 }
                    ?.getThumbnailIv()
                val cachedIsNotReady = cachedThumbnailIv != null && cachedThumbnailIv.all { it.toInt() == 0 }
                val healthy =
                    if (dbThumbnailIv == null) {
                        // no thumbnail yet: keep the not-ready row as-is so we don't re-probe the item on
                        // every FileObserver event during a batch. verifyThumbnails will generate it and
                        // reload, after which dbThumbnailIv is non-null and the else branch rebuilds it
                        cachedIsNotReady
                    } else {
                        cachedThumbnailIv != null && dbThumbnailIv.contentEquals(cachedThumbnailIv)
                    }

                if (healthy) return@forEach
                mediaStoreData.removeAt(existingIndex)
            }

            val mimeType = Files.probeContentType(Path(file.absolutePath))

            val type =
                if (mimeType.lowercase().contains("image")) MediaType.Image
                else if (mimeType.lowercase().contains("video")) MediaType.Video
                else return@forEach

            val decryptedBytes = run {
                val iv = secureDao.getIvFromSecuredPath(file.absolutePath)
                val thumbnailIv = secureDao.getIvFromSecuredPath(file.secureThumbnailImage(context).absolutePath)

                // pad corrupted/short ivs to 16 zero bytes so the [fileIv(16)][thumbnailIv(16)][path]
                // layout holds and getThumbnailIv() doesn't read into the path bytes; recover first
                val fileIv =
                    if (iv != null && iv.size == 16) {
                        // a 16-byte iv can still be wrong (decrypts to garbage); re-check it against the
                        // format magic and re-recover if bad rather than trusting size==16 forever
                        if (SecureIvRecovery.ivProducesValidHeader(file, iv, mimeType)) iv
                        else SecureIvRecovery.recoverAndPersist(context, file, mimeType, secureDao) ?: iv
                    } else if (iv != null) SecureIvRecovery.recoverAndPersist(context, file, mimeType, secureDao) ?: ByteArray(16)
                    else ByteArray(16)
                fileIv + (thumbnailIv ?: ByteArray(16))
            }

            val originalPath =
                secureDao.getOriginalPathFromSecuredPath(file.absolutePath) ?: context.appRestoredFilesDir

            val duration = if (type == MediaType.Video) {
                secureDao.getDuration(file.absolutePath)
            } else null

            Log.d(TAG, "DURATION $duration")

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
                parentPath = originalPath,
                size = 0L,
                immichUrl = null,
                hash = null,
                favourited = false,
                duration = duration
            )

            val securedItem = PhotoLibraryUIModel.SecuredMedia(
                item = item,
                auth = params.value.info.auth,
                endpoint = params.value.info.endpoint,
                bytes = decryptedBytes.plus(originalPath.encodeToByteArray())
            )

            mediaStoreData.add(securedItem)
        }

        val presentPaths = snapshot.map { it.absolutePath }
        items.value =
            mediaStoreData
                .filter { media ->
                    media.item.absolutePath in presentPaths
                }.sortedByDescending {
                    it.item.dateModified
                }
    }

    private suspend fun verifyThumbnails(context: Context) = withContext(thumbnailDispatcher) {
        val snapshot = _fileList.value ?: return@withContext

        var generatedAny = false

        snapshot.forEach { file ->
            val thumbnail = file.secureThumbnailImage(context)
            val mimeType = Files.probeContentType(Path(file.absolutePath))
            val type =
                if (mimeType.lowercase().contains("image")) MediaType.Image
                else if (mimeType.lowercase().contains("video")) MediaType.Video
                else return@forEach

            val isVideoDurationValid = type == MediaType.Video && secureDao.getDuration(file.absolutePath) != null

            // regenerate if the iv row is missing OR the cached png is gone. the thumbnail cache lives
            // in cacheDir, which the OS can purge under storage pressure, leaving a dangling iv row that
            // would otherwise never be rebuilt
            if (secureDao.getIvFromSecuredPath(thumbnail.absolutePath) != null &&
                thumbnail.exists() &&
                (type == MediaType.Image || isVideoDurationValid)
            ) return@forEach


            if (type == MediaType.Image) {
                Log.d(TAG, "updating image")
                addImageThumbnail(file, context)
            } else {
                Log.d(TAG, "updating video")
                addVideoThumbnail(file, context)
            }

            generatedAny = true
        }

        // refresh the listing so items holding a not-ready (zero) iv pick up their real iv via the
        // self-healing skip in load(). single coalesced reload, not one per file
        if (generatedAny) load(context)
    }

    private suspend fun addImageThumbnail(
        file: File,
        context: Context
    ) = withContext(Dispatchers.IO) {
        var iv = secureDao.getIvFromSecuredPath(file.absolutePath) ?: return@withContext

        // recover a corrupted iv (ByteArray(0) from a failed-secure catch block) before decoding
        if (iv.size != 16) {
            val mimeType = Files.probeContentType(Path(file.absolutePath))
            iv = SecureIvRecovery.recoverAndPersist(context, file, mimeType, secureDao) ?: run {
                Log.e(TAG, "Cannot generate thumbnail for ${file.name}: iv unrecoverable")
                return@withContext
            }
        }

        val bytes = EncryptionManager.decryptBytes(
            bytes = file.readBytes(),
            iv = iv
        )

        try {
            val thumbnail = Glide
                .with(context)
                .asBitmap()
                .load(bytes)
                .override(512)
                .submit()
                .get()

            addEncryptedThumbnail(context, thumbnail, file, secureDao)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to add thumbnail ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Regenerate a secure video's thumbnail. The original source is already deleted by securing time,
     * so the frame must come from the encrypted copy: decrypt it to the video cache, grab a frame,
     * re-encrypt the thumbnail. Mirrors the creation path in [com.kaii.photos.file_management.secure.LocalSecureManager.secure].
     */
    private suspend fun addVideoThumbnail(
        file: File,
        context: Context
    ) = withContext(Dispatchers.IO) {
        var iv = secureDao.getIvFromSecuredPath(file.absolutePath) ?: return@withContext

        if (iv.size != 16) {
            val mimeType = Files.probeContentType(Path(file.absolutePath))
            iv = SecureIvRecovery.recoverAndPersist(context, file, mimeType, secureDao) ?: run {
                Log.e(TAG, "Cannot generate video thumbnail for ${file.name}: iv unrecoverable")
                return@withContext
            }
        }

        val decrypted = try {
            EncryptionManager.decryptVideo(file.absolutePath, iv, context) {}
        } catch (e: Throwable) {
            Log.e(TAG, "Cannot decrypt ${file.name} for thumbnail", e)
            return@withContext
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(decrypted.absolutePath)

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.let {
                    secureDao.updateDuration(file.absolutePath, (it / 1000.0).roundToLong())
                }

            retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST, 1024, 1024)
                ?.let { bitmap ->
                    addEncryptedThumbnail(context, bitmap, file.secureVideoThumbnailImage(context), secureDao)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Cannot extract frame from ${file.name}", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
            decrypted.delete()
        }
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ) = fileManager.deleteFiles(files, albumId, immichId)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.shareFiles(files)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ) = fileManager.getExifData(file)

    override suspend fun decryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.decryptFiles(files)

    override suspend fun clearCaches() = fileManager.clearCaches()
}