package com.fexed.checklist.data.model

import androidx.room.Entity

@Entity(
    tableName = "sync_tombstones",
    primaryKeys = ["recordType", "syncId"]
)
data class SyncTombstone(
    val recordType: String,
    val syncId: String,
    val deletedAt: Long
) {
    companion object {
        const val CHECKLIST = "checklist"
        const val ITEM = "item"
    }
}
