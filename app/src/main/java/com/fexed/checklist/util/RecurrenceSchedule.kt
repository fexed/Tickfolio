package com.fexed.checklist.util

import com.fexed.checklist.data.model.ChecklistRecurrence
import java.util.Calendar

object RecurrenceSchedule {

    fun firstOccurrenceAfter(
        now: Long,
        frequency: ChecklistRecurrence,
        timeMinutes: Int,
        dayOfWeek: Int,
        dayOfMonth: Int
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            setTimeOfDay(timeMinutes)
        }

        when (frequency) {
            ChecklistRecurrence.DAILY -> Unit
            ChecklistRecurrence.WEEKLY -> {
                val daysUntil = (dayOfWeek - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
                calendar.add(Calendar.DAY_OF_MONTH, daysUntil)
            }
            ChecklistRecurrence.MONTHLY -> setDayOfMonth(calendar, dayOfMonth)
        }

        while (calendar.timeInMillis <= now) {
            advance(calendar, frequency, timeMinutes, dayOfMonth)
        }
        return calendar.timeInMillis
    }

    fun nextOccurrenceAfter(
        previousOccurrence: Long,
        now: Long,
        frequency: ChecklistRecurrence,
        timeMinutes: Int,
        dayOfMonth: Int
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = previousOccurrence
        }
        do {
            advance(calendar, frequency, timeMinutes, dayOfMonth)
        } while (calendar.timeInMillis <= now)
        return calendar.timeInMillis
    }

    private fun advance(
        calendar: Calendar,
        frequency: ChecklistRecurrence,
        timeMinutes: Int,
        dayOfMonth: Int
    ) {
        when (frequency) {
            ChecklistRecurrence.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            ChecklistRecurrence.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            ChecklistRecurrence.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, 1)
                setDayOfMonth(calendar, dayOfMonth)
            }
        }
        calendar.setTimeOfDay(timeMinutes)
    }

    private fun setDayOfMonth(calendar: Calendar, dayOfMonth: Int) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, lastDay))
    }

    private fun Calendar.setTimeOfDay(timeMinutes: Int) {
        val normalizedTime = timeMinutes.coerceIn(0, 23 * 60 + 59)
        set(Calendar.HOUR_OF_DAY, normalizedTime / 60)
        set(Calendar.MINUTE, normalizedTime % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
