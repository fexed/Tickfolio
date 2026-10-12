package com.fexed.checklist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem

@Database(
    entities = [Checklist::class, ChecklistItem::class],
    version = 1,
    exportSchema = false
)
abstract class ChecklistDatabase : RoomDatabase() {

    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: ChecklistDatabase? = null

        fun getDatabase(context: Context): ChecklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChecklistDatabase::class.java,
                    "checklist_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
