package com.fexed.checklist.ui.screens.agenda

import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.util.RecurrenceSchedule

enum class AgendaEventKind {
    ITEM_DUE,
    CHECKLIST_REMINDER
}

data class AgendaEvent(
    val id: String,
    val timestamp: Long,
    val checklistId: Long,
    val checklistTitle: String,
    val title: String,
    val kind: AgendaEventKind
)

object AgendaEventFactory {
    fun create(
        checklists: List<ChecklistWithItems>,
        nowMillis: Long
    ): List<AgendaEvent> = buildList {
        checklists.forEach { entry ->
            val checklist = entry.checklist
            entry.items.filterNot { it.isCompleted }.forEach { item ->
                item.dueAt?.let { dueAt ->
                val frequency = ChecklistRecurrence.fromName(item.dueRecurrenceFrequency)
                    val occurrenceAt = frequency?.let {
                        RecurrenceSchedule.nextOccurrenceAtOrAfter(dueAt, nowMillis, it)
                    } ?: dueAt
                    add(
                        AgendaEvent(
                            id = "item:${item.syncId}:$occurrenceAt",
                            timestamp = occurrenceAt,
                            checklistId = checklist.id,
                            checklistTitle = checklist.title,
                            title = item.title,
                            kind = AgendaEventKind.ITEM_DUE
                        )
                    )
                }
            }

            checklist.reminderTime
                ?.takeIf { it >= nowMillis }
                ?.let { reminderAt ->
                    add(
                        AgendaEvent(
                            id = "reminder:${checklist.syncId}:$reminderAt",
                            timestamp = reminderAt,
                            checklistId = checklist.id,
                            checklistTitle = checklist.title,
                            title = checklist.title,
                            kind = AgendaEventKind.CHECKLIST_REMINDER
                        )
                    )
                }

            val hasIncompleteItems = entry.items.isEmpty() || entry.items.any { !it.isCompleted }
            val reminderFrequency =
                ChecklistRecurrence.fromName(checklist.reminderRecurrenceFrequency)
            val recurringReminderAt = checklist.reminderRecurrenceNextAt
                ?.takeIf {
                    hasIncompleteItems &&
                        reminderFrequency != null &&
                        it >= nowMillis
                }
            recurringReminderAt?.let { reminderAt ->
                add(
                    AgendaEvent(
                        id = "recurring-reminder:${checklist.syncId}:$reminderAt",
                        timestamp = reminderAt,
                        checklistId = checklist.id,
                        checklistTitle = checklist.title,
                        title = checklist.title,
                        kind = AgendaEventKind.CHECKLIST_REMINDER
                    )
                )
            }
        }
    }.sortedWith(
        compareBy<AgendaEvent> { it.timestamp }
            .thenBy { it.checklistTitle }
            .thenBy { it.title }
    )
}
