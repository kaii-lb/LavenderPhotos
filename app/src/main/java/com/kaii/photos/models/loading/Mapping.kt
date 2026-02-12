package com.kaii.photos.models.loading

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.formatDate
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Flow<PagingData<MediaStoreData>>.mapToMedia(
    accessToken: String
) = this.map { pagingData ->
    pagingData.map {
        PhotoLibraryUIModel.Media(
            item = it,
            accessToken = accessToken
        ) as PhotoLibraryUIModel
    }
}

fun Flow<PagingData<PhotoLibraryUIModel>>.mapToSeparatedMedia(
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) = this.map { pagingData ->
    pagingData.insertSeparators { before, after ->
        before as PhotoLibraryUIModel.MediaImpl?
        after as PhotoLibraryUIModel.MediaImpl?

        val beforeDate: Long?
        val afterDate: Long?

        when {
            sortMode.isDisabled -> {
                beforeDate = null
                afterDate = null
            }

            sortMode == MediaItemSortMode.MonthTaken -> {
                beforeDate = before?.item?.getMonthTaken()
                afterDate = after?.item?.getMonthTaken()
            }

            sortMode.isDateModified -> {
                beforeDate = before?.item?.getDateModifiedDay()
                afterDate = after?.item?.getDateModifiedDay()
            }

            else -> {
                beforeDate = before?.item?.getDateTakenDay()
                afterDate = after?.item?.getDateTakenDay()
            }
        }

        when {
            beforeDate == null && afterDate != null -> PhotoLibraryUIModel.Section(
                title = formatDate(
                    timestamp = afterDate,
                    sortBy = sortMode,
                    format = format
                ),
                timestamp = afterDate
            )

            beforeDate != afterDate && afterDate != null -> PhotoLibraryUIModel.Section(
                title = formatDate(
                    timestamp = afterDate,
                    sortBy = sortMode,
                    format = format
                ),
                timestamp = afterDate
            )

            else -> null
        }
    }
}

fun Flow<PagingData<PhotoLibraryUIModel.SecuredMedia>>.mapToSecuredMedia(
    accessToken: String
) = this.map { pagingData ->
    pagingData.map {
        PhotoLibraryUIModel.SecuredMedia(
            item = it.item,
            bytes = it.bytes,
            accessToken = accessToken
        ) as PhotoLibraryUIModel
    }
}