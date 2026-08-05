package com.kaii.photos.domain.files

data class BatchModificationResult(
    val needsPermission: List<FileOperationItemMetadata>,
    val failed: List<FileOperationItemMetadata>
)