package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.formatDate
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale

// private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    val mediaFlow = Pager(
        config = PagingConfig(
            pageSize = 80,
            prefetchDistance = 40,
            enablePlaceholders = true,
            initialLoadSize = 80
        ),
        pagingSourceFactory = { MediaDatabase.getInstance(context).mediaDao().getPagedMedia() }
    ).flow.mapToMedia(sortMode = sortMode, format = displayDateFormat).cachedIn(viewModelScope)

    // TODO
    // fun cancelMediaFlow() {
    //     cancellationSignal.cancel()
    //     cancellationSignal = CancellationSignal()
    // }

    // TODO
    fun update(
        context: Context,
        album: AlbumInfo
    ) {
        if (album.paths.toSet() != this.albumInfo.paths.toSet()) {
            reinitDataSource(
                context = context,
                album = album,
                sortMode = sortMode,
                displayDateFormat = displayDateFormat
            )
        }
    }

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        if (album == albumInfo && sortMode == this.sortMode && displayDateFormat == this.displayDateFormat) return

        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        // cancelMediaFlow()
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        // cancelMediaFlow()
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

        // cancelMediaFlow()
    }

    override fun onCleared() {
        super.onCleared()
        // cancelMediaFlow()
    }
}

fun Flow<PagingData<MediaStoreData>>.mapToMedia(
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) = this.map { pagingData ->
    pagingData
        .map {
            PhotoLibraryUIModel.Media(it)
        }
        .insertSeparators { before, after ->
            val beforeDate = before?.item?.getDateTakenDay() // TODO
            val afterDate = after?.item?.getDateTakenDay()

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

/** Groups photos by date */
fun groupPhotosBy(
    media: List<MediaStoreData>,
    sortBy: MediaItemSortMode = MediaItemSortMode.DateTaken,
    displayDateFormat: DisplayDateFormat,
    context: Context
): List<PhotoLibraryUIModel> {
    if (media.isEmpty()) return emptyList()

    val sortedList =
        media.sortedByDescending { item ->
            when (sortBy) {
                MediaItemSortMode.DateTaken, MediaItemSortMode.MonthTaken, MediaItemSortMode.Disabled -> {
                    item.dateTaken
                }

                MediaItemSortMode.LastModified, MediaItemSortMode.DisabledLastModified -> {
                    item.dateModified
                }
            }
        }

    if (sortBy == MediaItemSortMode.Disabled) return sortedList.fastMap { PhotoLibraryUIModel.Media(it) }

    val grouped = sortedList.groupBy { item ->
        when (sortBy) {
            MediaItemSortMode.DateTaken -> {
                item.getDateTakenDay()
            }

            MediaItemSortMode.LastModified -> {
                item.getLastModifiedDay()
            }

            MediaItemSortMode.MonthTaken -> {
                item.getDateTakenMonth()
            }

            else -> throw IllegalStateException("Sort mode $sortBy should not be reached here")
        }
    }

    val sortedMap = grouped.toSortedMap(
        compareByDescending { time ->
            time
        }
    )

    val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val today = calendar.timeInMillis / 1000
    val daySeconds = 60 * 60 * 24
    val yesterday = today - daySeconds

    val mediaItems = mutableListOf<PhotoLibraryUIModel>()
    val todayString = context.resources.getString(R.string.today)
    val yesterdayString = context.resources.getString(R.string.yesterday)
    sortedMap.forEach { (sectionTime, children) ->
        val sectionKey = when (sectionTime) {
            today -> {
                todayString
            }

            yesterday -> {
                yesterdayString
            }

            else -> {
                formatDate(
                    timestamp = sectionTime,
                    sortBy = sortBy,
                    format = displayDateFormat
                )
            }
        }

        mediaItems.add(PhotoLibraryUIModel.Section(title = sectionKey))
        // mediaItems.add(listSection(sectionKey, sectionTime, children.size))

        mediaItems.addAll(children.fastMap { PhotoLibraryUIModel.Media(it) })
        // TODO
        // val section = SectionItem(date = sectionTime, childCount = children.size)
        // children.onEach {
        //     it.section = section
        // }
    }

    return mediaItems
}
