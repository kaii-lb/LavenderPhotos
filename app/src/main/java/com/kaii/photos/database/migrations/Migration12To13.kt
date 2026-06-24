package com.kaii.photos.database.migrations

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.uuid.Uuid

class Migration12To13 : Migration(startVersion = 12, endVersion = 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT * FROM custom_items")

        val idIndex = cursor.getColumnIndexOrThrow("id")
        val albumIndex = cursor.getColumnIndexOrThrow("album")

        val oldList = mutableListOf<Pair<Int, String>>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(idIndex)
            val album = cursor.getInt(albumIndex)
            val albumUuid = Uuid.fromLongs(album.toLong(), 0L).toString()

            oldList.add(Pair(id, albumUuid))
        }

        db.execSQL("DROP TABLE `custom_items`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_items` (`id` INTEGER NOT NULL, `album` TEXT NOT NULL, PRIMARY KEY(`id`, `album`), FOREIGN KEY(`id`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_items_id` ON `custom_items` (`id` DESC)")

        oldList.forEach { (id, album) ->
            db.delete(
                table = "custom_items",
                whereClause = "id = ?",
                whereArgs = arrayOf(id)
            )

            db.insert(
                table = "custom_items",
                conflictAlgorithm = OnConflictStrategy.REPLACE,
                values = ContentValues().apply {
                    put("id", id)
                    put("album", album)
                }
            )
        }
    }
}