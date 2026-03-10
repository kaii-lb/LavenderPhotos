package com.kaii.photos.compose.widgets.date_time

import android.text.format.DateFormat
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.CircularSelectingList

@Preview
@Composable
private fun HourPickerPreview() {
    HourPicker(
        initialHour = 1,
        modifier = Modifier
            .width(96.dp),
        setHour = {}
    )
}

@Composable
fun HourPicker(
    initialHour: Int,
    modifier: Modifier = Modifier,
    setHour: (day: Int) -> Unit
) {
    val is24Hr = DateFormat.is24HourFormat(LocalContext.current)

    CircularSelectingList(
        initialItemIndex = initialHour,
        items = {
            (0..23).map { hour ->
                val formatted =
                    if (is24Hr) hour
                    else (hour % 12).takeIf { it != 0 } ?: 12

                val suffix =
                    if (!is24Hr) if (hour in 0..11) "a.m." else "p.m."
                    else ""

                formatted.toString().padStart(
                    length = 2,
                    padChar = '0'
                ) + " " + suffix
            }
        },
        modifier = modifier,
        setSelectedIndex = setHour
    )
}