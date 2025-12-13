package com.kaii.photos.helpers

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun formatDate(timestamp: Long, sortBy: MediaItemSortMode, format: DisplayDateFormat): String {
    return if (timestamp != 0L) {
        val dateFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) {
                DateTimeFormatter.ofPattern("MMMM yyyy")
            } else {
                format.format
            }

        Instant.fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toJavaLocalDate()
            .format(dateFormat)
    } else {
        "Pretend there is a date here"
    }
}

enum class DisplayDateFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: DateTimeFormatter
) {
    Default(
        description = R.string.look_and_feel_date_format_default,
        icon = R.drawable.calendar_filled,
        format = DateTimeFormatter.ofPattern("EEE dd - MMM yyyy")
    ),

    Alternate(
        description = R.string.look_and_feel_date_format_alternate,
        icon = R.drawable.nest_clock_farsight_analog,
        format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    ),

    Short(
        description = R.string.look_and_feel_date_format_short,
        icon = R.drawable.clarify,
        format = DateTimeFormatter.ofPattern("MMM dd yyyy")
    ),

    Compressed(
        description = R.string.look_and_feel_date_format_compressed,
        icon = R.drawable.acute,
        format = DateTimeFormatter.ISO_LOCAL_DATE
    )
}

@OptIn(ExperimentalTime::class)
enum class TopBarDetailsFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: (Context, String, Long) -> String
) {
    Default(
        description = R.string.look_and_feel_date_format_default,
        icon = R.drawable.nest_clock_farsight_analog,
        format = { context, name, datetime ->
            name
        }
    ),

    Compressed(
        description = R.string.look_and_feel_date_format_compressed,
        icon = R.drawable.acute,
        format = { context, name, datetime ->
            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
        },
    ),

    Detailed(
        description = R.string.look_and_feel_date_format_detailed,
        icon = R.drawable.clarify,
        format = { context, name, datetime ->
            val is24Hr = android.text.format.DateFormat.is24HourFormat(context)

            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (is24Hr) "EEE dd MMM yyyy HH:mm"
                        else "EEE dd MMM yyyy hh:mm a"
                    )
                )
        }
    )
}