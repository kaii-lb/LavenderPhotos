package com.kaii.photos.helpers.paging

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import io.github.kaii_lb.lavender.immichintegration.Auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone

fun Flow<PagingData<MediaStoreData>>.mapToMedia(
    auth: Auth,
    endpoint: String
) = this.map { pagingData ->
    pagingData.map {
        PhotoLibraryUIModel.Media(
            item = it,
            auth = auth,
            endpoint = endpoint
        ) as PhotoLibraryUIModel
    }
}

fun Flow<PagingData<PhotoLibraryUIModel>>.mapToSeparatedMedia(
    sortMode: MediaItemSortMode,
    dateFormatter: LocalizedDateFormatter
) = this.map { pagingData ->
    val tz = TimeZone.getDefault()

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
                beforeDate = before?.item?.getMonthTaken(tz)
                afterDate = after?.item?.getMonthTaken(tz)
            }

            sortMode.isDateModified -> {
                beforeDate = before?.item?.getDateModifiedDay(tz)
                afterDate = after?.item?.getDateModifiedDay(tz)
            }

            else -> {
                beforeDate = before?.item?.getDateTakenDay(tz)
                afterDate = after?.item?.getDateTakenDay(tz)
            }
        }

        when {
            beforeDate == null && afterDate != null -> PhotoLibraryUIModel.Section(
                title = dateFormatter.formatDay(afterDate),
                timestamp = afterDate
            )

            beforeDate != afterDate && afterDate != null -> PhotoLibraryUIModel.Section(
                title = dateFormatter.formatDay(afterDate),
                timestamp = afterDate
            )

            else -> null
        }
    }
}

fun Flow<PagingData<PhotoLibraryUIModel.SecuredMedia>>.mapToSecuredMedia(
    auth: Auth,
    endpoint: String
) = this.map { pagingData ->
    pagingData.map {
        PhotoLibraryUIModel.SecuredMedia(
            item = it.item,
            bytes = it.bytes,
            auth = auth,
            endpoint = endpoint
        ) as PhotoLibraryUIModel
    }
}