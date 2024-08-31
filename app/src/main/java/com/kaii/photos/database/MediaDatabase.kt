package com.kaii.photos.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kaii.photos.database.daos.MediaEntityDao
import com.kaii.photos.database.entities.MediaEntity

@Database(entities =
    [
        MediaEntity::class
    ],
    version = 1,
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
}
