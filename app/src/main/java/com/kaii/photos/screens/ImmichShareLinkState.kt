package com.kaii.photos.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kaii_lb.lavender.immichintegration.serialization.SharedLinkRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.SharedLinkType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImmichShareLinkState {
    var customSlug by mutableStateOf<String?>(null)
    var password by mutableStateOf<String?>(null)
    var description by mutableStateOf("")
    var allowUploads by mutableStateOf(false)
    var allowDownloads by mutableStateOf(true)
    var expiryDate by mutableStateOf<Instant?>(null)

    @OptIn(ExperimentalUuidApi::class)
    fun createRequest(albumImmichId: String) =
        SharedLinkRequest(
            albumId = Uuid.parse(albumImmichId),
            allowDownload = allowDownloads,
            allowUpload = allowUploads,
            assetIds = emptyList(),
            description = description,
            expiresAt = expiryDate?.toString(),
            password = password,
            showMetadata = true,
            slug = customSlug,
            type = SharedLinkType.Album
        )
}