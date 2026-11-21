package com.fexed.checklist.ui.screens.myday

import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.util.RecurrenceSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class MyDayTask(
    val item: ChecklistItem,
    val checklistId: Long,
    val checklistTitle: String,
    val dueAt: Long?,
    val isDueAutomatically: Boolean,
    val isOverdue: Boolean,
    val isUserSelected: Boolean
)

data class MyDayContent(
    val tasks: List<MyDayTask>,
    val availableItems: List<MyDayTask>
)

object MyDayTaskFactory {
    fun create(
        checklists: List<ChecklistWithItems>,
        selectedItemSyncIds: Set<String>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): MyDayContent {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val records = checklists.flatMap { entry ->
            entry.items.map { item ->
                val frequency = ChecklistRecurrence.fromName(item.dueRecurrenceFrequency)
                val dueAt = item.dueAt?.let { anchor ->
                    if (frequency != null && !item.isCompleted) {
                        RecurrenceSchedule.nextOccurrenceAtOrAfter(anchor, nowMillis, frequency)
                    } else {
                        anchor
                    }
                }
                val dueDate = dueAt?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                }
                val isDueAutomatically = when {
                    dueDate == null -> false
                    frequency != null -> !item.isCompleted && dueDate == today
                    item.isCompleted -> dueDate == today
                    else -> dueDate <= today
                }
                val isUserSelected = item.syncId in selectedItemSyncIds
                MyDayTask(
                    item = item,
                    checklistId = entry.checklist.id,
                    checklistTitle = entry.checklist.title,
                    dueAt = dueAt,
                    isDueAutomatically = isDueAutomatically,
                    isOverdue = isDueAutomatically && !item.isCompleted &&
                        dueAt?.let { it < nowMillis } == true,
                    isUserSelected = isUserSelected
                )
            }
        }

        val tasks = records
            .filter { it.isDueAutomatically || it.isUserSelected }
            .sortedWith(taskOrder())
        val availableItems = records
            .filter { !it.isDueAutomatically && (!it.item.isCompleted || it.isUserSelected) }
            .sortedWith(
                compareBy<MyDayTask> { it.checklistTitle.lowercase(Locale.ROOT) }
                    .thenBy { it.item.title.lowercase(Locale.ROOT) }
            )

        return MyDayContent(tasks = tasks, availableItems = availableItems)
    }

    private fun taskOrder() = compareBy<MyDayTask> {
        when {
            it.isOverdue -> 0
            it.isDueAutomatically -> 1
            else -> 2
        }
    }.thenBy { if (it.isDueAutomatically) it.dueAt ?: Long.MAX_VALUE else Long.MAX_VALUE }
        .thenBy { it.checklistTitle.lowercase(Locale.ROOT) }
        .thenBy { it.item.title.lowercase(Locale.ROOT) }
}
