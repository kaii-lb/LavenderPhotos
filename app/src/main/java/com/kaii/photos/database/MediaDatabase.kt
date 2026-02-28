package com.kaii.photos.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.FavouritedItemEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SearchDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.daos.TagDao
import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.database.daos.TrashedItemEntityDao
import com.kaii.photos.database.entities.ColorTypeConverter
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.FavouritedItemEntity
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.database.entities.TaggedItem
import com.kaii.photos.database.entities.TrashedItemEntity

@Database(
    entities =
        [
            MediaStoreData::class,
            TrashedItemEntity::class,
            FavouritedItemEntity::class,
            SecuredItemEntity::class,
            CustomItem::class,
            SyncTask::class,
            Tag::class,
            TaggedItem::class
        ],
    version = 12,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = DeleteDupeEntityTable::class),
        AutoMigration(from = 7, to = 8, spec = DeleteOldMediaTable::class),
        AutoMigration(from = 8, to = 9, spec = DropCustomIdColumnSpec::class),
        AutoMigration(from = 10, to = 11, spec = DropOldCustomTable::class),
        AutoMigration(from = 11, to = 12)
    ]
)
@TypeConverters(ColorTypeConverter::class)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun favouritesDao(): FavouritedItemEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao
    abstract fun customDao(): CustomEntityDao
    abstract fun taskDao(): SyncTaskDao
    abstract fun searchDao(): SearchDao
    abstract fun tagDao(): TagDao
    abstract fun taggedItemsDao(): TaggedItemsDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getInstance(context: Context): MediaDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                val instance = Room.databaseBuilder(
                    appContext,
                    MediaDatabase::class.java,
                    "media-database"
                ).apply {
                    addMigrations(Migration3to4(appContext), Migration4to5(appContext), Migration9To10(appContext))
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

@DeleteTable(tableName = "custom_media")
class DropOldCustomTable : AutoMigrationSpec