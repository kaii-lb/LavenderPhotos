package com.kaii.photos.database.migrations

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration16To17(val context: Context) : Migration(startVersion = 16, endVersion = 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sync_tasks DROP COLUMN itemIds")
        db.execSQL("ALTER TABLE sync_tasks ADD COLUMN items TEXT NOT NULL DEFAULT '[]'")
    }
}