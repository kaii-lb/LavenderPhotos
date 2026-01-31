package com.kaii.photos.models.search_page

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaDataSource
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import com.kaii.photos.models.multi_album.groupPhotosBy
import com.kaii.photos.models.multi_album.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.models.search_page.SearchViewModel"

class SearchViewModel(
    context: Context,
    private var sortMode: MediaItemSortMode,
    private var displayDateFormat: DisplayDateFormat
) : ViewModel() {
    var query = ""
    private var cancellationSignal = CancellationSignal()
    private val mediaStoreDataSource = mutableStateOf(
        MediaDataSource(
            context = context,
            sqliteQuery = SQLiteQuery(query = "", paths = null, basePaths = null),
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
        )
    )

    val mediaFlow by derivedStateOf {
        getMediaDataFlow().value.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 10.seconds.inWholeMilliseconds
            ),
            initialValue = emptyList()
        )
    }

    // TODO: fix this
    val mediaPagingFlow = Pager(
        config = PagingConfig(
            pageSize = 80,
            prefetchDistance = 40,
            enablePlaceholders = true,
            initialLoadSize = 80
        ),
        pagingSourceFactory = { MediaDatabase.getInstance(context).mediaDao().getPagedMedia() }
    ).flow.mapToMedia(sortMode = sortMode, format = displayDateFormat).cachedIn(viewModelScope)

    private fun getMediaDataFlow() = derivedStateOf {
        mediaStoreDataSource.value.loadMediaStoreData().flowOn(Dispatchers.IO)
    }

    private val _groupedMedia = MutableStateFlow<List<PhotoLibraryUIModel>>(emptyList())
    val groupedMedia = _groupedMedia.asStateFlow()

    fun search(
        search: String,
        context: Context
    ) = viewModelScope.launch(Dispatchers.IO) {
        val query = search.trim()
        this@SearchViewModel.query = query

        if (query.isEmpty()) {
            _groupedMedia.value = groupPhotosBy(
                media = mediaFlow.value,
                sortBy = sortMode,
                displayDateFormat = displayDateFormat,
                context = context
            )
            return@launch
        }

        var final = searchByDateFormat(query = query)

        if (final.isEmpty()) {
            final = searchByDateNames(query = query)
        }

        if (final.isEmpty()) {
            final = searchByName(name = query)
        }

        setMedia(
            context = context,
            media = final,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    }

    // TODO: fix
    /** [setMedia] but without grouping */
    fun overrideMedia(
        context: Context,
        media: List<MediaStoreData>
    ) {
        _groupedMedia.value = groupPhotosBy(
            media = media,
            sortBy = sortMode,
            displayDateFormat = displayDateFormat,
            context = context
        )
    }

    fun setMedia(
        context: Context,
        media: List<MediaStoreData>,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = viewModelScope.launch(Dispatchers.IO) {
        this@SearchViewModel.sortMode = sortMode
        this@SearchViewModel.displayDateFormat = displayDateFormat

        _groupedMedia.value = groupPhotosBy(
            media = media,
            sortBy = sortMode,
            displayDateFormat = displayDateFormat,
            context = context
        )
    }

    fun clear() {
        _groupedMedia.value = emptyList()
        cancellationSignal.cancel()
        cancellationSignal = CancellationSignal()
    }

    fun restart(
        context: Context,
        sortMode: MediaItemSortMode = this.sortMode,
        displayDateFormat: DisplayDateFormat = this.displayDateFormat
    ) {
        this.sortMode = sortMode
        this.displayDateFormat = displayDateFormat

        mediaStoreDataSource.value = MediaDataSource(
            context = context,
            sqliteQuery = SQLiteQuery(query = "", paths = null, basePaths = null),
            sortMode = sortMode,
            cancellationSignal = cancellationSignal,
            displayDateFormat = displayDateFormat
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