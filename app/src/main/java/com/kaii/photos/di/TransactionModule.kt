package com.kaii.photos.di

import com.kaii.photos.database.transactions.RoomTransactionRunner
import com.kaii.photos.database.transactions.TransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionModule {
    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}