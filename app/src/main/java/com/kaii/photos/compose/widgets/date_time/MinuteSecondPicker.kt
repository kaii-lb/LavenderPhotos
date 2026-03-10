package com.kaii.photos.compose.widgets.date_time

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.CircularSelectingList

@Preview
@Composable
private fun MinuteSecondPickerPreview() {
    MinuteSecondPicker(
        initialIndex = 30,
        modifier = Modifier
            .width(80.dp),
        set = {}
    )
}

@Composable
fun MinuteSecondPicker(
    initialIndex: Int,
    modifier: Modifier = Modifier,
    set: (day: Int) -> Unit
) {
    CircularSelectingList(
        initialItemIndex = initialIndex,
        items = {
            (0..59).map {
                it.toString().padStart(
                    length = 2,
                    padChar = '0'
                )
            }
        },
        modifier = modifier,
        setSelectedIndex = set
    )
}