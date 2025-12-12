package com.kaii.photos.helpers

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun formatDate(timestamp: Long, sortBy: MediaItemSortMode, format: DisplayDateFormat): String {
    return if (timestamp != 0L) {
        val dateFormat =
            if (sortBy == MediaItemSortMode.MonthTaken) {
                LocalDate.Companion.Format {
                    monthName(MonthNames.ENGLISH_FULL)
                    char(' ')
                    year()
                }
            } else {
                format.format
            }

        Instant.fromEpochSeconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .format(dateFormat)
    } else {
        "Pretend there is a date here"
    }
}

enum class DisplayDateFormat(
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val format: DateTimeFormat<LocalDate>
) {
    Default(
        description = R.string.look_and_feel_date_format_default,
        icon = R.drawable.calendar_filled,
        format = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            char(' ')
            day()

            char(' ')
            char('-')
            char(' ')

            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year()
        }
    ),

    Alternate(
        description = R.string.look_and_feel_date_format_alternate,
        icon = R.drawable.nest_clock_farsight_analog,
        format = LocalDate.Format {
            day()
            char('/')
            monthNumber()
            char('/')
            year()
        }
    ),

    Short(
        description = R.string.look_and_feel_date_format_short,
        icon = R.drawable.clarify,
        format = LocalDate.Format {
            day()
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            yearTwoDigits(1960)
        }
    ),

    Compressed(
        description = R.string.look_and_feel_date_format_compressed,
        icon = R.drawable.acute,
        format = LocalDate.Format {
            year()
            char('/')
            monthNumber()
            char('/')
            day()
        }
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
                .format(LocalDateTime.Format {
                    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                    char(' ')
                    day()
                    char(' ')
                    monthName(MonthNames.ENGLISH_ABBREVIATED)
                    char(' ')
                    year()
                })
        },
    ),

    Detailed(
        description = R.string.look_and_feel_date_format_detailed,
        icon = R.drawable.clarify,
        format = { context, name, datetime ->
            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(
                    LocalDateTime.Format {
                        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                        char(' ')
                        day()
                        char(' ')
                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                        char(' ')
                        year()

                        char(' ')

                        val is24Hr = android.text.format.DateFormat.is24HourFormat(context)
                        if (is24Hr) hour() else amPmHour()

                        char(':')
                        minute()

                        if (!is24Hr) {
                            char(' ')
                            amPmMarker("a.m.", "p.m.")
                        }
                    })
        }
    )
}