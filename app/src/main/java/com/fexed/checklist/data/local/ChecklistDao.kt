package com.fexed.checklist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY lastOpenedAt DESC")
    fun getAllChecklistsWithItems(): Flow<List<ChecklistWithItems>>

    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    fun getChecklistWithItems(id: Long): Flow<ChecklistWithItems?>

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY lastOpenedAt DESC")
    suspend fun getAllChecklistsForWidget(): List<ChecklistWithItems>

    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistForWidget(id: Long): ChecklistWithItems?

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistById(id: Long): Checklist?

    @Query("SELECT * FROM checklists ORDER BY lastOpenedAt DESC LIMIT 1")
    suspend fun getLatestOpenedChecklist(): Checklist?

    @Query("SELECT * FROM checklists WHERE geofenceLat IS NOT NULL AND geofenceLng IS NOT NULL")
    suspend fun getChecklistsWithGeofence(): List<Checklist>

    @Query(
        """SELECT * FROM checklists
            WHERE recurrenceFrequency IS NOT NULL
            AND recurrenceTimeMinutes IS NOT NULL
            AND recurrenceNextAt IS NOT NULL
            AND recurrenceNextAt <= :now"""
    )
    suspend fun getDueRecurringChecklists(now: Long): List<Checklist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist): Long

    @Update
    suspend fun updateChecklist(checklist: Checklist)

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklistById(id: Long): Int

    @Query("UPDATE checklists SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long): Int

    // Checklist Items
    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY position ASC, createdAt ASC")
    fun getItemsForChecklist(checklistId: Long): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getItemById(id: Long): ChecklistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItem>): List<Long>

    @Update
    suspend fun updateItem(item: ChecklistItem)

    @Delete
    suspend fun deleteItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItemById(id: Long): Int

    @Query("UPDATE checklist_items SET isCompleted = :completed, completedChecks = CASE WHEN :completed THEN requiredChecks ELSE 0 END WHERE id = :id")
    suspend fun setItemCompleted(id: Long, completed: Boolean): Int

    @Query("UPDATE checklist_items SET completedChecks = CASE WHEN :completedChecks < 0 THEN 0 WHEN :completedChecks > requiredChecks THEN requiredChecks ELSE :completedChecks END, isCompleted = CASE WHEN :completedChecks >= requiredChecks THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun setItemCheckCount(id: Long, completedChecks: Int): Int

    @Query("UPDATE checklist_items SET isCompleted = 0, completedChecks = 0 WHERE checklistId = :checklistId")
    suspend fun resetAllItems(checklistId: Long): Int

    @Transaction
    suspend fun resetRecurringChecklist(checklistId: Long, expectedNextAt: Long, nextAt: Long): Boolean {
        if (advanceRecurrence(checklistId, expectedNextAt, nextAt) != 1) return false
        resetAllItems(checklistId)
        return true
    }

    @Query(
        """UPDATE checklists SET recurrenceNextAt = :nextAt
            WHERE id = :checklistId AND recurrenceNextAt = :expectedNextAt"""
    )
    suspend fun advanceRecurrence(checklistId: Long, expectedNextAt: Long, nextAt: Long): Int

    @Query("UPDATE checklist_items SET isCompleted = 1, completedChecks = requiredChecks WHERE checklistId = :checklistId")
    suspend fun completeAllItems(checklistId: Long): Int
}
