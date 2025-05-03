package com.kaii.photos.models.multi_album

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private const val TAG = "MULTI_ALBUM_VIEW_MODEL"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortBy: MediaItemSortMode
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(initDataSource(context, albumInfo, sortBy))

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
    }

    private fun getMediaDataFlow(): State<Flow<List<MediaStoreData>>> = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() = cancellationSignal.cancel()

    fun reinitDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode = sortBy
    ) {
        sortBy = sortMode
        if (album == albumInfo) return

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, album, sortBy)
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        sortBy = sortMode

        cancelMediaFlow()
        cancellationSignal = CancellationSignal()
        mediaStoreDataSource.value = initDataSource(context, albumInfo, sortBy)
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortBy: MediaItemSortMode
    ) = run {
        val query = mainViewModel.settings.MainPhotosView.getSQLiteQuery(album.paths)
        Log.d(TAG, "query is $query")

        albumInfo = album
        this.sortBy = sortBy

        MultiAlbumDataSource(
            context = context,
            queryString = query,
            sortBy = sortBy,
            cancellationSignal = cancellationSignal
        )
    }
}

/** Groups photos by date */
fun groupPhotosBy(
    media: List<MediaStoreData>,
    sortBy: MediaItemSortMode = MediaItemSortMode.DateTaken
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
    sortedMap.forEach { (sectionTime, children) ->
        val sectionKey = when (sectionTime) {
            today -> {
                "Today"
            }

            yesterday -> {
                "Yesterday"
            }

            else -> {
                formatDate(timestamp = sectionTime, sortBy = sortBy)
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

fun formatDate(timestamp: Long, sortBy: MediaItemSortMode): String {
    return if (timestamp != 0L) {
        val dateTimeFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) DateTimeFormatter.ofPattern("MMMM yyyy")
            else DateTimeFormatter.ofPattern("EEE d - MMMM yyyy")

        val localDateTime =
            Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateTimeString = localDateTime.format(dateTimeFormat)
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
