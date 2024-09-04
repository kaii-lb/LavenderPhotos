package com.kaii.photos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kaii.photos.database.daos.MediaEntityDao
import com.kaii.photos.database.daos.TrashedItemEntityDao
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.TrashedItemEntity

@Database(entities =
    [
        MediaEntity::class,
        TrashedItemEntity::class
    ],
    version = 1,
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
}
