package com.kaii.photos.database.transactions

import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val db: MediaDatabase
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}