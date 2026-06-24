package com.kaii.photos.domain

data class PageResponse<T>(
    val data: List<T>,
    val isEndOfList: Boolean
)