package com.kaii.photos.file_management.sync.types

data class LinkAction(
    val localId: Long,
    val hash: String,
    val immichUrl: String
)