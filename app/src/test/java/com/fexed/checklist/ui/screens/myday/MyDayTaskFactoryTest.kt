package com.fexed.checklist.ui.screens.myday

import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyDayTaskFactoryTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun includesOverdueAndDueTodayItemsAndKeepsSelectedItemsAvailableToRemove() {
        val now = timestamp(day = 10, hour = 9)
        val checklist = ChecklistWithItems(
            checklist = Checklist(id = 1L, title = "Home"),
            items = listOf(
                item("overdue", "Overdue", timestamp(day = 9, hour = 8)),
                item("past-today", "Past today", timestamp(day = 10, hour = 8)),
                item("later-today", "Later today", timestamp(day = 10, hour = 11)),
                item("completed-today", "Completed today", timestamp(day = 10, hour = 7), true),
                item("future", "Future", timestamp(day = 11, hour = 8)),
                item("without-date", "Without date", null),
                item("selected", "Selected task", null),
                item("completed-old", "Completed old", timestamp(day = 8, hour = 8), true)
            )
        )

        val content = MyDayTaskFactory.create(
            checklists = listOf(checklist),
            selectedItemSyncIds = setOf("selected"),
            nowMillis = now,
            zone = zone
        )

        assertEquals(
            listOf("overdue", "past-today", "completed-today", "later-today", "selected"),
            content.tasks.map { it.item.syncId }
        )
        assertTrue(content.tasks.first().isOverdue)
        assertTrue(content.tasks.last().isUserSelected)
        assertEquals(
            listOf("future", "selected", "without-date"),
            content.availableItems.map { it.item.syncId }
        )
    }

    @Test
    fun includesUpcomingRecurringDueTodayButNotFutureDueItems() {
        val now = timestamp(day = 10, hour = 9)
        val checklist = ChecklistWithItems(
            checklist = Checklist(id = 2L, title = "Routine"),
            items = listOf(
                item("recurring", "Recurring today", timestamp(day = 10, hour = 11))
                    .copy(dueRecurrenceFrequency = "DAILY"),
                item("future", "Future due", timestamp(day = 11, hour = 8))
            )
        )

        val content = MyDayTaskFactory.create(
            checklists = listOf(checklist),
            selectedItemSyncIds = emptySet(),
            nowMillis = now,
            zone = zone
        )

        assertEquals(listOf("recurring"), content.tasks.map { it.item.syncId })
        assertEquals(timestamp(day = 10, hour = 11), content.tasks.single().dueAt)
    }

    private fun item(
        syncId: String,
        title: String,
        dueAt: Long?,
        isCompleted: Boolean = false
    ) = ChecklistItem(
        checklistId = 1L,
        title = title,
        dueAt = dueAt,
        isCompleted = isCompleted,
        syncId = syncId
    )

    private fun timestamp(day: Int, hour: Int): Long =
        LocalDate.of(2025, 4, day)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
