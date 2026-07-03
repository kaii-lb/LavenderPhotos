package com.kaii.photos.domain.immich

import io.github.kaii_lb.lavender.immichintegration.serialization.user.UsageByUserDto

data class ImmichServerInfo(
    val version: String,
    val build: String?,
    val online: Boolean,
    val diskSize: String,
    val diskUsed: String,
    val diskUsedPercentage: Float,
    val perUserStorage: List<UsageByUserDto>,
    val newVersion: String?
)