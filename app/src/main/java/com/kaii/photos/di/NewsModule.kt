package com.kaii.photos.di

import com.kaii.photos.data.datasources.GithubLatestNewsDataSource
import com.kaii.photos.data.datasources.LatestNewsDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NewsModule {
    @Binds
    abstract fun bindLatestNewsDataSource(impl: GithubLatestNewsDataSource): LatestNewsDataSource
}