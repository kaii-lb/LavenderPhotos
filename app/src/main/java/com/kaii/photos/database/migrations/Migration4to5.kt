package com.kaii.photos.database.migrations

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration4to5(val context: Context) : Migration(startVersion = 4, endVersion = 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE secureditementity")
        db.execSQL("CREATE TABLE IF NOT EXISTS `secureditementity` (`originalPath` TEXT NOT NULL, `secured_path` TEXT NOT NULL, `iv` BLOB NOT NULL, PRIMARY KEY(`originalPath`))")
    }
}