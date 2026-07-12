package com.kaii.photos.domain.immich

fun String.cleanUrl() = this.let {
    var cleaned = it
    while (cleaned.endsWith("/")) {
        cleaned = cleaned.removeSuffix("/")
    }

    cleaned
}