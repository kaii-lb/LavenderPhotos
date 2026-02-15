package com.kaii.photos.helpers.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kaii.photos.database.entities.MediaStoreData

class ListPagingSource(
    private val media: List<MediaStoreData>
) : PagingSource<Int, MediaStoreData>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaStoreData> {
        val position = params.key ?: 0

        val endPosition = minOf(position + params.loadSize, media.size)

        return try {
            val data = if (position < media.size) {
                media.subList(position, endPosition)
            } else {
                emptyList()
            }

            LoadResult.Page(
                data = data,
                prevKey = if (position == 0) null else position - params.loadSize,
                nextKey = if (endPosition >= media.size) null else endPosition,
                itemsBefore = if (position == 0) 0 else position - 1,
                itemsAfter = if (endPosition >= media.size) 0 else media.size - endPosition
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaStoreData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }
}