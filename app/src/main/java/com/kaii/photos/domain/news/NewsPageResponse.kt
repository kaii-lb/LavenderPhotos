package com.kaii.photos.domain.news

data class NewsPageResponse(
    val data: List<News>,
    val page: Int,
    val isEndOfList: Boolean
)