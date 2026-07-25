package com.kaii.photos.file_management.managers.gateways

interface SyncWorkerGateway {
    fun enqueueSyncWorker(albumId: String)
}