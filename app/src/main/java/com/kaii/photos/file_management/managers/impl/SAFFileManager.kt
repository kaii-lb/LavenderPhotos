package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.SAFDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.helpers.exif.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.math.round
import kotlin.time.Instant

class SAFFileManager @Inject constructor(
    private val safDao: SAFDao,
    private val mediaStoreGateway: MediaStoreGateway
) : Share, ExtractExif, CountAndSize {
    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = flow {
        // no point in showing a snackbar for an instant operation
        val result = mediaStoreGateway.share(files)

        emit(
            value = FileOperationProgress.Finished(
                result = result
            )
        )
    }

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = withContext(Dispatchers.IO) {
        val media = safDao.getById(file.id) ?: return@withContext Result.Error(FileOperationError.Failed)

        val formattedDateTime =
            Instant.fromEpochSeconds(media.dateTaken)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (mediaStoreGateway.is24HrFormat()) "EEE dd MMM yyyy - HH:mm:ss"
                        else "EEE dd MMM yyyy - h:mm:ss a"
                    )
                )

        val parentPath = Base64.decode(media.parentPath)
            .let {
                URLDecoder.decode(it.decodeToString(), "UTF-8")
            }
            .substringAfterLast("/")
            .substringAfter(":")

        val size = media.size.let { bytes ->
            if (bytes < 1000000) { // less than a mb display in kb
                val kb = round(bytes * 10 / 1000f) / 10
                "$kb KB"
            } else {
                val mb = round(bytes / 100000f) / 10
                "$mb MB"
            }
        }

        Result.Success(
            data = mapOf(
                MediaData.Name to media.displayName,
                MediaData.Path to "${parentPath}/${media.displayName}",
                MediaData.Size to size,
                MediaData.Date to formattedDateTime
            )
        )
    }

    override suspend fun getMediaCount(album: AlbumType): Int = withContext(Dispatchers.IO) {
        val treeUri = (album as AlbumType.SAFFolder).base64TreeUri

        if (album.showNested) safDao.countMediaInPathsPrefixes(treeUri = treeUri)
        else safDao.countMediaInPaths(treeUri = treeUri)
    }

    override suspend fun getMediaSize(album: AlbumType): Long = withContext(Dispatchers.IO) {
        val treeUri = (album as AlbumType.SAFFolder).base64TreeUri

        if (album.showNested) safDao.mediaSizeByPathPrefixes(treeUri = treeUri)
        else safDao.mediaSize(treeUri = treeUri)
    }
}