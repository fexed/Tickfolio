package com.fexed.checklist.ui.screens.overview

import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ChecklistSortOption
import java.util.Locale

internal fun sortChecklists(
    checklists: List<ChecklistWithItems>,
    option: ChecklistSortOption,
    ascending: Boolean
): List<ChecklistWithItems> {
    val direction = if (ascending) 1 else -1
    val titleComparator = compareBy<ChecklistWithItems> {
        it.checklist.title.lowercase(Locale.ROOT)
    }
    val comparator = when (option) {
        ChecklistSortOption.TITLE -> if (ascending) titleComparator else titleComparator.reversed()
        ChecklistSortOption.DUE_DATE -> Comparator<ChecklistWithItems> { first, second ->
            val firstDueDate = first.items.mapNotNull { it.dueAt }.minOrNull()
            val secondDueDate = second.items.mapNotNull { it.dueAt }.minOrNull()
            when {
                firstDueDate == null && secondDueDate != null -> 1
                firstDueDate != null && secondDueDate == null -> -1
                else -> compareValues(firstDueDate, secondDueDate) * direction
            }
        }.thenComparing(titleComparator)
        ChecklistSortOption.LAST_EDITED -> compareBy<ChecklistWithItems> {
            maxOf(it.checklist.updatedAt, it.items.maxOfOrNull { item -> item.updatedAt } ?: 0L)
        }.let { if (ascending) it else it.reversed() }.thenComparing(titleComparator)
        ChecklistSortOption.CREATED -> compareBy<ChecklistWithItems> {
            it.checklist.createdAt
        }.let { if (ascending) it else it.reversed() }.thenComparing(titleComparator)
        ChecklistSortOption.LAST_OPENED -> compareBy<ChecklistWithItems> {
            it.checklist.lastOpenedAt
        }.let { if (ascending) it else it.reversed() }.thenComparing(titleComparator)
    }
    return checklists.sortedWith(comparator)
}
