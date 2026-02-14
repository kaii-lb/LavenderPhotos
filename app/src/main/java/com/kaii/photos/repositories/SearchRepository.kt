package com.kaii.photos.repositories

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.ListPagingSource
import com.kaii.photos.models.loading.mapToMedia
import com.kaii.photos.models.loading.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
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

private const val TAG = "com.kaii.photos.repositories.SearchRepository"

// TODO: try to use pure sqlite queries for filtering so we can immediately use paging, instead of loading everything still
@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepository(
    private val scope: CoroutineScope,
    info: ImmichBasicInfo,
    context: Context,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) {
    private data class RoomQueryParams(
        val items: List<MediaStoreData>,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val accessToken: String
    )

    private var query = ""
    private val mediaDao = MediaDatabase.getInstance(context.applicationContext).mediaDao()

    private val params = MutableStateFlow(
        value = RoomQueryParams(
            items = emptyList(),
            sortMode = sortMode,
            format = format,
            accessToken = info.accessToken
        )
    )

    private var allMedia = emptyList<MediaStoreData>()

    init {
        scope.launch {
            params.mapLatest { it.sortMode.isDateModified }.distinctUntilChanged().flatMapLatest { isDateModified ->
                if (isDateModified) mediaDao.getAllMediaDateModified()
                else mediaDao.getAllMediaDateTaken()
            }.collectLatest {
                allMedia = it
                search(query, true)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val mediaFlow = params.debounce(1.seconds).flatMapLatest { details ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 300
            ),
            pagingSourceFactory = { ListPagingSource(media = details.items) }
        ).flow.mapToMedia(accessToken = details.accessToken)
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            format = details.format
        )
    }.cachedIn(scope)

    fun search(
        query: String,
        ignoreSameQueryCheck: Boolean = false
    ) = scope.launch(Dispatchers.IO) {
        val query = query.trim()
        if (query.isBlank()) {
            this@SearchRepository.query = query
            params.value = params.value.copy(items = allMedia)
            return@launch
        }

        if (this@SearchRepository.query == query && !ignoreSameQueryCheck) return@launch
        this@SearchRepository.query = query

        var final = searchByDateFormat(query = query)

        if (final.isEmpty()) {
            final = searchByDateNames(query = query)
        }

        if (final.isEmpty()) {
            final = searchByName(name = query)
        }

        params.value = params.value.copy(items = final)
    }

    fun update(
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        accessToken: String?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            accessToken = accessToken ?: snapshot.accessToken
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

    private fun searchByName(name: String): List<MediaStoreData> {
        return allMedia.filter { data ->
            data.type != MediaType.Section && data.displayName.lowercase().contains(name.lowercase())
        }
    }

    @OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)
    private fun searchByDateFormat(query: String): List<MediaStoreData> {
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

        return allMedia.filter { data ->
            val date = data.getDateTakenDay().toLocalDate()

            data.type != MediaType.Section && date.day == parsed.day && date.month == parsed.month && date.year == parsed.year
        }
    }

    private fun searchByDateNames(query: String): List<MediaStoreData> {
        val unpacked = query.split(" ")
        if (unpacked.size !in 2..3) return emptyList()

        try {
            // year
            if (unpacked[0].lowercase() == "#year:" && unpacked[1].toIntOrNull() != null && unpacked[1].length == 4 && unpacked.size == 2) {
                val year = unpacked[1].toInt()

                return allMedia.filter { data ->
                    val date = data.getDateTakenDay().toLocalDate()

                    date.year == year
                }
            }

            // month
            if (unpacked[0].lowercase() == "#month:" && Month.entries.indexOfFirst { it.name == unpacked[1].uppercase() } != -1 && unpacked.size == 2) {
                val month = Month.valueOf(unpacked[1].uppercase()).number

                return allMedia.filter { data ->
                    val date = data.getDateTakenDay().toLocalDate()

                    date.month.number == month
                }
            }

            // day
            if (unpacked[0].lowercase() == "#day:" && DayOfWeek.entries.indexOfFirst { it.name == unpacked[1].uppercase() } != -1 && unpacked.size == 2) {
                val day = DayOfWeek.valueOf(unpacked[1].uppercase()).isoDayNumber

                return allMedia.filter { data ->
                    val date = data.getDateTakenDay().toLocalDate()

                    date.dayOfWeek.isoDayNumber == day
                }
            }

            // dayNumber month year
            if (unpacked[0].length <= 2) {
                val month = Month.valueOf(unpacked[1].uppercase()).number
                val year = unpacked.getOrNull(2)?.toIntOrNull()

                unpacked[0].toIntOrNull()?.let { day ->
                    return allMedia.filter { data ->
                        val date = data.getDateTakenDay().toLocalDate()

                        data.type != MediaType.Section && date.day == day && date.month.number == month && year?.let { date.year == year } ?: true
                    }
                }
            }

            // month year
            if (unpacked[0].length > 2 && unpacked[1].toIntOrNull() != null) {
                val month = Month.valueOf(unpacked[0].uppercase()).number
                val year = unpacked[1].toIntOrNull() ?: 0

                return allMedia.filter { data ->
                    val date = data.getDateTakenDay().toLocalDate()

                    data.type != MediaType.Section && date.month.number == month && date.year == year
                }
            }

            val day = DayOfWeek.valueOf(unpacked[0].uppercase()).isoDayNumber
            val month = Month.valueOf(unpacked[1].uppercase()).number
            val year = unpacked.getOrNull(2)?.toIntOrNull()

            println("DAY $day $month $year")

            return allMedia.filter { data ->
                val date = data.getDateTakenDay().toLocalDate()

                data.type != MediaType.Section && date.dayOfWeek.isoDayNumber == day && date.month.number == month && year?.let { date.year == year } ?: true
            }
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, e.toString())
            e.printStackTrace()

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