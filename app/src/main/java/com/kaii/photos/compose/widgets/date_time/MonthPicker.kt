package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.CircularSelectingList
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Preview
@Composable
private fun MonthPickerPreview() {
    MonthPicker(
        initialMonth = YearMonth(year = 2026, month = Month.DECEMBER),
        modifier = Modifier
            .width(128.dp),
        setSelectedMonth = {}
    )
}

@Composable
fun MonthPicker(
    initialMonth: YearMonth,
    modifier: Modifier = Modifier,
    setSelectedMonth: (month: Month) -> Unit
) {
    val locale = LocalLocale.current.platformLocale

    CircularSelectingList(
        initialItemIndex = Month.entries.indexOf(initialMonth.month),
        items = {
            Month.entries.map {
                LocalDate.of(
                    initialMonth.year,
                    it.number,
                    1
                ).format(
                    DateTimeFormatter.ofPattern("MMMM").withLocale(locale)
                )
            }
        },
        modifier = modifier,
        setSelectedIndex = {
            setSelectedMonth(Month.entries[it])
        }
    )
}