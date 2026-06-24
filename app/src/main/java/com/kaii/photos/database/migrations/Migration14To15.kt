package com.kaii.photos.database.migrations

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import kotlinx.coroutines.runBlocking

class Migration14To15(val context: Context) : Migration(startVersion = 14, endVersion = 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media ADD COLUMN 'duration' INTEGER")

        val mediaStoreIds = mutableSetOf<Long>()
        db.query("SELECT id FROM media WHERE uri LIKE 'content://%' AND type = 'Video'").use { cursor ->
            val colIndex = cursor.getColumnIndexOrThrow("id")
            while (cursor.moveToNext()) {
                mediaStoreIds.add(cursor.getLong(colIndex))
            }
        }

        if (mediaStoreIds.isEmpty()) return

        val contentValues = ContentValues()

        val data = runBlocking {
            getMediaStoreDataForIds(ids = mediaStoreIds, context = context)
        }

        data.forEach { item ->
            contentValues.clear()
            contentValues.put("duration", item.duration)

            db.update(
                table = "media",
                conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                values = contentValues,
                whereClause = "id = ?",
                whereArgs = arrayOf(item.id)
            )
        }
    }
}