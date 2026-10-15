package com.fexed.checklist.data.model

import androidx.room.Embedded
import androidx.room.Relation
import java.util.Locale

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
        get() = items.sumOf { it.requiredChecks }

    val completedCount: Int
        get() = items.sumOf { it.completedChecks.coerceIn(0, it.requiredChecks) }

    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    val tags: List<String>
        get() = items.flatMap { it.tags }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sorted()
}
