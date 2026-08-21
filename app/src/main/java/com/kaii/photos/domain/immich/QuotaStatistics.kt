package com.kaii.photos.domain.immich

/** All values are in bytes */
data class QuotaStatistics(
    val usage: Long?,
    val size: Long?
)