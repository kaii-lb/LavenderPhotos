package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.widgets.DateTimePickerState
import com.kaii.photos.widgets.rememberDateTimePickerState
import kotlinx.datetime.yearMonth

@Preview
@Composable
private fun DatePickerPreview() {
    DatePicker(
        state = rememberDateTimePickerState(mediaItem = MediaStoreData.dummyItem)
    )
}

@Composable
fun DatePicker(
    state: DateTimePickerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(48.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        DayPicker(
            initialDay = state.initialDateTime.day,
            month = state.date.yearMonth,
            modifier = Modifier
                .weight(0.5f),
            setSelectedDay = state::setDay
        )

        MonthPicker(
            initialMonth = state.initialDateTime.date.yearMonth,
            modifier = Modifier
                .weight(1f),
            setSelectedMonth = state::setMonth
        )

        YearPicker(
            initialYear = state.initialDateTime.year,
            modifier = Modifier
                .weight(0.6f),
            setSelectedYear = state::setYear
        )
    }
}