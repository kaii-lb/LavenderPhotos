package com.kaii.photos.database.migrations

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.net.toUri
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaii.photos.mediastore.LavenderContentProvider
import com.kaii.photos.mediastore.LavenderMediaColumns

class Migration9To10(val context: Context) : Migration(startVersion = 9, endVersion = 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = context.contentResolver.query(
            LavenderContentProvider.CONTENT_URI,
            arrayOf(
                LavenderMediaColumns.URI,
                LavenderMediaColumns.PARENT_ID
            ),
            null,
            null
        ) ?: return

        val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
        val parentCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.PARENT_ID)

        while (cursor.moveToNext()) {
            val uri = cursor.getString(uriCol)
            val parent = cursor.getInt(parentCol)

            db.insert(
                table = "custom_media",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                values = ContentValues().apply {
                    put("mediaId", uri.toUri().lastPathSegment!!.toLong())
                    put("album", parent)
                }
            )
        }

        cursor.close()
    }
}