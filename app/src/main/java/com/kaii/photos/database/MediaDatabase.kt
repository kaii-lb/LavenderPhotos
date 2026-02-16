package com.kaii.photos.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.FavouritedItemEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.TrashedItemEntityDao
import com.kaii.photos.database.entities.CustomItemEntity
import com.kaii.photos.database.entities.FavouritedItemEntity
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.database.entities.TrashedItemEntity

@Database(
    entities =
        [
            MediaStoreData::class,
            TrashedItemEntity::class,
            FavouritedItemEntity::class,
            SecuredItemEntity::class,
            CustomItemEntity::class
        ],
    version = 10,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = DeleteDupeEntityTable::class),
        AutoMigration(from = 7, to = 8, spec = DeleteOldMediaTable::class),
        AutoMigration(from = 8, to = 9, spec = DropCustomIdColumnSpec::class)
    ]
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun favouritesDao(): FavouritedItemEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao
    abstract fun customDao(): CustomEntityDao

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
                    addMigrations(Migration3to4(context), Migration4to5(context), Migration9To10(context))
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
class DeleteDupeEntityTable : AutoMigrationSpec

@DeleteTable(tableName = "MediaEntity")
class DeleteOldMediaTable : AutoMigrationSpec

@DeleteColumn(
    tableName = "media",
    columnName = "customId"
)
class DropCustomIdColumnSpec : AutoMigrationSpec