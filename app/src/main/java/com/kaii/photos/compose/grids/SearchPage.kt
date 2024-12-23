package com.kaii.photos.compose.grids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.photos.compose.SearchTextField
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SearchPage(
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<MainScreenViewType>
) {
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(LocalContext.current, MediaItemSortMode.DateTaken)
    )
    val mediaStoreDataHolder =
        searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val originalGroupedMedia = remember { mutableStateOf(mediaStoreDataHolder.value) }

    val groupedMedia = remember { mutableStateOf(originalGroupedMedia.value) }

    val gridState = rememberLazyGridState()

    BackHandler(
        enabled = currentView.value == MainScreenViewType.SearchPage && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        searchViewModel.cancelMediaFlow()
        currentView.value = MainScreenViewType.PhotosGridView
    }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background)
    ) {
    	val coroutineScope = rememberCoroutineScope()
		val scrollBackToTop = {
			coroutineScope.launch {
				gridState.animateScrollToItem(0)
			}
		}

        val searchedForText = rememberSaveable { mutableStateOf("") }
        var searchNow by rememberSaveable { mutableStateOf(false) }

        var hideLoadingSpinner by remember { mutableStateOf(false) }
        val showLoadingSpinner by remember {
            derivedStateOf {
                if (groupedMedia.value.isEmpty()) true else !hideLoadingSpinner
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val placeholdersList = remember {
            	val month = months.random().replaceFirstChar {
            			it.uppercase()
            		}
            	val day = days.random().replaceFirstChar {
            			it.uppercase()
            		}
            	val date = (1..31).random()
            	val year = (2016..2024).random()

	            listOf(
	                "Search for a photo's name",
	                "Search for a specific date",
	                "$month $date $year",
	                "$month $year",
	                "Search by day",
	                "$day $month $year",
	                "$date $month $year"
	            )
            }
            val placeholder = remember { placeholdersList.random() }

            SearchTextField(
                searchedForText = searchedForText,
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp, 0.dp),
                onSearch = {
                    if (!showLoadingSpinner) {
                        searchNow = true
                        scrollBackToTop()
                    }
                },
                onClear = {
                    searchedForText.value = ""
                    searchNow = true
                    scrollBackToTop()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(key1 = mediaStoreDataHolder.value) {
            originalGroupedMedia.value = mediaStoreDataHolder.value

            if (searchedForText.value == "") {
                groupedMedia.value = originalGroupedMedia.value
                hideLoadingSpinner = true
            }
        }

        LaunchedEffect(hideLoadingSpinner) {
            if (!hideLoadingSpinner) {
                delay(10000)
                hideLoadingSpinner = true
            }
        }

        LaunchedEffect(key1 = searchedForText.value) {
            if (searchedForText.value == "") {
                groupedMedia.value = originalGroupedMedia.value
                return@LaunchedEffect
            }

            hideLoadingSpinner = false

			coroutineScope.launch {
                val possibleDate = searchedForText.value.trim().toDateListOrNull()

                if (possibleDate.component1() != null) {
                    val local = originalGroupedMedia.value.filter {
                        it.type != MediaType.Section &&
                                (possibleDate.getOrNull(0)?.toDayLong()
                                    ?.let { date -> it.getDateTakenDay() == date } ?: false ||
                                        possibleDate.getOrNull(1)?.toDayLong()
                                            ?.let { date -> it.getDateTakenDay() == date } ?: false ||
                                        possibleDate.getOrNull(2)?.toDayLong()
                                            ?.let { date -> it.getDateTakenDay() == date } ?: false ||
                                        possibleDate.getOrNull(3)?.toDayLong()
                                            ?.let { date -> it.getDateTakenDay() == date } ?: false)
                    }

                    groupedMedia.value = groupPhotosBy(local, MediaItemSortMode.DateTaken)
		       		hideLoadingSpinner = true

                    return@launch
                }

                val onlyMonthYearSplit = searchedForText.value.trim().split(" ")
                if (onlyMonthYearSplit.size == 2) {
                    val month = months.firstOrNull { onlyMonthYearSplit[0] in it }
                    val year = onlyMonthYearSplit[1]

                    if (year.contains(Regex("[0-9]{4}")) && month != null && year.toIntOrNull() != null) {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year.toIntOrNull()!!)
                            set(Calendar.MONTH, months.indexOf(month))
                            set(Calendar.DAY_OF_MONTH, 0)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val local = originalGroupedMedia.value.filter {
                            it.type != MediaType.Section &&
                                    it.getDateTakenMonth() == calendar.timeInMillis / 1000
                        }

                        groupedMedia.value = groupPhotosBy(local, MediaItemSortMode.DateTaken)
			       		hideLoadingSpinner = true

                        return@launch
                    }
                }

                val groupedMediaLocal = originalGroupedMedia.value.filter {
                    val isMedia = it.type != MediaType.Section
                    val matchesFilter =
                        it.displayName?.contains(searchedForText.value.trim(), true) == true
                    isMedia && matchesFilter
                }

                groupedMedia.value = groupPhotosBy(groupedMediaLocal, MediaItemSortMode.DateTaken)
	       		hideLoadingSpinner = true
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                navController = navController,
                path = "",
                selectedItemsList = selectedItemsList,
                viewProperties = if (searchedForText.value == "") ViewProperties.SearchLoading else ViewProperties.SearchNotFound,
                state = gridState,
                modifier = Modifier
                    .align(Alignment.Center)
            )

            if (showLoadingSpinner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(48.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(1000.dp))
                            .background(CustomMaterialTheme.colorScheme.surfaceContainer),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp),
                            color = CustomMaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private val months = listOf(
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december"
)

private val days = listOf(
	"monday",
	"tuesday",
	"wednesday",
	"thursday",
	"friday",
	"saturday",
	"sunday"
)

private fun String.toDateListOrNull(): List<Date?> {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    dateFormat.isLenient = true

    val year = run {
        val split = this.split(" ")
        if (split.size == 3) {
            if (split[2].contains(Regex("[0-9]{4}"))) split[2].toIntOrNull()
            else null
        } else null
    }

    val month = months.firstOrNull {
        this.lowercase().split(" ").getOrElse(1) { "definitely is not a month" } in it
    }?.let {
        months.indexOf(it) + 1
    }

    if (year != null && month != null) {
        days.firstOrNull {
            this.lowercase().split(" ").getOrElse(0) { "definitely is not a day" } in it
        }?.let { weekDay ->
            var localDate = kotlinx.datetime.LocalDate(year, month, 1)

            val list = emptyList<Date?>().toMutableList()
            while (localDate.dayOfWeek != kotlinx.datetime.DayOfWeek.of(days.indexOf(weekDay) + 1) && localDate.month == kotlinx.datetime.Month.of(
                    month
                ) && localDate.year == year
            ) {
                localDate = localDate.plus(DatePeriod.parse("P0Y1D"))
            }
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 7}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 14}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )
            list.add(
                try {
                    dateFormat.parse("${localDate.dayOfMonth + 21}/$month/$year")
                } catch (_: Throwable) {
                    null
                }
            )

            return list
        }
    }

    val formats = listOf(
        "dd/MM/yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd MM yyyy",
        "dd MMM yyyy",
        "dd MMMM yyyy",
        "MM/dd/yyyy",
        "MM-dd-yyyy",
        "MM dd yyyy",
        "MMM dd yyyy",
        "MMMM dd yyyy"
    )

    for (format in formats) {
        val dateFormatter = SimpleDateFormat(format, Locale.getDefault())
        try {
            return listOf(dateFormatter.parse(this))
        } catch (_: Throwable) {
        }
    }
    return listOf(null)
}

private fun Date.toDayLong(): Long {
    val millis = this.time
    val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return calendar.timeInMillis / 1000
}
