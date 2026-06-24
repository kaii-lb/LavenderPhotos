package com.kaii.photos.database.migrations

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration10To11 : Migration(startVersion = 10, endVersion = 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT * FROM custom_media")

        val idIndex = cursor.getColumnIndexOrThrow("mediaId")
        val albumIndex = cursor.getColumnIndexOrThrow("album")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_hash` ON `media` (`hash`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_immichUrl` ON `media` (`immichUrl`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_absolutePath` ON `media` (`absolutePath`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_items` (`id` INTEGER NOT NULL, `album` INTEGER NOT NULL, PRIMARY KEY(`id`, `album`), FOREIGN KEY(`id`) REFERENCES `media`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_items_id` ON `custom_items` (`id` DESC)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateModified` INTEGER NOT NULL, `status` TEXT NOT NULL, `type` TEXT NOT NULL, `itemIds` TEXT NOT NULL)")

        while (cursor.moveToNext()) {
            val id = cursor.getInt(idIndex)
            val album = cursor.getInt(albumIndex)

            db.insert(
                table = "custom_items",
                conflictAlgorithm = OnConflictStrategy.REPLACE,
                values = ContentValues().apply {
                    put("id", id)
                    put("album", album)
                }
            )
        }

        db.execSQL("DROP TABLE custom_media")
    }
}