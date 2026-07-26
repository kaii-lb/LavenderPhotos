package com.kaii.photos.database.transactions

import androidx.room.withTransaction
import com.kaii.photos.database.MediaDatabase

class RoomTransactionRunner(
    private val db: MediaDatabase
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}