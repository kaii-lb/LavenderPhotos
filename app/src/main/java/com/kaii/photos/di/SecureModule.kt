package com.kaii.photos.di

import android.content.Context
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.file_management.secure.LocalSecureManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecureModule {
    @Provides
    @Singleton
    fun provideLocalSecureManager(
        secureDao: SecuredMediaItemEntityDao,
        @ApplicationContext context: Context
    ): LocalSecureManager = LocalSecureManager(secureDao, context)
}
