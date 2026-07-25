package com.kaii.photos.domain.files

data class FileOperationItemMetadata(
    val id: Long,
    val uri: String,
    val absolutePath: String,
    val isImage: Boolean,
    val immichUrl: String?
) {
    val immichId: String?
        get() = immichUrl?.split("/")?.dropLast(1)?.last()

    val isCloud: Boolean
        get() = uri.startsWith("/api")

    val isLocalOrLinked: Boolean
        get() = !isCloud

    val isCloudOrLinked: Boolean
        get() = immichId != null
}