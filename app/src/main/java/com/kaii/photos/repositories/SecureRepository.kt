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
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.managers.SecureFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.SecureIvRecovery
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.paging.SecuredListPagingSource
import com.kaii.photos.helpers.paging.mapToSecuredMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.helpers.secureVideoThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getThumbnailIv
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.roundToLong
import kotlin.reflect.KClass

class SecureRepository(
    scope: CoroutineScope,
    context: Context,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo {
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
            try {
                // use{} flushes and closes the stream; the old .let{} leaked the fd and a
                // never-flushed buffered stream could drop the trailing bytes
                secureThumbnail.outputStream().use { it.write(encrypted) }
            } catch (e: IOException) {
                Log.d(TAG, e.toString())
                e.printStackTrace()
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
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private val appContext = context.applicationContext
    private val db = MediaDatabase.getInstance(appContext)
    private val secureDao = db.securedItemEntityDao()

    override val fileManager = SecureFileManager(
        secureDao = secureDao,
        mediaDao = db.mediaDao(),
        customDao = db.customDao(),
        syncTaskDao = db.taskDao(),
        assetClient = AssetsClient(
            endpoint = "",
            auth = Auth.None,
            client = context.appModule.apiClient
        ),
        albumsClient = AlbumsClient(
            endpoint = "",
            auth = Auth.None,
            client = context.appModule.apiClient
        )
    )

    private val secureFolder = File(appContext.appSecureFolderDir)

    private val repoScope = scope

    // load() does a read-modify-write on items.value, so concurrent calls (init, the FileObserver firing
    // once per file during a batch secure, the post-verify reload) used to lost-update each other and
    // freeze a not-ready zero-iv row. serialise load() and coalesce bursts: while one runs, extra
    // requests just set a rerun flag instead of spawning more coroutines
    private val loadMutex = Mutex()
    @Volatile
    private var loadQueued = false

    private fun requestLoad(context: Context) {
        repoScope.launch { runLoadCoalesced(context) }
    }

    private suspend fun runLoadCoalesced(context: Context) {
        loadQueued = true
        if (!loadMutex.tryLock()) return // a load is already active; it will observe loadQueued and rerun
        try {
            while (loadQueued) {
                loadQueued = false
                load(context)
            }
        } finally {
            loadMutex.unlock()
        }
    }

    private val fileObserver =
        object : FileObserver(File(appContext.appSecureFolderDir), CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
            override fun onEvent(event: Int, path: String?) {
                // doesn't matter what event type just refresh
                if (path != null) {
                    _fileList.value = secureFolder.listFiles()
                    requestLoad(appContext)
                }
            }
        }

    private val _fileList = MutableStateFlow(secureFolder.listFiles())

    private val items = MutableStateFlow(emptyList<PhotoLibraryUIModel.SecuredMedia>())
    private val params = combine(info, sortMode, format, items) { info, sortMode, format, items ->
        Params(
            items = items,
            sortMode = sortMode,
            format = format,
            info = info
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = Params(
            items = emptyList(),
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default,
            info = ImmichBasicInfo.Empty
        )
    )

    init {
        scope.launch {
            runLoadCoalesced(context)
        }

        scope.launch {
            verifyThumbnails(context)
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
            format = params.format
        )
    }.cachedIn(scope)

    fun attachFileObserver() {
        fileObserver.startWatching()
    }

    fun detachFileObserver() {
        fileObserver.stopWatching()
    }

    private suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val snapshot = _fileList.value?.sortedBy { it.lastModified() } ?: return@withContext

        val mediaStoreData = items.value.toMutableList()
        val metadataRetriever = MediaMetadataRetriever()

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
                    if (iv != null && iv.size == 16) iv
                    else if (iv != null) SecureIvRecovery.recoverAndPersist(context, file, mimeType, secureDao) ?: ByteArray(16)
                    else ByteArray(16)
                fileIv + (thumbnailIv ?: ByteArray(16))
            }

            val originalPath =
                secureDao.getOriginalPathFromSecuredPath(file.absolutePath) ?: context.appRestoredFilesDir

            val duration = if (type == MediaType.Video) {
                // thanks to IvanCarapovic
                // https://github.com/IvanCarapovic/LavenderPhotos/blob/22494d0684ce3dc6f7b6f01ee0a8f41f31787dcd/app/src/main/java/com/kaii/photos/compose/grids/PhotoGridView.kt#L517
                try {
                    metadataRetriever.setDataSource(file.absolutePath)
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } catch (e: RuntimeException) {
                    Log.e(TAG, e.toString())
                    Log.e(TAG, "The failing file was ${file.absolutePath}")
                    null
                }
            } else null

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
                duration = duration?.let { (it / 1000.0).roundToLong() }
            )

            val securedItem = PhotoLibraryUIModel.SecuredMedia(
                item = item,
                auth = params.value.info.auth,
                endpoint = params.value.info.endpoint,
                bytes = decryptedBytes.plus(originalPath.encodeToByteArray())
            )

            mediaStoreData.add(securedItem)
        }

        metadataRetriever.close()

        val presentPaths = _fileList.value!!.map { it.absolutePath }
        items.value =
            mediaStoreData
                .filter { media ->
                    media.item.absolutePath in presentPaths
                }.sortedByDescending {
                    it.item.dateModified
                }
    }

    private suspend fun verifyThumbnails(context: Context) = withContext(Dispatchers.IO) {
        val snapshot = _fileList.value ?: return@withContext

        var generatedAny = false

        snapshot.forEach { file ->
            val thumbnail = file.secureThumbnailImage(context)

            // regenerate if the iv row is missing OR the cached png is gone. the thumbnail cache lives
            // in cacheDir, which the OS can purge under storage pressure, leaving a dangling iv row that
            // would otherwise never be rebuilt
            if (secureDao.getIvFromSecuredPath(thumbnail.absolutePath) != null && thumbnail.exists()) return@forEach

            val mimeType = Files.probeContentType(Path(file.absolutePath))
            val type =
                if (mimeType.lowercase().contains("image")) MediaType.Image
                else if (mimeType.lowercase().contains("video")) MediaType.Video
                else return@forEach

            if (type == MediaType.Image) {
                addImageThumbnail(file, context)
            } else {
                addImageThumbnail(file.secureVideoThumbnailImage(context), context)
            }
            generatedAny = true
        }

        // refresh the listing so items holding a not-ready (zero) iv pick up their real iv via the
        // self-healing skip in load(). single coalesced reload, not one per file
        if (generatedAny) runLoadCoalesced(context)
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

        val thumbnail = Glide
            .with(context)
            .asBitmap()
            .load(bytes)
            .override(512)
            .submit()
            .get()

        addEncryptedThumbnail(context, thumbnail, file, secureDao)
    }

    override suspend fun getMediaCount(): Int {
        throw NotImplementedError("Cannot get media count in secure folder")
    }

    override suspend fun getMediaSize(): Long {
        throw NotImplementedError("Cannot get media size in secure folder")
    }

    override fun allowedAlbumTypesFor(moving: Boolean): List<KClass<out AlbumType>> {
        throw NotImplementedError("Cannot use this in secure folder")
    }

    override suspend fun renameAlbum(context: Context, newName: String) {
        throw NotImplementedError("Cannot rename the secure folder")
    }

    override suspend fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.permanentlyDelete(context, list)

    override suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean = fileManager.restore(context, list)

    override suspend fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.share(context, list)
}