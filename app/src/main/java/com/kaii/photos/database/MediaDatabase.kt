package com.kaii.photos.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaii.photos.database.daos.FavouritedItemEntityDao
import com.kaii.photos.database.daos.ImmichDuplicateEntityDao
import com.kaii.photos.database.daos.MediaEntityDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.TrashedItemEntityDao
import com.kaii.photos.database.entities.FavouritedItemEntity
import com.kaii.photos.database.entities.ImmichDupeConverter
import com.kaii.photos.database.entities.ImmichDuplicateEntity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.database.entities.TrashedItemEntity

@Database(
    entities =
        [
            MediaEntity::class,
            TrashedItemEntity::class,
            FavouritedItemEntity::class,
            SecuredItemEntity::class,
            ImmichDuplicateEntity::class
        ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 5, to = 6)
    ]
)
@TypeConverters(value = [ImmichDupeConverter::class])
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun favouritedItemEntityDao(): FavouritedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao
    abstract fun immichDuplicateEntityDao(): ImmichDuplicateEntityDao
}
