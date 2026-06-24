package com.kaii.photos.database.migrations

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.mediastore.toMediaType

class Migration3to4(val context: Context) : Migration(startVersion = 3, endVersion = 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val favItems = db.query("SELECT * FROM favouriteditementity")
        val newFavItems = emptyList<MediaStoreData>().toMutableList()
        val metadataRetriever = MediaMetadataRetriever()

        val idNum = favItems.getColumnIndexOrThrow("id")
        val dateTakenNum = favItems.getColumnIndexOrThrow("date_taken")
        val mimeTypeNum = favItems.getColumnIndexOrThrow("mime_type")
        val displayNameNum = favItems.getColumnIndexOrThrow("display_name")
        val absolutePathNum = favItems.getColumnIndexOrThrow("absolute_path")
        val typeNum = favItems.getColumnIndexOrThrow("type")
        val dateModifiedNum = favItems.getColumnIndexOrThrow("date_modified")

        while (favItems.moveToNext()) {
            val id = favItems.getLong(idNum)
            val dateTaken = favItems.getLong(dateTakenNum)
            val mimeType = favItems.getString(mimeTypeNum)
            val displayName = favItems.getString(displayNameNum)
            val absolutePath = favItems.getString(absolutePathNum)
            val type = favItems.getString(typeNum).toMediaType()
            val dateModified = favItems.getLong(dateModifiedNum)

            val uri = context.contentResolver.getUriFromAbsolutePath(absolutePath = absolutePath, type = type).toString()

            val duration =
                if (type == MediaType.Video) {
                    // thanks to IvanCarapovic
                    // https://github.com/IvanCarapovic/LavenderPhotos/blob/22494d0684ce3dc6f7b6f01ee0a8f41f31787dcd/app/src/main/java/com/kaii/photos/compose/grids/PhotoGridView.kt#L517
                    metadataRetriever.setDataSource(context, uri.toUri())
                    metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } else null

            newFavItems.add(
                MediaStoreData(
                    id = id,
                    dateTaken = dateTaken,
                    mimeType = mimeType,
                    displayName = displayName,
                    absolutePath = absolutePath,
                    type = type,
                    dateModified = dateModified,
                    uri = uri,
                    hash = null,
                    immichUrl = null,
                    favourited = true,
                    parentPath = "",
                    size = 0L,
                    duration = duration
                )
            )
        }

        db.execSQL("DROP TABLE favouriteditementity")
        db.execSQL("CREATE TABLE `favouriteditementity` (`id` INTEGER NOT NULL, `date_taken` INTEGER NOT NULL, `mime_type` TEXT NOT NULL, `display_name` TEXT NOT NULL, `absolute_path` TEXT NOT NULL, `type` TEXT NOT NULL, `date_modified` INTEGER NOT NULL, `uri` TEXT NOT NULL, PRIMARY KEY(`id`))")

        metadataRetriever.close()

        newFavItems.forEach { item ->
            db.insert(
                "favouriteditementity",
                OnConflictStrategy.REPLACE,
                ContentValues().apply {
                    put("id", item.id)
                    put("date_taken", item.dateTaken)
                    put("mime_type", item.mimeType)
                    put("display_name", item.displayName)
                    put("absolute_path", item.absolutePath)
                    put("type", item.type.toString())
                    put("date_modified", item.dateModified)
                    put("uri", item.uri)
                }
            )
        }
    }
}