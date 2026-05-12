package com.kaii.photos.repositories

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.util.fastMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.kaii.photos.R
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SearchDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.file_management.managers.HybridFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
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
    Date(nameId = R.string.search_by_date),
    Tag(nameId = R.string.search_by_tag)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepository(
    private val searchDao: SearchDao,
    private val taggedItemsDao: TaggedItemsDao,
    scope: CoroutineScope,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat,
    client: ApiClient,
    mediaDao: MediaDao,
    customDao: CustomEntityDao,
    syncTaskDao: SyncTaskDao
) : BaseRepo {
    private data class RoomQueryParams(
        val query: String,
        val sortMode: MediaItemSortMode,
        val format: DisplayDateFormat,
        val info: ImmichBasicInfo,
        val mode: SearchMode,
        val tags: Set<Tag>
    )

    private val params = MutableStateFlow(
        value = RoomQueryParams(
            query = "",
            sortMode = sortMode,
            format = format,
            info = info,
            mode = SearchMode.Name,
            tags = emptySet()
        )
    )

    override val fileManager = HybridFileManager(
        isCustom = false,
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = syncTaskDao,
        assetClient = AssetsClient(
            endpoint = "",
            auth = Auth.None,
            client = client
        ),
        albumsClient = AlbumsClient(
            endpoint = "",
            auth = Auth.None,
            client = client
        )
    )

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager.setEndpoint(info.endpoint)
                    fileManager.setAuth(info.auth)
                }
        }
    }

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
                    // show all tagged media
                    details.mode == SearchMode.Tag -> {
                        searchByTags(details.query, details.tags)
                    }

                    details.query.isBlank() -> {
                        searchDao.getAll(dateModified = details.sortMode.isDateModified)
                    }

                    details.mode == SearchMode.Name -> {
                        searchDao.searchByName(query = "%${details.query}%", dateModified = details.sortMode.isDateModified)
                    }

                    details.mode == SearchMode.Date -> {
                        searchByDate(query = details.query) ?: ListPagingSource(media = emptyList())
                    }

                    else -> {
                        searchDao.getAll(dateModified = details.sortMode.isDateModified)
                    }
                }
            }
        ).flow.mapToMedia(
            auth = details.info.auth,
            endpoint = details.info.endpoint
        )
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
        tags: Set<Tag>
    ) {
        if (params.value.mode == SearchMode.Tag) {
            params.value = params.value.copy(query = query, tags = tags)
        } else {
            params.value = params.value.copy(query = query, tags = emptySet())
        }
    }

    fun update(
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        info: ImmichBasicInfo?,
        mode: SearchMode?
    ) {
        val snapshot = params.value
        params.value = snapshot.copy(
            sortMode = sortMode ?: snapshot.sortMode,
            format = format ?: snapshot.format,
            info = info ?: snapshot.info,
            mode = mode ?: snapshot.mode
        )
    }

    override fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Folder::class
    )

    override suspend fun getMediaCount(): Int {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    override suspend fun getMediaSize(): Long {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    override suspend fun renameAlbum(context: Context, newName: String) {
        throw IllegalAccessException("This cannot and should not be called in a search context.")
    }

    private fun searchByDate(query: String): PagingSource<Int, MediaStoreData>? {
        var source = searchByDateFormat(query)

        if (source == null) {
            source = searchByDayNumberMonthYear(query)
        }

        if (source == null) {
            source = searchByDayMonthYear(query)
        }

        if (source == null) {
            source = searchByYearMonth(query)
        }

        if (source == null) {
            source = searchByYear(query)
        }

        if (source == null) {
            source = searchByMonth(query)
        }

        if (source == null) {
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
    private fun searchByDateFormat(query: String): PagingSource<Int, MediaStoreData>? {
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

        if (parsed == null) return null

        val start = LocalDate(parsed.year, parsed.month, parsed.day).atStartOfDayIn(TimeZone.currentSystemDefault())

        return searchDao.searchBetweenDates(
            startDate = start.epochSeconds,
            endDate = start.plus(1.days).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByYear(query: String): PagingSource<Int, MediaStoreData>? {
        return query.trim().toIntOrNull()?.let { yearDate ->
            val year = YearMonth(year = yearDate, month = 1)

            searchDao.searchBetweenDates(
                startDate = year.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
                endDate = year.plusYear().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
                dateModified = params.value.sortMode.isDateModified
            )
        }
    }

    private fun searchByMonth(query: String): PagingSource<Int, MediaStoreData>? {
        val dateModified = params.value.sortMode.isDateModified

        val search = query.trim().uppercase()

        return Month.entries
            .find { it.name == search }
            ?.let { month ->
                val number = month.number.toString().padStart(2, '0')
                searchDao.searchByMonth(month = number, dateModified = dateModified)
            }
    }

    private fun searchByDay(query: String): PagingSource<Int, MediaStoreData>? {
        val dateModified = params.value.sortMode.isDateModified

        val search = query.trim().uppercase()

        return DayOfWeek.entries
            .find { it.name == search }
            ?.let { day ->
                val number = (day.isoDayNumber % 7).toString() // %6 since sunday = 0
                searchDao.searchByDay(day = number, dateModified = dateModified)
            }
    }

    private fun searchByYearMonth(query: String): PagingSource<Int, MediaStoreData>? {
        val search = query.trim().uppercase().split(" ")

        if (search.size < 2) return null

        val month = search[0]
        val year = search[1].toIntOrNull()

        if (year == null || month !in Month.entries.fastMap { it.name }) return null

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        return searchDao.searchBetweenDates(
            startDate = yearMonth.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            endDate = yearMonth.plusMonth().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByDayNumberMonthYear(query: String): PagingSource<Int, MediaStoreData>? {
        val search = query.trim().uppercase().split(" ")

        if (search.size < 3) return null

        val day = search[0].toIntOrNull()
        val month = search[1]
        val year = search[2].toIntOrNull()

        if (day == null || year == null || month !in Month.entries.fastMap { it.name }) return null

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        if (day < yearMonth.days.start.day || day > yearMonth.days.endInclusive.day) return null

        val start = yearMonth.onDay(day).atStartOfDayIn(TimeZone.currentSystemDefault())

        return searchDao.searchBetweenDates(
            startDate = start.epochSeconds,
            endDate = start.plus(1.days).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByDayMonthYear(query: String): PagingSource<Int, MediaStoreData>? {
        val search = query.trim().uppercase().split(" ")

        if (search.size < 3) return null

        val dayName = search[0]
        val month = search[1]
        val year = search[2].toIntOrNull()

        if (year == null || month !in Month.entries.fastMap { it.name } || dayName !in DayOfWeek.entries.fastMap { it.name }) return null

        val yearMonth = YearMonth(
            year = year,
            month = Month.valueOf(month).number
        )

        return searchDao.searchForDaysInMonthYear(
            startDate = yearMonth.onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            endDate = yearMonth.plusMonth().onDay(1).atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds,
            day = DayOfWeek.valueOf(dayName).isoDayNumber.toString(),
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByTags(
        query: String,
        tags: Set<Tag>
    ): PagingSource<Int, MediaStoreData> {
        val dateModified = params.value.sortMode.isDateModified

        val tagIds = tags.map { it.id }

        return when {
            query.isBlank() && tags.isNotEmpty() && dateModified -> taggedItemsDao.getAllInTagsDateModified(tags = tagIds, tagCount = tagIds.size)

            query.isBlank() && tags.isNotEmpty() -> taggedItemsDao.getAllInTagsDateTaken(tags = tagIds, tagCount = tagIds.size)

            query.isNotBlank() && tags.isNotEmpty() && dateModified -> taggedItemsDao.searchInTagsDateModified(
                query = "%$query%",
                tags = tagIds,
                tagCount = tagIds.size
            )

            query.isNotBlank() && tags.isNotEmpty() -> taggedItemsDao.searchInTagsDateTaken(query = "%$query%", tags = tagIds, tagCount = tagIds.size)

            query.isNotBlank() && tags.isEmpty() && !dateModified -> taggedItemsDao.searchDateModified("%$query%")

            query.isNotBlank() && tags.isEmpty() -> taggedItemsDao.searchDateTaken("%$query%")

            else -> {
                if (dateModified) taggedItemsDao.getAllDateModified()
                else taggedItemsDao.getAllDateTaken()
            }
        }
    }
}