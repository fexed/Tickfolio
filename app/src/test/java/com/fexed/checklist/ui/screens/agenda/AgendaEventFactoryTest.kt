package com.fexed.checklist.ui.screens.agenda

import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AgendaEventFactoryTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun useUtcTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun includesIncompleteDueItemsAndUpcomingChecklistRemindersInTimeOrder() {
        val now = timestamp(day = 10, hour = 9)
        val checklist = ChecklistWithItems(
            checklist = Checklist(
                id = 1L,
                title = "Trip",
                syncId = "trip",
                reminderTime = timestamp(day = 10, hour = 12),
                reminderRecurrenceFrequency = "DAILY",
                reminderRecurrenceNextAt = timestamp(day = 11, hour = 8)
            ),
            items = listOf(
                ChecklistItem(
                    checklistId = 1L,
                    title = "Overdue item",
                    dueAt = timestamp(day = 9, hour = 8),
                    syncId = "overdue"
                ),
                ChecklistItem(
                    checklistId = 1L,
                    title = "Upcoming item",
                    dueAt = timestamp(day = 10, hour = 10),
                    syncId = "upcoming"
                ),
                ChecklistItem(
                    checklistId = 1L,
                    title = "Completed item",
                    dueAt = timestamp(day = 10, hour = 11),
                    isCompleted = true,
                    syncId = "completed"
                )
            )
        )

        val events = AgendaEventFactory.create(listOf(checklist), now)

        assertEquals(
            listOf(
                "Overdue item",
                "Upcoming item",
                "Trip",
                "Trip"
            ),
            events.map { it.title }
        )
        assertEquals(
            listOf(
                AgendaEventKind.ITEM_DUE,
                AgendaEventKind.ITEM_DUE,
                AgendaEventKind.CHECKLIST_REMINDER,
                AgendaEventKind.CHECKLIST_REMINDER
            ),
            events.map { it.kind }
        )
    }

    @Test
    fun recurringItemShowsItsNextOccurrenceAndChecklistReminderRemainsScheduled() {
        val now = timestamp(day = 10, hour = 9)
        val dueAt = timestamp(day = 7, hour = 8)
        val checklist = ChecklistWithItems(
            checklist = Checklist(
                id = 2L,
                title = "Maintenance",
                syncId = "maintenance",
                reminderRecurrenceFrequency = "DAILY",
                reminderRecurrenceNextAt = timestamp(day = 11, hour = 8)
            ),
            items = listOf(
                ChecklistItem(
                    checklistId = 2L,
                    title = "Check tires",
                    dueAt = dueAt,
                    dueRecurrenceFrequency = "WEEKLY",
                    syncId = "tires"
                ),
                ChecklistItem(
                    checklistId = 2L,
                    title = "Completed task",
                    isCompleted = true,
                    syncId = "done"
                )
            )
        )

        val events = AgendaEventFactory.create(listOf(checklist), now)

        assertEquals(
            timestamp(day = 14, hour = 8),
            events.single { it.title == "Check tires" }.timestamp
        )
        assertEquals(
            listOf("Maintenance", "Check tires"),
            events.map { it.title }
        )
    }

    @Test
    fun recurringChecklistReminderIsSuppressedWhenAllItemsAreComplete() {
        val now = timestamp(day = 10, hour = 9)
        val checklist = ChecklistWithItems(
            checklist = Checklist(
                id = 4L,
                title = "Finished checklist",
                syncId = "finished",
                reminderRecurrenceFrequency = "DAILY",
                reminderRecurrenceNextAt = timestamp(day = 11, hour = 8)
            ),
            items = listOf(
                ChecklistItem(
                    checklistId = 4L,
                    title = "Completed task",
                    isCompleted = true,
                    syncId = "done"
                )
            )
        )

        assertEquals(emptyList<AgendaEvent>(), AgendaEventFactory.create(listOf(checklist), now))
    }

    @Test
    fun pastOneTimeRemindersAreNotShownAsUpcoming() {
        val now = timestamp(day = 10, hour = 9)
        val checklist = ChecklistWithItems(
            checklist = Checklist(
                id = 3L,
                title = "Old reminder",
                syncId = "old",
                reminderTime = timestamp(day = 9, hour = 8)
            ),
            items = emptyList()
        )

        assertEquals(emptyList<AgendaEvent>(), AgendaEventFactory.create(listOf(checklist), now))
    }

    private fun timestamp(day: Int, hour: Int): Long =
        LocalDate.of(2025, 4, day)
            .atTime(hour, 0)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
}
