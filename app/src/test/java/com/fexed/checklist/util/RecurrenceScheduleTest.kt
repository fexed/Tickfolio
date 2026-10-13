package com.fexed.checklist.util

import com.fexed.checklist.data.model.ChecklistRecurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RecurrenceScheduleTest {

    @Test
    fun firstDailyOccurrenceUsesSelectedTimeTodayWhenStillUpcoming() = withUtc {
        val now = timestamp(2025, Calendar.JANUARY, 1, 8, 0)

        val result = RecurrenceSchedule.firstOccurrenceAfter(
            now = now,
            frequency = ChecklistRecurrence.DAILY,
            timeMinutes = 9 * 60,
            dayOfWeek = Calendar.MONDAY,
            dayOfMonth = 1
        )

        assertEquals(timestamp(2025, Calendar.JANUARY, 1, 9, 0), result)
    }

    @Test
    fun firstDailyOccurrenceAfterResetTimeUsesTomorrow() = withUtc {
        val now = timestamp(2025, Calendar.JANUARY, 1, 9, 0)

        val result = RecurrenceSchedule.firstOccurrenceAfter(
            now = now,
            frequency = ChecklistRecurrence.DAILY,
            timeMinutes = 9 * 60,
            dayOfWeek = Calendar.MONDAY,
            dayOfMonth = 1
        )

        assertEquals(timestamp(2025, Calendar.JANUARY, 2, 9, 0), result)
    }

    @Test
    fun firstWeeklyOccurrenceUsesSelectedWeekday() = withUtc {
        val now = timestamp(2025, Calendar.JANUARY, 4, 10, 0)

        val result = RecurrenceSchedule.firstOccurrenceAfter(
            now = now,
            frequency = ChecklistRecurrence.WEEKLY,
            timeMinutes = 9 * 60,
            dayOfWeek = Calendar.MONDAY,
            dayOfMonth = 1
        )

        assertEquals(timestamp(2025, Calendar.JANUARY, 6, 9, 0), result)
    }

    @Test
    fun firstMonthlyOccurrenceClampsToLastDayOfShortMonth() = withUtc {
        val now = timestamp(2025, Calendar.FEBRUARY, 1, 8, 0)

        val result = RecurrenceSchedule.firstOccurrenceAfter(
            now = now,
            frequency = ChecklistRecurrence.MONTHLY,
            timeMinutes = 9 * 60,
            dayOfWeek = Calendar.MONDAY,
            dayOfMonth = 31
        )

        assertEquals(timestamp(2025, Calendar.FEBRUARY, 28, 9, 0), result)
    }

    @Test
    fun missedMonthlyOccurrencesAdvanceToNextFutureScheduledDate() = withUtc {
        val previous = timestamp(2025, Calendar.JANUARY, 31, 9, 0)
        val now = timestamp(2025, Calendar.MARCH, 1, 12, 0)

        val result = RecurrenceSchedule.nextOccurrenceAfter(
            previousOccurrence = previous,
            now = now,
            frequency = ChecklistRecurrence.MONTHLY,
            timeMinutes = 9 * 60,
            dayOfMonth = 31
        )

        assertEquals(timestamp(2025, Calendar.MARCH, 31, 9, 0), result)
    }

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis

    private inline fun withUtc(test: () -> Unit) {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            test()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
