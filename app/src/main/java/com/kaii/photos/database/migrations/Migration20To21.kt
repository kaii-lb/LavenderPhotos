package com.kaii.photos.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migrate to new and improved sync task system */
class Migration20To21 : Migration(startVersion = 20, endVersion = 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS sync_task_item;")
        db.execSQL("DROP TABLE IF EXISTS sync_tasks;")

        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `operation` TEXT NOT NULL, `isRemoval` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `lastError` TEXT)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_task_item` (`mediaId` INTEGER NOT NULL, `taskId` INTEGER NOT NULL, `immichId` TEXT, PRIMARY KEY(`mediaId`, `taskId`), FOREIGN KEY(`taskId`) REFERENCES `sync_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_task_item_taskId` ON `sync_task_item` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_task_item_mediaId` ON `sync_task_item` (`mediaId`)")
    }
}