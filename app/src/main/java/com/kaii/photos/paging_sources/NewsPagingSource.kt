package com.kaii.photos.paging_sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kaii.photos.datasources.News
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
            val currentPage = params.key ?: 1
            val response = newsRepository.getNewsData(currentPage, params.loadSize)

            LoadResult.Page(
                data = response.data,
                prevKey = (currentPage - 1).takeIf { currentPage != 1 },
                nextKey = (currentPage + 1).takeIf { !response.isEndOfList }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}