package com.kaii.photos.repositories

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plusMonth
import kotlinx.datetime.plusYear
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

private const val TAG = "com.kaii.photos.repositories.SearchRepository"

enum class SearchMode(
    @param:StringRes val nameId: Int
) {
    Name(nameId = R.string.search_by_name),
    Date(nameId = R.string.search_by_date)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepository(
    info: ImmichBasicInfo,
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private data class RoomQueryParams(
        val query: String,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val accessToken: String,
        val mode: SearchMode
    )

    private val dao = MediaDatabase.getInstance(context.applicationContext).searchDao()

    private val params = MutableStateFlow(
        value = RoomQueryParams(
            query = "",
            sortMode = sortMode,
            format = format,
            accessToken = info.accessToken,
            mode = SearchMode.Name
        )
    )

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val mediaFlow = params.flatMapLatest { details ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 300
            ),
            pagingSourceFactory = {
                when {
                    details.query.isBlank() -> {
                        dao.getAll(dateModified = details.sortMode.isDateModified)
                    }

                    details.mode == SearchMode.Name -> {
                        dao.searchByName(query = "%${details.query}%", dateModified = details.sortMode.isDateModified)
                    }

                    details.mode == SearchMode.Date -> {
                        searchByDate(query = details.query)
                    }

                    else -> {
                        dao.getAll(dateModified = details.sortMode.isDateModified)
                    }
                }
            }
        ).flow.mapToMedia(accessToken = details.accessToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }

    fun search(
        query: String
    ) {
        _query.value = query
        params.value = params.value.copy(query = query)
    }

    fun update(
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        accessToken: String?,
        mode: SearchMode?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            accessToken = accessToken ?: snapshot.accessToken,
            mode = mode ?: snapshot.mode
        )
    }

    private fun searchByDate(query: String): PagingSource<Int, MediaStoreData> {
        var source = searchByDateFormat(query)

        if (source::class == ListPagingSource::class) {
            source = searchByDayNumberMonthYear(query)
        }

        if (source::class == ListPagingSource::class) {
            source = searchByDayMonthYear(query)
        }

        if (source::class == ListPagingSource::class) {
            source = searchByYearMonth(query)
        }

        if (source::class == ListPagingSource::class) {
            source = searchByYear(query)
        }

        if (source::class == ListPagingSource::class) {
            source = searchByMonth(query)
        }

        if (source::class == ListPagingSource::class) {
            source = searchByDay(query)
        }

        return source
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

    @OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
    private fun searchByDateFormat(query: String): PagingSource<Int, MediaStoreData> {
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

        if (parsed == null) return ListPagingSource(media = emptyList())

        val start = LocalDate(parsed.year, parsed.month, parsed.day).atStartOfDayIn(TimeZone.currentSystemDefault())

        return dao.searchBetweenDates(
            startDate = start.epochSeconds,
            endDate = start.plus(1.days).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByYear(query: String): PagingSource<Int, MediaStoreData> {
        return query.trim().toIntOrNull()?.let { yearDate ->
            val year = YearMonth(year = yearDate, month = 1)

            dao.searchBetweenDates(
                startDate = year.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
                endDate = year.plusYear().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
                dateModified = params.value.sortMode.isDateModified
            )
        } ?: ListPagingSource(media = emptyList())
    }

    private fun searchByMonth(query: String): PagingSource<Int, MediaStoreData> {
        val dateModified = params.value.sortMode.isDateModified

        val search = query.trim().uppercase()

        return if (search in Month.entries.fastMap { it.name }) {
            val month = Month.valueOf(search).number.toString().padStart(2, '0')
            dao.searchByMonth(month = month, dateModified = dateModified)
        } else {
            ListPagingSource(media = emptyList())
        }
    }

    private fun searchByDay(query: String): PagingSource<Int, MediaStoreData> {
        val dateModified = params.value.sortMode.isDateModified

        val search = query.trim().uppercase()

        return if (search in DayOfWeek.entries.fastMap { it.name }) {
            val day = DayOfWeek.valueOf(search).isoDayNumber.toString()
            dao.searchByDay(day = day, dateModified = dateModified)
        } else {
            ListPagingSource(media = emptyList())
        }
    }

    private fun searchByYearMonth(query: String): PagingSource<Int, MediaStoreData> {
        val search = query.trim().uppercase().split(" ")
        val emptySource = ListPagingSource(media = emptyList())

        if (search.size < 2) return emptySource

        val month = search[0]
        val year = search[1].toIntOrNull()

        if (year == null || month !in Month.entries.fastMap { it.name }) return emptySource

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        return dao.searchBetweenDates(
            startDate = yearMonth.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            endDate = yearMonth.plusMonth().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByDayNumberMonthYear(query: String): PagingSource<Int, MediaStoreData> {
        val search = query.trim().uppercase().split(" ")
        val emptySource = ListPagingSource(media = emptyList())

        if (search.size < 3) return emptySource

        val day = search[0].toIntOrNull()
        val month = search[1]
        val year = search[2].toIntOrNull()

        if (day == null || year == null || month !in Month.entries.fastMap { it.name }) return emptySource

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        if (day < yearMonth.days.start.day || day > yearMonth.days.endInclusive.day) return emptySource

        val start = yearMonth.onDay(day).atStartOfDayIn(TimeZone.currentSystemDefault())

        return dao.searchBetweenDates(
            startDate = start.epochSeconds,
            endDate = start.plus(1.days).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByDayMonthYear(query: String): PagingSource<Int, MediaStoreData> {
        val search = query.trim().uppercase().split(" ")
        val emptySource = ListPagingSource(media = emptyList())

        if (search.size < 3) return emptySource

        val dayName = search[0]
        val month = search[1]
        val year = search[2].toIntOrNull()

        if (year == null || month !in Month.entries.fastMap { it.name } || dayName !in DayOfWeek.entries.fastMap { it.name }) return emptySource

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        return dao.searchForDaysInMonthYear(
            startDate = yearMonth.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            endDate = yearMonth.plusMonth().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            day = DayOfWeek.valueOf(dayName).isoDayNumber.toString(),
            dateModified = params.value.sortMode.isDateModified
        )
    }
}