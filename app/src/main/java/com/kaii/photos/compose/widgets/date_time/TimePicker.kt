package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.widgets.DateTimePickerState
import com.kaii.photos.widgets.rememberDateTimePickerState

@Preview
@Composable
private fun TimePickerPreview() {
    TimePicker(
        state = rememberDateTimePickerState(mediaItem = MediaStoreData.dummyItem)
    )
}

@Composable
fun TimePicker(
    state: DateTimePickerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(48.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        HourPicker(
            initialHour = state.initialDateTime.hour,
            modifier = Modifier
                .weight(1f),
            setHour = state::setHour
        )

        Text(
            text = ":",
            color = MaterialTheme.colorScheme.secondary,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                maxFontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp
            )
        )

        MinuteSecondPicker(
            initialIndex = state.initialDateTime.minute,
            modifier = Modifier
                .weight(1f),
            set = state::setMinute
        )

        Text(
            text = ":",
            color = MaterialTheme.colorScheme.secondary,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                maxFontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp
            )
        )

        MinuteSecondPicker(
            initialIndex = state.initialDateTime.second,
            modifier = Modifier
                .weight(1f),
            set = state::setSecond
        )
    }
}