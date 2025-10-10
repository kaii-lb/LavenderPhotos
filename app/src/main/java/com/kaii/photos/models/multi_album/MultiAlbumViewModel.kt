package com.kaii.photos.models.multi_album

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.CancellationSignal
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.StreamingDataSource
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import java.util.Locale

// private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortMode, displayDateFormat))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300000
            ),
            initialValue = emptyList()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO).flatMapMerge { it.flowOn(Dispatchers.IO) }.flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, this.sortMode, displayDateFormat)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, this.sortMode, this.displayDateFormat)
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortMode, displayDateFormat)
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = run {
        val query = getSQLiteQuery(album.paths)

        albumInfo = album
        this.sortMode = sortBy
        this.displayDateFormat = displayDateFormat

        StreamingDataSource(
            context = context,
            sqliteQuery = query,
            sortBy = sortBy,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )
    }
}

/** Groups photos by date */
fun groupPhotosBy(
    media: List<MediaStoreData>,
    sortBy: MediaItemSortMode = MediaItemSortMode.DateTaken,
    displayDateFormat: DisplayDateFormat,
    context: Context
): List<MediaStoreData> {
    if (media.isEmpty()) return emptyList()

    val sortedList =
        media.sortedByDescending { item ->
            when (sortBy) {
                MediaItemSortMode.DateTaken, MediaItemSortMode.MonthTaken, MediaItemSortMode.Disabled -> {
                    item.dateTaken
                }

                MediaItemSortMode.LastModified -> {
                    item.dateModified
                }
            }
        }

    if (sortBy == MediaItemSortMode.Disabled) return sortedList

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

    val mediaItems = mutableListOf<MediaStoreData>()
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

        val section = SectionItem(date = sectionTime, childCount = children.size)
        mediaItems.add(listSection(sectionKey, sectionTime, children.size))

        mediaItems.addAll(
            children.onEach {
                it.section = section
            }
        )
    }

    return mediaItems
}

enum class DisplayDateFormat(val format: String) {
    Default(format = "EEE d - MMMM yyyy"),
    Short(format = "d - MMM yy"),
    Compressed(format = "MM/dd/yyyy")
}

fun formatDate(timestamp: Long, sortBy: MediaItemSortMode, format: DisplayDateFormat): String {
    return if (timestamp != 0L) {
        val dateFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            else SimpleDateFormat(format.format, Locale.getDefault())

        val dateTimeString = dateFormat.format(Date(timestamp * 1000))
        dateTimeString.toString()
    } else {
        "Pretend there is a date here"
    }
}

private fun listSection(title: String, key: Long, childCount: Int): MediaStoreData {
    val mediaSection = MediaStoreData(
        type = MediaType.Section,
        dateModified = key,
        dateTaken = key,
        uri = "$title $key".toUri(),
        displayName = title,
        id = 0L,
        mimeType = null,
        section = SectionItem(
            date = key,
            childCount = childCount
        )
    )
    return mediaSection
}
