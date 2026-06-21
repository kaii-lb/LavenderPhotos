package com.kaii.photos.models.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.paging_sources.NewsPagingSource
import com.kaii.photos.repositories.NewsRepository

class NewsViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {
    val news = Pager(
        config = PagingConfig(pageSize = 20),
        initialKey = 1,
        pagingSourceFactory = { NewsPagingSource(newsRepository) }
    ).flow.cachedIn(viewModelScope)
}