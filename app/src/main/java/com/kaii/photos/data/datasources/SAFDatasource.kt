package com.kaii.photos.data.datasources

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileLoadError
import com.kaii.photos.mediastore.MediaType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.io.encoding.Base64

class SAFDatasource @AssistedInject constructor(
    @param:ApplicationContext private val context: Context,
    @Assisted private val album: AlbumType.SAFFolder
) {
    @AssistedFactory
    interface Factory {
        fun create(album: AlbumType.SAFFolder): SAFDatasource
    }

    suspend fun fetch(): Result<List<MediaStoreData>, FileLoadError> = withContext(Dispatchers.IO) {
        try {
            val treeUri = Base64.decode(album.base64TreeUri).decodeToString().toUri()
            val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

            val mediaItems = mutableListOf<MediaStoreData>()

            collectMedia(
                treeUri = treeUri,
                documentId = rootDocumentId,
                parentPath = album.base64TreeUri,
                into = mediaItems
            )

            Result.Success(data = mediaItems)
        } catch (e: Throwable) {
            Log.e(
                SAFDatasource::class.qualifiedName,
                "Failed to load SAF folder!",
                e
            )

            Result.Error(FileLoadError)
        }
    }

    private fun collectMedia(
        treeUri: Uri,
        documentId: String,
        parentPath: String,
        into: MutableList<MediaStoreData>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        context.contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idCol)
                val mimeType = cursor.getString(mimeCol)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectMedia(
                        treeUri = treeUri,
                        documentId = id,
                        parentPath = "$parentPath/$id",
                        into = into
                    )

                    continue
                }

                if (mimeType == null || (!mimeType.startsWith("image/") && !mimeType.startsWith("video/"))) continue

                val displayName = cursor.getString(nameCol)
                val size = cursor.getLong(sizeCol)
                val dateModified = cursor.getLong(dateModifiedCol) / 1000
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)

                into.add(
                    MediaStoreData(
                        id = stableId(treeUri, id),
                        uri = fileUri.toString(),
                        absolutePath = id,
                        parentPath = parentPath,
                        displayName = displayName,
                        dateTaken = dateModified,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        type = if (mimeType.startsWith("image/")) MediaType.Image else MediaType.Video,
                        immichUrl = null,
                        hash = null,
                        size = size,
                        favourited = false,
                        duration = null,
                        isSAF = true
                    )
                )
            }
        }
    }

    private fun stableId(treeUri: Uri, documentId: String): Long {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$treeUri|$documentId".toByteArray())

        val hash = ByteBuffer.wrap(digest).long
        return hash or Long.MIN_VALUE // force sign bit to be negative for impossible collisions with mediastore ids
    }
}