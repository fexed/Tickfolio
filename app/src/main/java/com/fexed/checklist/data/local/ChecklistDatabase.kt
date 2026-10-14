package com.fexed.checklist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.SyncTombstone

@Database(
    entities = [Checklist::class, ChecklistItem::class, SyncTombstone::class],
    version = 5,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklists ADD COLUMN recurrenceFrequency TEXT")
                db.execSQL("ALTER TABLE checklists ADD COLUMN recurrenceTimeMinutes INTEGER")
                db.execSQL("ALTER TABLE checklists ADD COLUMN recurrenceDayOfWeek INTEGER")
                db.execSQL("ALTER TABLE checklists ADD COLUMN recurrenceDayOfMonth INTEGER")
                db.execSQL("ALTER TABLE checklists ADD COLUMN recurrenceNextAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklists ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE checklists ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE checklists SET syncId = lower(hex(randomblob(16))), updatedAt = MAX(createdAt, lastOpenedAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_checklists_syncId ON checklists(syncId)")

                db.execSQL("ALTER TABLE checklist_items ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE checklist_items SET syncId = lower(hex(randomblob(16))), updatedAt = createdAt")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_checklist_items_syncId ON checklist_items(syncId)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS sync_tombstones (
                        recordType TEXT NOT NULL,
                        syncId TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        PRIMARY KEY(recordType, syncId)
                    )"""
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN dueAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN dueNotificationSentAt INTEGER DEFAULT NULL")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
