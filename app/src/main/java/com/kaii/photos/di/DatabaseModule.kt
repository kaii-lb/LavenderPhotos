package com.kaii.photos.di

import android.content.Context
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.daos.TagDao
import com.kaii.photos.database.entities.ExifDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMediaDatabase(
        @ApplicationContext context: Context
    ): MediaDatabase = MediaDatabase.getInstance(context)

    @Provides
    fun provideMediaDao(db: MediaDatabase): MediaDao = db.mediaDao()

    @Provides
    fun provideCustomEntityDao(db: MediaDatabase): CustomEntityDao = db.customDao()

    @Provides
    fun provideSyncTaskDao(db: MediaDatabase): SyncTaskDao = db.taskDao()

    @Provides
    fun provideSecuredMediaItemEntityDao(db: MediaDatabase): SecuredMediaItemEntityDao = db.securedItemEntityDao()

    @Provides
    fun provideExifDataDao(db: MediaDatabase): ExifDataDao = db.exifDataDao()

    @Provides
    fun provideTagDao(db: MediaDatabase): TagDao = db.tagDao()
}