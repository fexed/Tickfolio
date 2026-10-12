package com.fexed.checklist.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ChecklistWithItems(
    @Embedded
    val checklist: Checklist,
    @Relation(
        parentColumn = "id",
        entityColumn = "checklistId"
    )
    val items: List<ChecklistItem>
) {
    val totalCount: Int
        get() = items.size

    val completedCount: Int
        get() = items.count { it.isCompleted }

    val progress: Float
        get() = if (items.isEmpty()) 0f else completedCount.toFloat() / items.size

    val tags: List<String>
        get() = items.map { it.tag.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
}
