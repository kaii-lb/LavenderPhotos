package com.kaii.photos.models.search_page

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
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

private data class SearchParams(
    val items: List<MediaStoreData>,
    val accessToken: String
)

class SearchViewModel(
    context: Context,
    private var sortMode: MediaItemSortMode,
    private var displayDateFormat: DisplayDateFormat,
    private val separators: Boolean
) : ViewModel() {
    private var query = ""

    private val mediaDao = MediaDatabase.getInstance(context).mediaDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allMedia =
        mediaDao.getAllMedia()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    private val params = MutableStateFlow(
        value = SearchParams(
            items = emptyList(),
            accessToken = ""
        )
    )
    private val settings = SettingsImmichImpl(context = context, viewModelScope = viewModelScope)

    init {
        viewModelScope.launch {
            settings.getImmichBasicInfo().collectLatest {
                params.value = params.value.copy(accessToken = it.accessToken)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val mediaFlow = params.debounce(1.seconds).flatMapLatest { (media, accessToken) ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 300
            ),
            pagingSourceFactory = { ListPagingSource(media = media) }
        ).flow.mapToMedia(
            sortMode = sortMode,
            format = displayDateFormat,
            accessToken = accessToken,
            separators = separators
        )
    }.cachedIn(viewModelScope)

    fun search(
        query: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val query = query.trim()
        this@SearchViewModel.query = query

        if (query.isEmpty()) {
            params.value = params.value.copy(items = allMedia.value)
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
            media = final,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    }

    fun setMedia(
        media: List<MediaStoreData>,
        sortMode: MediaItemSortMode,
        displayDateFormat: DisplayDateFormat
    ) = viewModelScope.launch(Dispatchers.IO) {
        this@SearchViewModel.sortMode = sortMode
        this@SearchViewModel.displayDateFormat = displayDateFormat

        params.value = params.value.copy(items = media)
    }

    fun clear() {
        params.value = params.value.copy(items = emptyList())
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
        val list = allMedia.value

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

        val list = allMedia.value
        return list.filter { data ->
            val date = data.getDateTakenDay().toLocalDate()

            data.type != MediaType.Section && date.day == parsed.day && date.month == parsed.month && date.year == parsed.year
        }
    }

    fun searchByDateNames(query: String): List<MediaStoreData> {
        val unpacked = query.split(" ")
        if (unpacked.size !in 2..3) return emptyList()

        try {
            val list = allMedia.value

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
    private fun Long.toLocalDate() = Instant.fromEpochSeconds(
        epochSeconds = this
    ).toLocalDateTime(
        timeZone = TimeZone.currentSystemDefault()
    ).date
}

