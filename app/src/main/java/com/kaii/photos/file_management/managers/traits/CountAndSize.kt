package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.datastore.AlbumType

interface CountAndSize {
    suspend fun getMediaCount(album: AlbumType): Int

    suspend fun getMediaSize(album: AlbumType): Long
}