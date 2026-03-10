package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.CircularSelectingList
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

@Preview
@Composable
private fun DayPickerPreview() {
    DayPicker(
        initialDay = 1,
        month = YearMonth(year = 2026, month = Month.JANUARY),
        modifier = Modifier
            .width(80.dp),
        setSelectedDay = {}
    )
}

@Composable
fun DayPicker(
    initialDay: Int,
    month: YearMonth,
    modifier: Modifier = Modifier,
    setSelectedDay: (day: Int) -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = month.days.size * 100 / 2 - 2 + initialDay - 1,
        initialFirstVisibleItemScrollOffset =
            with(density) { 24.dp.roundToPx() }
    )

    var selectedDay by remember { mutableIntStateOf(initialDay) }
    LaunchedEffect(month) {
        val targetDay = selectedDay.coerceAtMost(month.numberOfDays)

        if (targetDay != selectedDay) {
            setSelectedDay(targetDay)
        }

        val center = (month.numberOfDays * 100) / 2
        val targetIndex = center + targetDay - 1 - 2

        listState.scrollToItem(
            index = targetIndex,
            scrollOffset = with(density) { 24.dp.roundToPx() }
        )
    }

    CircularSelectingList(
        initialItemIndex = initialDay - 1,
        items = {
            month.days.map {
                it.day.toString().padStart(
                    length = 2,
                    padChar = '0'
                )
            }
        },
        listState = listState,
        modifier = modifier,
        setSelectedIndex = {
            val day = month.days.toList()[it].day
            selectedDay = day
            setSelectedDay(day)
        }
    )
}