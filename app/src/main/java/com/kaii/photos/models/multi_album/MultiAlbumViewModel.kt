package com.kaii.photos.models.multi_album

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.helpers.formatDate
import com.kaii.photos.mediastore.MediaDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

// private const val TAG = "com.kaii.photos.models.MultiAlbumViewModel"

class MultiAlbumViewModel(
    context: Context,
    var albumInfo: AlbumInfo,
    var sortMode: MediaItemSortMode,
    var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(
        initDataSource(
            context = context,
            album = albumInfo,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    )

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300.seconds.inWholeMilliseconds
            ),
            initialValue = emptyList()
        )
    }

    private fun getMediaDataFlow() = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    fun cancelMediaFlow() {
        cancellationSignal.cancel()
        cancellationSignal = CancellationSignal()
    }

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

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = album,
                sortMode = sortMode,
                displayDateFormat = displayDateFormat
            )
    }

    fun changeSortMode(
        context: Context,
        sortMode: MediaItemSortMode
    ) {
        if (this.sortMode == sortMode) return

        this.sortMode = sortMode

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = this.albumInfo,
                sortMode = sortMode,
                displayDateFormat = this.displayDateFormat
            )
    }

    fun changeDisplayDateFormat(
        context: Context,
        displayDateFormat: DisplayDateFormat
    ) {
        if (this.displayDateFormat == displayDateFormat) return

        this.displayDateFormat = displayDateFormat

        cancelMediaFlow()
        mediaStoreDataSource.value =
            initDataSource(
                context = context,
                album = this.albumInfo,
                sortMode = this.sortMode,
                displayDateFormat = displayDateFormat
            )
    }

    private fun initDataSource(
        context: Context,
        album: AlbumInfo,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = run {
        val query = getSQLiteQuery(album.paths)

        this.albumInfo = album
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        MediaDataSource(
            context = context,
            sqliteQuery = query,
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )
    }

    override fun onCleared() {
        super.onCleared()
        cancelMediaFlow()
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

                MediaItemSortMode.LastModified, MediaItemSortMode.DisabledLastModified -> {
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
