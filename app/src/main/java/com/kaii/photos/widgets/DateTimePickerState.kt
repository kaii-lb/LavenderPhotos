package com.kaii.photos.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.yearMonth
import kotlin.time.Duration.Companion.seconds

open class DateTimePickerState(
    val initialDateTime: LocalDateTime,
    private val scope: CoroutineScope
) {
    var date by mutableStateOf(initialDateTime.date)
        private set

    var time by mutableStateOf(initialDateTime.time)
        private set

    var isError by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun setIsLoading(loading: Boolean) {
        isLoading = loading
    }

    fun setIsError(error: Boolean) {
        isError = error

        if (isError) scope.launch {
            delay(1.seconds)
            isError = false
            isLoading = false
        }
    }

    fun setDay(day: Int) {
        val days = date.yearMonth.numberOfDays
        val actual = if (day !in 1..days) days else day

        date = LocalDate(date.year, date.month, actual)
    }

    fun setMonth(month: Month) {
        val days = YearMonth(date.year, month).numberOfDays
        val day = if (date.day !in 1..days) days else date.day

        date = LocalDate(date.year, month, day)
    }

    fun setYear(year: Int) {
        val days = YearMonth(year, date.month).numberOfDays
        val day = if (date.day !in 1..days) days else date.day

        date = LocalDate(year, date.month, day)
    }

    fun setSecond(second: Int) {
        time = LocalTime(time.hour, time.minute, second)
    }

    fun setMinute(minute: Int) {
        time = LocalTime(time.hour, minute, time.second)
    }

    fun setHour(hour: Int) {
        time = LocalTime(hour, time.minute, time.second)
    }

    fun getDateTime() = date.atTime(time).toInstant(timeZone = TimeZone.currentSystemDefault())
}

@Composable
fun rememberDateTimePickerState(
    initialDateTime: LocalDateTime
): DateTimePickerState {
    val coroutineScope = rememberCoroutineScope()

    return remember(initialDateTime) {
        DateTimePickerState(
            initialDateTime = initialDateTime,
            scope = coroutineScope
        )
    }
}