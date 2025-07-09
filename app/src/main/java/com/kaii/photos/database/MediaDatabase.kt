package com.kaii.photos.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.kaii.photos.database.daos.FavouritedItemEntityDao
import com.kaii.photos.database.daos.MediaEntityDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.TrashedItemEntityDao
import com.kaii.photos.database.entities.FavouritedItemEntity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.database.entities.TrashedItemEntity

@Database(
    entities =
        [
            MediaEntity::class,
            TrashedItemEntity::class,
            FavouritedItemEntity::class,
            SecuredItemEntity::class
        ],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = DeleteOldTable::class)
    ]
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun favouritedItemEntityDao(): FavouritedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getInstance(context: Context): MediaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    MediaDatabase::class.java,
                    "media-database"
                ).apply {
                    addMigrations(Migration3to4(context), Migration4to5(context))
                    enableMultiInstanceInvalidation()
                    fallbackToDestructiveMigrationOnDowngrade(false)
                }.build()

                INSTANCE = instance
                instance
            }
        }
    }

}

@DeleteTable("ImmichDuplicateEntity")
class DeleteOldTable : AutoMigrationSpec