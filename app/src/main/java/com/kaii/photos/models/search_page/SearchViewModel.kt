package com.kaii.photos.models.search_page

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaDataSource
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.models.search_page.SearchViewModel"

class SearchViewModel(
    context: Context,
    sortMode: MediaItemSortMode,
    displayDateFormat: DisplayDateFormat
) : ViewModel() {
    private val cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource =
        MediaDataSource(
            context = context,
            sqliteQuery = SQLiteQuery(query = "", paths = null, basePaths = null),
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> =
        mediaStoreDataSource.loadMediaStoreData().flowOn(Dispatchers.IO).flowOn(Dispatchers.IO)

    private val _groupedMedia = MutableStateFlow<List<MediaStoreData>>(emptyList())
    val groupedMedia = _groupedMedia.asStateFlow()

    fun setMedia(
        context: Context,
        media: List<MediaStoreData>,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) {
        _groupedMedia.value = groupPhotosBy(
            media = media,
            sortBy = sortMode,
            displayDateFormat = displayDateFormat,
            context = context
        )
    }

    private val formats = listOf(
        "d/M/yyyy",
        "d/M/yyyy",
        "d-M-yyyy",
        "d M yyyy",
        "d M yyyy",
        "d M yyyy",
        "M/d/yyyy",
        "M-d-yyyy",
        "M d yyyy",
        "M d yyyy",
        "M d yyyy"
    )

    fun searchByName(name: String): List<MediaStoreData> {
        val list = mediaFlow.value

        return list.filter { data ->
            data.type != MediaType.Section && data.displayName.lowercase().contains(name.lowercase())
        }
    }

    @OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
    fun searchByDateFormat(query: String): List<MediaStoreData> {
        var parsed: LocalDate? = null

        for (format in formats) {
            try {
                parsed = LocalDate.parse(
                    input = query,
                    format = LocalDate.Format {
                        byUnicodePattern(format)
                    }
                )
                break
            } catch (e: Throwable) {
                Log.d(TAG, e.toString())
            }
        }

        if (parsed == null) return emptyList()

        val list = mediaFlow.value
        return list.filter { data ->
            val date = data.getDateTakenDay().toLocalDate()

            data.type != MediaType.Section && date.day == parsed.day && date.month == parsed.month && date.year == parsed.year
        }
    }

    fun searchByDateNames(query: String): List<MediaStoreData> {
        val unpacked = query.split(" ")
        if (unpacked.size !in 2..3) return emptyList()

        try {
            val list = mediaFlow.value

            // dayNumber month year
            if (unpacked[0].length <= 2) {
                val month = Month.valueOf(unpacked[1].uppercase()).number
                val year = unpacked.getOrNull(2)?.toIntOrNull()

                unpacked[0].toIntOrNull()?.let { day ->
                    return list.filter { data ->
                        val date = data.getDateTakenDay().toLocalDate()

                        data.type != MediaType.Section && date.day == day && date.month.number == month && year?.let { date.year == year } ?: true
                    }
                }
            }

            // month year
            if (unpacked[0].length > 2 && unpacked[1].length == 4) {
                val month = Month.valueOf(unpacked[0].uppercase()).number
                val year = unpacked[1].toIntOrNull() ?: 0

                return list.filter { data ->
                    val date = data.getDateTakenDay().toLocalDate()

                    data.type != MediaType.Section && date.month.number == month && date.year == year
                }
            }

            val day = DayOfWeek.valueOf(unpacked[0].uppercase()).isoDayNumber
            val month = Month.valueOf(unpacked[1].uppercase()).number
            val year = unpacked.getOrNull(2)?.toIntOrNull()

            return list.filter { data ->
                val date = data.getDateTakenDay().toLocalDate()

                data.type != MediaType.Section && date.dayOfWeek.isoDayNumber == day && date.month.number == month && year?.let { date.year == year } ?: true
            }
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, e.toString())

            return emptyList()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun Long.toLocalDate() = Instant.fromEpochSeconds(
        epochSeconds = this
    ).toLocalDateTime(
        timeZone = TimeZone.currentSystemDefault()
    ).date
}