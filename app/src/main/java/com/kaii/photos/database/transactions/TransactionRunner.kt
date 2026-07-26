package com.kaii.photos.database.transactions

interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}