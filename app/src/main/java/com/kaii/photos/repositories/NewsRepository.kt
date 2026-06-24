package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.NewsDataSource
import com.kaii.photos.domain.PageResponse
import com.kaii.photos.domain.news.News

class NewsRepository(
    private val newsDataSource: NewsDataSource
) {
    suspend fun getNewsData(offset: Int, size: Int): PageResponse<News> {
        return newsDataSource.getPage(offset, size)
    }
}