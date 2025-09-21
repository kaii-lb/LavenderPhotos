package com.kaii.photos.models.multi_album

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.MultiAlbumDataSource
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortBy: MediaItemSortMode,
    private val displayDateFormat: DisplayDateFormat,
    private val database: MediaDatabase,
    var ignorePaths: Boolean = false
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortBy, ignorePaths))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300000
            ),
            initialValue = emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = sortBy,
        ignorePaths: Boolean = this.ignorePaths
    ) {
        this.sortBy = sortMode
        this.ignorePaths = ignorePaths
        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, sortBy, ignorePaths)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        this.sortBy = sortMode
        this.ignorePaths = ignorePaths

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy, ignorePaths)
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode,
        ignorePaths: Boolean
    ) = run {
        val query = getSQLiteQuery(album.paths)
        Log.d(TAG, "query is $query")

        albumInfo = album
        this.sortBy = sortBy
        this.ignorePaths = ignorePaths

        MultiAlbumDataSource(
            context = context,
            queryString = query,
            sortBy = sortBy,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat,
            database = database
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
