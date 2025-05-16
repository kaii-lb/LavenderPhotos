package com.aks_labs.tulsi.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.aks_labs.tulsi.database.daos.FavouritedItemEntityDao
import com.aks_labs.tulsi.database.daos.MediaEntityDao
import com.aks_labs.tulsi.database.daos.SecuredMediaItemEntityDao
import com.aks_labs.tulsi.database.daos.TrashedItemEntityDao
import com.aks_labs.tulsi.database.entities.FavouritedItemEntity
import com.aks_labs.tulsi.database.entities.MediaEntity
import com.aks_labs.tulsi.database.entities.SecuredItemEntity
import com.aks_labs.tulsi.database.entities.TrashedItemEntity

@Database(entities =
    [
        MediaEntity::class,
        TrashedItemEntity::class,
        FavouritedItemEntity::class,
        SecuredItemEntity::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        // AutoMigration(from = 4, to = 5)
    ]
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaEntityDao(): MediaEntityDao
    abstract fun trashedItemEntityDao(): TrashedItemEntityDao
    abstract fun favouritedItemEntityDao(): FavouritedItemEntityDao
    abstract fun securedItemEntityDao(): SecuredMediaItemEntityDao
}


