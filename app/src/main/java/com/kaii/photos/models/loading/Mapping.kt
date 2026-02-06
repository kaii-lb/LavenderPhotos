package com.kaii.photos.models.loading

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.formatDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Flow<PagingData<MediaStoreData>>.mapToMedia(
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    accessToken: String,
    separators: Boolean = true
) = this.map { pagingData ->
    if (sortMode.isDisabled || !separators) {
        pagingData.map {
            PhotoLibraryUIModel.Media(
                item = it,
                accessToken = accessToken
            ) as PhotoLibraryUIModel
        }
    } else {
        pagingData.map {
            PhotoLibraryUIModel.Media(
                item = it,
                accessToken = accessToken
            )
        }.insertSeparators { before, after ->
            val beforeDate: Long?
            val afterDate: Long?

            when {
                sortMode.isLastModified -> {
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
                    )
                )

                beforeDate != afterDate && afterDate != null -> PhotoLibraryUIModel.Section(
                    title = formatDate(
                        timestamp = afterDate,
                        sortBy = sortMode,
                        format = format
                    )
                )

                else -> null
            }
        }
    }
}

fun Flow<PagingData<PhotoLibraryUIModel.SecuredMedia>>.mapToMedia(
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    separators: Boolean = true
) = this.map { pagingData ->
    if (sortMode.isDisabled || !separators) {
        pagingData.map {
            it as PhotoLibraryUIModel
        }
    } else {
        pagingData.map {
            it
        }.insertSeparators { before, after ->
            val beforeDate: Long?
            val afterDate: Long?

            when {
                sortMode.isLastModified -> {
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
                    )
                )

                beforeDate != afterDate && afterDate != null -> PhotoLibraryUIModel.Section(
                    title = formatDate(
                        timestamp = afterDate,
                        sortBy = sortMode,
                        format = format
                    )
                )

                else -> null
            }
        }
    }
}