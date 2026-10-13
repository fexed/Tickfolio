package com.fexed.checklist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem

@Database(
    entities = [Checklist::class, ChecklistItem::class],
    version = 2,
    exportSchema = false
)
abstract class ChecklistDatabase : RoomDatabase() {

    abstract fun checklistDao(): ChecklistDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN requiredChecks INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN completedChecks INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE checklist_items SET completedChecks = 1 WHERE isCompleted = 1")
            }
        }

        @Volatile
        private var INSTANCE: ChecklistDatabase? = null

        fun getDatabase(context: Context): ChecklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChecklistDatabase::class.java,
                    "checklist_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
