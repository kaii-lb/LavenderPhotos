package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.NewsDataSource
import com.kaii.photos.domain.news.NewsPageResponse

class NewsRepository(
    private val newsDataSource: NewsDataSource
) {
    suspend fun getNewsData(offset: Int, size: Int): NewsPageResponse {
        return newsDataSource.getPage(offset, size)
    }
}