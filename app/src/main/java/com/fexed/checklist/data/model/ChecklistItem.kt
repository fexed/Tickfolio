package com.fexed.checklist.data.model

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Checklist::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("checklistId")
    ]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val checklistId: Long,
    val title: String,
    val description: String = "",
    val tag: String = "",
    val imageUri: String? = null,
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val requiredChecks: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val completedChecks: Int = if (isCompleted) requiredChecks else 0,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''")
    val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL")
    val dueAt: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val dueNotificationSentAt: Long? = null
)
