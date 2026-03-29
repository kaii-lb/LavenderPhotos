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
import com.kaii.photos.file_management.HybridFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.ListPagingSource
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
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
) {
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

    private var fileManager = HybridFileManager(
        mediaDao = mediaDao,
        customDao = customDao,
        syncTaskDao = syncTaskDao,
        assetClient = AssetsClient(
            baseUrl = "",
            client = client
        ),
        albumsClient = AlbumsClient(
            baseUrl = "",
            client = client
        ),
        info = ImmichBasicInfo.Empty
    )

    init {
        scope.launch {
            params.mapLatest { it.info }
                .distinctUntilChanged()
                .collectLatest { info ->
                    fileManager = HybridFileManager(
                        mediaDao = mediaDao,
                        customDao = customDao,
                        syncTaskDao = syncTaskDao,
                        assetClient = AssetsClient(
                            baseUrl = info.endpoint,
                            client = client
                        ),
                        albumsClient = AlbumsClient(
                            baseUrl = info.endpoint,
                            client = client
                        ),
                        info = info
                    )
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
                        searchByDate(query = details.query)
                    }

                    else -> {
                        searchDao.getAll(dateModified = details.sortMode.isDateModified)
                    }
                }
            }
        ).flow.mapToMedia(accessToken = details.info.accessToken)
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

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = fileManager.getExifData(context, media)

    fun allowedAlbumTypesFor(
        moving: Boolean
    ) = fileManager.allowedAlbumTypesFor(
        moving = moving,
        current = AlbumType.Folder::class
    )

    suspend fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        overrideDisplayName: ((displayName: String) -> String)?,
        onItemDone: (totaCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.copyItems(context, list, destination, preserveDate, overrideDisplayName) {
            count += 1
            onItemDone(count)
        }.size == list.size
    }

    suspend fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType,
        preserveDate: Boolean,
        onItemDone: (totalCount: Int) -> Unit
    ): Boolean {
        var count = 0

        return fileManager.moveItems(context, list, destination, preserveDate) {
            count += 1
            onItemDone(count)
        }
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = fileManager.renameItem(context, uri, newName)

    suspend fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean,
        onItemDone: (totaCount: Int) -> Unit
    ) = fileManager.setTrashed(context, list, trashed, null, null, onItemDone)

    suspend fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.permanentlyDelete(context, list)

    suspend fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ) = fileManager.setFavourite(context, favourite, list)

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

        return searchDao.searchBetweenDates(
            startDate = start.epochSeconds,
            endDate = start.plus(1.days).epochSeconds,
            dateModified = params.value.sortMode.isDateModified
        )
    }

    private fun searchByYear(query: String): PagingSource<Int, MediaStoreData> {
        return query.trim().toIntOrNull()?.let { yearDate ->
            val year = YearMonth(year = yearDate, month = 1)

            searchDao.searchBetweenDates(
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
            searchDao.searchByMonth(month = month, dateModified = dateModified)
        } else {
            ListPagingSource(media = emptyList())
        }
    }

    private fun searchByDay(query: String): PagingSource<Int, MediaStoreData> {
        val dateModified = params.value.sortMode.isDateModified

        val search = query.trim().uppercase()

        return if (search in DayOfWeek.entries.fastMap { it.name }) {
            val day = DayOfWeek.valueOf(search).isoDayNumber.toString()
            searchDao.searchByDay(day = day, dateModified = dateModified)
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

        return searchDao.searchBetweenDates(
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

        return searchDao.searchBetweenDates(
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