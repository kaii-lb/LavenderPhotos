package com.kaii.photos.file_management.managers

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.PhotosApplication
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskItem
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.exifDataToMediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.toActivity
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.toContentId
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.UriAssetSource
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetBulkUploadCheckItem
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetMediaCreateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import java.io.File
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface GenericFileManager {
    companion object {
        private val TAG = GenericFileManager::class.qualifiedName
    }

    interface Action {
        data class Copy(
            val list: List<SelectionManager.SelectedItem>,
            val destination: AlbumType
        ) : Action

        data class Move(
            val list: List<SelectionManager.SelectedItem>,
            val origin: AlbumType,
            val destination: AlbumType
        ) : Action

        data class Trash(
            val list: List<SelectionManager.SelectedItem>,
            val trashed: Boolean
        ) : Action

        data class Delete(
            val list: List<SelectionManager.SelectedItem>
        ) : Action

        data class Favourite(
            val list: List<SelectionManager.SelectedItem>,
            val favourite: Boolean
        ) : Action

        data class RenameItem(
            val uri: String,
            val newName: String
        ) : Action

        data class RenameAlbum(
            val newName: String
        ) : Action

        data class Share(
            val list: List<SelectionManager.SelectedItem>
        ) : Action

        data class Secure(
            val list: List<SelectionManager.SelectedItem>
        ) : Action

        data class Restore(
            val list: List<SelectionManager.SelectedItem>
        ) : Action
    }

    data class CopyResult(
        val id: Long,
        val immichId: String?
    )

    val mediaDao: MediaDao
    val customDao: CustomEntityDao
    val syncTaskDao: SyncTaskDao
    val assetClient: AssetsClient
    val albumsClient: AlbumsClient

    fun setEndpoint(endpoint: String) {
        albumsClient.setEndpoint(endpoint)
        assetClient.setEndpoint(endpoint)
    }

    fun setAuth(auth: Auth) {
        albumsClient.setAuth(auth)
        assetClient.setAuth(auth)
    }

    fun allowedAlbumTypesFor(
        moving: Boolean,
        current: KClass<out AlbumType>
    ): List<KClass<out AlbumType>> {
        return if (!moving) {
            listOf(
                AlbumType.Folder::class,
                AlbumType.Custom::class,
                AlbumType.Cloud::class
            )
        } else {
            listOf(current)
        }
    }

    suspend fun getShareItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = list

    suspend fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        val items = getShareItems(context, list)

        if (items.size == 1) {
            val item = items.first()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = if (item.isImage) "image/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, item.uri.toUri())
            }

            val chooserIntent = Intent.createChooser(shareIntent, null)
            context.startActivity(chooserIntent)
        } else {
            val hasVideos = items.any { !it.isImage }
            val intent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = if (hasVideos) "video/*" else "image/*"
            }

            val fileUris = ArrayList<Uri>()
            items.forEach {
                fileUris.add(it.uri.toUri())
            }

            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int? = null
    ): PendingIntent? =
        if (list.isEmpty()) null
        else MediaStore.createFavoriteRequest(
            context.contentResolver,
            list.fastMap { it.uri.toUri() },
            favourite
        )

    @OptIn(ExperimentalUuidApi::class)
    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        albumId: String?,
        immichId: String?,
        taskId: Int? = null,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean

    suspend fun permanentlyDelete(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        taskId: Int? = null
    ): Boolean {
        if (list.isNotEmpty()) {
            val deleteRequest = MediaStore.createDeleteRequest(
                context.contentResolver,
                list.map { it.uri.toUri() }
            )

            context.toActivity()?.startIntentSenderForResult(
                deleteRequest.intentSender,
                9997,
                null,
                0,
                0,
                0
            )
        }

        return true
    }

    /** returns null if the operation succeeded, otherwise lets the caller handle the [RecoverableSecurityException] */
    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        try {
            contentResolver.update(uri.toUri(), contentValues, null)
            contentResolver.notifyChange(uri.toUri(), null)

            return null
        } catch (securityException: SecurityException) {
            Log.e(TAG, securityException.toString())
            securityException.printStackTrace()

            val recoverableSecurityException =
                securityException as? RecoverableSecurityException ?: throw RuntimeException(securityException.message, securityException)

            val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
            return intentSender
        }
    }

    suspend fun renameAlbum(
        context: Context,
        album: AlbumType,
        newName: String,
        taskId: Int? = null
    ) {
        val settings = PhotosApplication.appModule.settings.albums

        settings.edit(
            id = album.id,
            newInfo = when (album) {
                is AlbumType.Cloud -> album.copy(name = newName)
                is AlbumType.Custom -> album.copy(name = newName)
                is AlbumType.Folder -> album.copy(name = newName)
                AlbumType.PlaceHolder -> throw IllegalArgumentException("Physically cannot rename ${AlbumType.PlaceHolder::class.name}")
            }
        )
    }

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) =
        if (media.isCloud) {
            val is24Hr = DateFormat.is24HourFormat(context)

            customDao.getExifData(id = media.id)?.let { exifData ->
                exifDataToMediaData(
                    name = media.displayName,
                    path = media.uri,
                    info = exifData,
                    is24Hr = is24Hr,
                    fallback = media.dateTaken
                )
            } ?: run {
                val formattedDateTime =
                    Instant.fromEpochSeconds(media.dateTaken)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toJavaLocalDateTime()
                        .format(
                            DateTimeFormatter.ofPattern(
                                if (is24Hr) "EEE dd MMM yyyy - HH:mm:ss"
                                else "EEE dd MMM yyyy - h:mm:ss a"
                            )
                        )

                mapOf(
                    MediaData.Name to media.displayName,
                    MediaData.Path to media.uri,
                    MediaData.Size to media.size.toString(),
                    MediaData.Date to formattedDateTime
                )
            }
        } else {
            getExifDataForMedia(
                inputStream =
                    context.contentResolver.openInputStream(media.uri.toUri())
                        ?: File(media.absolutePath).inputStream(),
                absolutePath = media.absolutePath,
                is24Hr = DateFormat.is24HourFormat(context),
                fallback = media.dateTaken
            )
        }

    suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean

    suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean

    suspend fun moveItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        taskId: Int? = null,
        origin: AlbumType? = null,
        onItemDone: (uri: String) -> Unit
    ): Boolean

    /** @param overrideDisplayName should not contain file extension */
    suspend fun copyItems(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)? = null,
        taskId: Int? = null,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult>

    suspend fun copyToCustom(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Custom,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        customDao.upsertAll(
            items = list.fastMap {
                CustomItem(
                    id = it.id,
                    album = destination.id
                )
            }
        )

        val ids = list.fastMap { it.id }
        val mediaItems = customDao.getMediaInAlbum(album = destination.id).filter {
            it.id in ids
        }

        mediaItems.forEach { onItemDone(it.uri) }

        launch {
            delay(5000.milliseconds)
            if (destination.immichId != null) {
                CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.id)
            }
        }

        return@withContext mediaItems.fastMap {
            CopyResult(
                id = it.id,
                immichId = it.immichId
            )
        }
    }

    /** @param onItemDone gives the uri of the current item copied */
    suspend fun copyToLocal(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Folder,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (uri: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        val items = getMediaStoreDataForIds(
            ids = list.fastMap { it.id }.toSet(),
            context = context
        )

        val newItems = mutableListOf<CopyResult>()
        items.forEachIndexed { index, media ->
            destination.paths.forEach { path ->
                contentResolver.insertMedia(
                    context = context,
                    media = media,
                    destination = path,
                    overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    preserveDate = preserveDate,
                    onInsert = { original, new ->
                        contentResolver.copyUriToUri(original, new)

                        new.toContentId(contentResolver = contentResolver, type = media.type)?.let {
                            newItems.add(
                                CopyResult(
                                    id = it,
                                    immichId = media.immichId
                                )
                            )
                        }
                    }
                )
            }

            onItemDone(media.uri)
        }

        launch {
            delay(5000.milliseconds)
            if (destination.immichId != null) {
                CloudSyncWorker.immediateEnqueue(context = context, albumId = destination.id)
            }
        }

        return@withContext newItems.toList()
    }

    /** @param destination is the immich album id */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun copyToCloud(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType.Cloud,
        taskId: Int? = null,
        onItemDone: (uri: String) -> Unit
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        val ids = list.fastMap { it.id }.toSet()
        val media = getMediaStoreDataForIds(
            ids = ids,
            context = context
        )

        customDao.upsertAll(
            items = media.map {
                CustomItem(
                    id = it.id,
                    album = destination.id
                )
            }
        )

        val hashes = media.associate { item ->
            val hash = item.hash ?: calculateSha1Checksum(file = File(item.absolutePath))

            item.id to hash
        }

        val bulkCheck = assetClient.check(
            assets = AssetBulkUploadCheckDto(
                media.map { item ->
                    AssetBulkUploadCheckItem(
                        checksum = hashes[item.id]!!,
                        id = item.id.toString()
                    )
                }
            )
        )?.associateBy { it.id } ?: return@withContext emptyList()

        val taskId = taskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Upload,
                destination = destination.immichId
            )
        ).toInt()

        syncTaskDao.insert(
            media.map { item ->
                SyncTaskItem(
                    mediaId = item.id,
                    taskId = taskId
                )
            }
        )

        val trashedItems = mutableListOf<Uuid>()

        val total = media.map { mediaItem ->
            val bulkResponse = bulkCheck[mediaItem.id.toString()]

            if (bulkResponse?.assetId != null) {
                // TODO: progressManager.increaseProgress()

                mediaDao.linkToImmich(
                    id = mediaItem.id,
                    hash = hashes[mediaItem.id]!!,
                    immichUrl = mediaItem.immichUrl ?: "/api/assets/${bulkResponse.assetId}"
                )

                if (bulkResponse.isTrashed) {
                    trashedItems.add(Uuid.parse(bulkResponse.assetId!!))
                }

                CopyResult(
                    id = mediaItem.id,
                    immichId = mediaItem.immichId ?: bulkResponse.assetId
                )
            } else {
                val resp = assetClient.upload(
                    AssetMediaCreateDto(
                        assetSource = UriAssetSource(
                            context = context,
                            uri = mediaItem.uri.toUri()
                        ),
                        fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                        fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                        metadata = emptyList(),
                        filename = mediaItem.displayName
                    )
                )

                if (resp != null) {
                    // TODO: progressManager.increaseProgress()

                    mediaDao.linkToImmich(
                        id = mediaItem.id,
                        hash = hashes[mediaItem.id]!!,
                        immichUrl = "/api/assets/${resp.id}/original"
                    )
                }

                CopyResult(
                    id = mediaItem.id,
                    immichId = resp?.id?.toString()
                )
            }
        }

        assetClient.restore(ids = trashedItems)

        albumsClient.addAssets(
            albumId = Uuid.parse(destination.immichId),
            assetIds = total.fastMap { Uuid.parse(it.immichId!!) }
        ).let { success ->
            syncTaskDao.updateTaskStatus(
                id = taskId,
                status =
                    if (success) SyncTaskStatus.Synced
                    else SyncTaskStatus.Waiting
            )
        }

        return@withContext total
    }
}