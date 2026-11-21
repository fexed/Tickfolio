package com.fexed.checklist.data.model

enum class ChecklistRecurrence {
    DAILY,
    WEEKLY,
    MONTHLY;

    companion object {
        private val byName = values().associateBy { it.name }

        fun fromName(name: String?): ChecklistRecurrence? = name?.let(byName::get)
    }
}
