package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.CircularSelectingList

@Preview
@Composable
private fun YearPickerPreview() {
    YearPicker(
        initialYear = 2026,
        modifier = Modifier
            .width(80.dp),
        setSelectedYear = {}
    )
}

@Composable
fun YearPicker(
    initialYear: Int,
    modifier: Modifier = Modifier,
    setSelectedYear: (day: Int) -> Unit
) {
    CircularSelectingList(
        initialItemIndex = (1970..2038).indexOf(initialYear),
        items = {
            (1970..2038).map {
                it.toString()
            }
        },
        modifier = modifier,
        setSelectedIndex = {
            setSelectedYear(1970 + it)
        }
    )
}