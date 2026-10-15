package com.fexed.checklist.ui.screens.overview

import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ChecklistSortOption
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistSortingTest {
    private fun checklist(
        id: Long,
        title: String,
        dueAt: Long? = null,
        updatedAt: Long = 0L,
        itemUpdatedAt: Long = 0L,
        createdAt: Long = 0L,
        lastOpenedAt: Long = 0L
    ) = ChecklistWithItems(
        checklist = Checklist(
            id = id,
            title = title,
            createdAt = createdAt,
            lastOpenedAt = lastOpenedAt,
            updatedAt = updatedAt
        ),
        items = if (dueAt == null && itemUpdatedAt == 0L) {
            emptyList()
        } else {
            listOf(
                ChecklistItem(
                    checklistId = id,
                    title = "Item",
                    dueAt = dueAt,
                    updatedAt = itemUpdatedAt
                )
            )
        }
    )

    @Test
    fun titleSortSupportsBothDirections() {
        val source = listOf(checklist(1, "Bravo"), checklist(2, "alpha"))

        assertEquals(
            listOf("alpha", "Bravo"),
            sortChecklists(source, ChecklistSortOption.TITLE, ascending = true)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("Bravo", "alpha"),
            sortChecklists(source, ChecklistSortOption.TITLE, ascending = false)
                .map { it.checklist.title }
        )
    }

    @Test
    fun dueDateSortKeepsMissingDatesLastInBothDirections() {
        val source = listOf(
            checklist(1, "No date"),
            checklist(2, "Later", dueAt = 200L),
            checklist(3, "Earlier", dueAt = 100L)
        )

        assertEquals(
            listOf("Earlier", "Later", "No date"),
            sortChecklists(source, ChecklistSortOption.DUE_DATE, ascending = true)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("Later", "Earlier", "No date"),
            sortChecklists(source, ChecklistSortOption.DUE_DATE, ascending = false)
                .map { it.checklist.title }
        )
    }

    @Test
    fun lastEditedSortUsesNewestChecklistOrItemTimestamp() {
        val source = listOf(
            checklist(1, "Checklist edit", updatedAt = 100L),
            checklist(2, "Item edit", updatedAt = 20L, itemUpdatedAt = 300L),
            checklist(3, "Old", updatedAt = 10L)
        )

        assertEquals(
            listOf("Old", "Checklist edit", "Item edit"),
            sortChecklists(source, ChecklistSortOption.LAST_EDITED, ascending = true)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("Item edit", "Checklist edit", "Old"),
            sortChecklists(source, ChecklistSortOption.LAST_EDITED, ascending = false)
                .map { it.checklist.title }
        )
    }

    @Test
    fun creationAndLastOpenedSortSupportBothDirections() {
        val source = listOf(
            checklist(1, "Second", createdAt = 20L, lastOpenedAt = 30L),
            checklist(2, "First", createdAt = 10L, lastOpenedAt = 40L)
        )

        assertEquals(
            listOf("First", "Second"),
            sortChecklists(source, ChecklistSortOption.CREATED, ascending = true)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("Second", "First"),
            sortChecklists(source, ChecklistSortOption.CREATED, ascending = false)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("Second", "First"),
            sortChecklists(source, ChecklistSortOption.LAST_OPENED, ascending = true)
                .map { it.checklist.title }
        )
        assertEquals(
            listOf("First", "Second"),
            sortChecklists(source, ChecklistSortOption.LAST_OPENED, ascending = false)
                .map { it.checklist.title }
        )
    }
}
