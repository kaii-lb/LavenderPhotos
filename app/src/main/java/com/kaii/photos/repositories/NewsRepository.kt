package com.kaii.photos.repositories

import com.kaii.photos.datasources.NewsDataSource

class NewsRepository(
    private val newsDataSource: NewsDataSource
) {
    suspend fun getNewsData(page: Int, size: Int): NewsDataSource.PageResponse {
        return newsDataSource.getPage(page, size)
    }
}