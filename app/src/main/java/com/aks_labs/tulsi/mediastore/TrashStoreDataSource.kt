package com.aks_labs.tulsi.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.aks_labs.tulsi.MainActivity
import com.aks_labs.tulsi.database.entities.MediaEntity
import com.aks_labs.tulsi.helpers.MediaItemSortMode
import com.aks_labs.tulsi.helpers.getDateTakenForMedia
import com.aks_labs.tulsi.models.multi_album.groupPhotosBy

/** Loads metadata from the media store for images and videos. */
class TrashStoreDataSource(
    context: Context,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal
) : MediaStoreDataSource(
    context, "trash", sortBy, cancellationSignal
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaStore.Images.Media.DATA,
				MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.IS_TRASHED
            )
    }

    override fun query(): List<MediaStoreData> {
        val database = MainActivity.applicationDatabase
        val mediaEntityDao = database.mediaEntityDao()

        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )
        val data: MutableList<MediaStoreData> = emptyList<MediaStoreData>().toMutableList()

        val bundle = Bundle()
        bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        bundle.putString(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            "(${MediaColumns.IS_TRASHED} = 1 AND ((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})))"
        )

        val mediaCursor = context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            PROJECTION,
            bundle,
            null
        ) ?: return data

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)

                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
                val dateTaken =
	                if (mediaStoreDateTaken == 0L) {
	                	if (dateModified != 0L) {
	                		dateModified
	                	} else {
	                		val possibleDateTaken = mediaEntityDao.getDateTaken(id)

	                		if (possibleDateTaken != 0L) {
	                			possibleDateTaken
                   			} else {
                                val taken = getDateTakenForMedia(absolutePath)

                                mediaEntityDao.insertEntity(
                                    MediaEntity(
                                        id = id,
                                        mimeType = mimeType,
                                        dateTaken = taken,
                                        displayName = displayName
                                    )
                                )
                                taken
	                    	}
	                	}
	                } else {
	                	mediaStoreDateTaken
	                }

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

                val uriParentPath =
                    if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                data.add(
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = uri,
                        mimeType = mimeType,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        absolutePath = absolutePath
                    )
                )
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy)
    }
}


