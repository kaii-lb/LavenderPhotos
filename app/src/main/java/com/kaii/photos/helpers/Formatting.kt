package com.kaii.photos.helpers

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
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
        icon = R.drawable.event_filled,
        format = DateTimeFormatter.ofPattern("EEE dd - MMM yyyy")
    ),

    Numeric(
        description = R.string.look_and_feel_date_format_numeric,
        icon = R.drawable.schedule_filled,
        format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    ),

    Short(
        description = R.string.look_and_feel_date_format_short,
        icon = R.drawable.acute,
        format = DateTimeFormatter.ofPattern("dd MMM yyyy")
    ),

    Standard(
        description = R.string.look_and_feel_date_format_standard,
        icon = R.drawable.article_filled,
        format = DateTimeFormatter.ISO_LOCAL_DATE
    )
}

@OptIn(ExperimentalTime::class)
enum class TopBarDetailsFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: (Context, String, Long) -> String
) {
    FileName(
        description = R.string.look_and_feel_date_format_filename,
        icon = R.drawable.clarify,
        format = { context, name, datetime ->
            name
        }
    ),

    Date(
        description = R.string.look_and_feel_date_format_date_taken,
        icon = R.drawable.acute,
        format = { context, name, datetime ->
            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
        },
    ),

    DateTime(
        description = R.string.look_and_feel_date_format_datetime_taken,
        icon = R.drawable.overview_filled,
        format = { context, name, datetime ->
            val is24Hr = android.text.format.DateFormat.is24HourFormat(context)

            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (is24Hr) "EEE dd MMM yyyy HH:mm"
                        else "EEE dd MMM yyyy h:mm a"
                    )
                )
        }
    )
}