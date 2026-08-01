package com.kaii.photos.file_management.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Restore
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.helpers.getDecryptCacheForFile
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class SecureFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val secureDao: SecuredMediaItemEntityDao,
    private val gateway: AndroidMediaStoreGateway,
    private val secureManager: LocalSecureManager
) : Delete, Restore, Share, ExtractExif {
    companion object {
        private val TAG = SecureFileManager::class.qualifiedName
    }

    override suspend fun decryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = channelFlow {
        val media = files.toSecureMedia(context = context)
        var result: Result<Unit, FileOperationError> = Result.Success(Unit)

        send(element = FileOperationProgress.Started(
            action = FileOperationAction.LongOperationType.Secure,
            fileCount = files.size
        ))

        media.forEach {
            result = secureManager.restore(media = it)

            send(element = FileOperationProgress.ItemDone(uri = it.item.uri))
        }

        send(
            element = FileOperationProgress.Finished(
                result = result
            )
        )
    }

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> {
        val cachedFiles = emptyList<FileOperationItemMetadata>().toMutableList()
        val items = files.toSecureMedia(context = context)

        items.forEach { item ->
            val iv = item.bytes?.getIv() ?: return Result.Error(FileOperationError.Failed)

            val originalFile = File(item.item.absolutePath)
            val cachedFile =
                if (item.item.type == MediaType.Video) {
                    getSecureDecryptedVideoFile(originalFile.name, context)
                } else {
                    getDecryptCacheForFile(originalFile, context)
                }

            if (!cachedFile.exists()) {
                if (item.item.type == MediaType.Video) {
                    EncryptionManager.decryptVideo(
                        absolutePath = originalFile.absolutePath,
                        context = context,
                        iv = iv,
                        progress = {}
                    )
                } else {
                    EncryptionManager.decryptInputStream(
                        inputStream = originalFile.inputStream(),
                        outputStream = cachedFile.outputStream(),
                        fileSize = originalFile.length(),
                        iv = iv
                    )
                }
            }

            cachedFile.deleteOnExit()
            cachedFiles.add(
                FileOperationItemMetadata(
                    id = item.item.id,
                    uri = FileProvider.getUriForFile(
                        context,
                        LAVENDER_FILE_PROVIDER_AUTHORITY,
                        cachedFile
                    ).toString(),
                    absolutePath = cachedFile.absolutePath,
                    isImage = item.item.type == MediaType.Image,
                    immichUrl = null
                )
            )
        }

        return gateway.share(files = cachedFiles)
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = try {
        files.forEach { item ->
            val file = File(
                context.appSecureFolderDir,
                item.uri.substringAfterLast("/") // filename
            )

            file.delete()
            val thumbnail = file.secureThumbnailImage(context)
            thumbnail.delete()

            secureDao.deleteEntityBySecuredPath(securedPath = file.absolutePath)
            secureDao.deleteEntityBySecuredPath(securedPath = thumbnail.absolutePath)
        }

        Result.Success(Unit)
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        Result.Error(FileOperationError.Failed)
    }

    // TODO: clean this up
    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> {
        val threshold = 500

        val media = listOf(file).toSecureMedia(context = context).firstOrNull() ?: return Result.Error(FileOperationError.Failed)

        val decryptedFile = if (!file.isImage) {
            val originalFile = File(file.absolutePath)
            val cachedFile = getSecureDecryptedVideoFile(
                name = originalFile.name,
                context = context
            )

            if (cachedFile.length() < originalFile.length()) {
                while (cachedFile.length() + threshold < originalFile.length()) {
                    delay(100.milliseconds)
                }

                cachedFile
            } else {
                cachedFile
            }
        } else {
            val originalFile = File(file.absolutePath)
            val cachedFile = getDecryptCacheForFile(
                file = originalFile,
                context = context
            )

            if (!cachedFile.exists()) {
                val iv = media.bytes?.getIv()

                if (iv == null) {
                    Log.e(TAG, "IV for ${media.item.displayName} was null, aborting")
                    return Result.Error(FileOperationError.Failed)
                }

                EncryptionManager.decryptInputStream(
                    inputStream = originalFile.inputStream(),
                    outputStream = cachedFile.outputStream(),
                    fileSize = originalFile.length(),
                    iv = iv
                )

                cachedFile
            } else if (cachedFile.length() < originalFile.length()) {
                while (cachedFile.length() + threshold < originalFile.length()) {
                    delay(100.milliseconds)
                }

                cachedFile
            } else {
                cachedFile
            }
        }

        val mediaData = getExifDataForMedia(
            inputStream = decryptedFile.inputStream(),
            absolutePath = decryptedFile.absolutePath,
            is24Hr = gateway.is24HrFormat(),
            fallback = media.item.dateModified
        ).toMutableMap().apply {
            set(MediaData.Path, media.item.parentPath)
            set(MediaData.Name, media.item.displayName)
        }

        return Result.Success(data = mediaData)
    }
}