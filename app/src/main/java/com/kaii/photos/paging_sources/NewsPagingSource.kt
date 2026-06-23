package com.kaii.photos.paging_sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kaii.photos.domain.news.News
import com.kaii.photos.repositories.NewsRepository

class NewsPagingSource(
    private val newsRepository: NewsRepository
) : PagingSource<Int, News>() {
    override fun getRefreshKey(state: PagingState<Int, News>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, News> {
        return try {
            val currentOffset = params.key ?: 0
            val response = newsRepository.getNewsData(currentOffset, params.loadSize)

            LoadResult.Page(
                data = response.data,
                prevKey = maxOf(0, currentOffset - params.loadSize).takeIf { currentOffset != 0 },
                nextKey = (currentOffset + response.data.size).takeIf { !response.isEndOfList }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override val jumpingSupported = true
}