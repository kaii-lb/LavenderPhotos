package com.kaii.photos.domain.files

sealed interface FilePermissionAction {
    data class Share(
        val files: List<FileOperationItemMetadata>
    ) : FilePermissionAction
}