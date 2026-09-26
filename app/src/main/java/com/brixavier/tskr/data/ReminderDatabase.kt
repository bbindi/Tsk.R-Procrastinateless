package com.brixavier.tskr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence

/**
 * Migration from Database Schema Version 6 to Schema Version 7.
 * Adds recurrence columns to 'reminders' and creates 'reminder_occurrences' table with initial backfill.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add recurrence columns to 'reminders' table
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceType` TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceInterval` INTEGER NOT NULL DEFAULT 1")

        // 2. Create 'reminder_occurrences' table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminder_occurrences` (
                `id` TEXT NOT NULL,
                `templateId` TEXT,
                `title` TEXT NOT NULL,
                `priority` TEXT NOT NULL DEFAULT 'NORMAL',
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `completedAtMs` INTEGER,
                `subTasksRaw` TEXT NOT NULL DEFAULT '',
                `snoozedUntilMs` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`templateId`) REFERENCES `reminders`(`id`) ON DELETE SET NULL
            )
        """.trimIndent())

        // 3. Create indexes on 'reminder_occurrences'
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_templateId` ON `reminder_occurrences` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_status` ON `reminder_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_date_time` ON `reminder_occurrences` (`date`, `time`)")

        // 4. Backfill existing V1 reminders safely into 'reminder_occurrences'
        db.execSQL("""
            INSERT INTO `reminder_occurrences` (`id`, `templateId`, `title`, `priority`, `date`, `time`, `status`, `subTasksRaw`, `completedAtMs`)
            SELECT `id`, `id`, `title`, `priority`, `date`, `time`,
                   CASE WHEN `isActive` = 1 THEN 'PENDING' ELSE 'COMPLETED' END,
                   `subTasksRaw`,
                   NULL
            FROM `reminders`
        """.trimIndent())
    }
}

/**
 * Migration from Database Schema Version 7 to Schema Version 8.
 * Adds user-configurable alert stage booleans to 'reminders' and 'reminder_occurrences'.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add alert columns to 'reminders' (DEFAULT 1 preserves 3-stage cascade for existing tasks)
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `notify7Days` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `notify24Hours` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `notifyDueNow` INTEGER NOT NULL DEFAULT 1")

        // Add alert columns to 'reminder_occurrences'
        db.execSQL("ALTER TABLE `reminder_occurrences` ADD COLUMN `notify7Days` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `reminder_occurrences` ADD COLUMN `notify24Hours` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `reminder_occurrences` ADD COLUMN `notifyDueNow` INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * Migration from Database Schema Version 8 to Schema Version 9.
 * Introduces the Plans feature foundation.
 * Creates `plans` table and adds nullable `planId` to `reminders` and `reminder_occurrences`.
 * Uses table recreation to enforce ON DELETE SET NULL foreign keys safely in SQLite.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create Plans table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plans` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `deadline` TEXT,
                `status` TEXT NOT NULL,
                `createdAtMs` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // 2. Recreate reminders table to add planId with Foreign Key
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders_new` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `priority` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `subTasksRaw` TEXT NOT NULL,
                `motivation` TEXT NOT NULL,
                `recurrenceType` TEXT NOT NULL,
                `recurrenceInterval` INTEGER NOT NULL,
                `notify7Days` INTEGER NOT NULL,
                `notify24Hours` INTEGER NOT NULL,
                `notifyDueNow` INTEGER NOT NULL,
                `planId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`planId`) REFERENCES `plans`(`id`) ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `reminders_new`
            SELECT *, NULL FROM `reminders`
        """.trimIndent())

        db.execSQL("DROP TABLE `reminders`")
        db.execSQL("ALTER TABLE `reminders_new` RENAME TO `reminders`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_planId` ON `reminders` (`planId`)")

        // 3. Recreate reminder_occurrences table to add planId with Foreign Key
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminder_occurrences_new` (
                `id` TEXT NOT NULL,
                `templateId` TEXT,
                `title` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `completedAtMs` INTEGER,
                `subTasksRaw` TEXT NOT NULL,
                `snoozedUntilMs` INTEGER,
                `notify7Days` INTEGER NOT NULL,
                `notify24Hours` INTEGER NOT NULL,
                `notifyDueNow` INTEGER NOT NULL,
                `planId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`templateId`) REFERENCES `reminders`(`id`) ON DELETE SET NULL,
                FOREIGN KEY(`planId`) REFERENCES `plans`(`id`) ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `reminder_occurrences_new`
            SELECT *, NULL FROM `reminder_occurrences`
        """.trimIndent())

        db.execSQL("DROP TABLE `reminder_occurrences`")
        db.execSQL("ALTER TABLE `reminder_occurrences_new` RENAME TO `reminder_occurrences`")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_templateId` ON `reminder_occurrences` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_status` ON `reminder_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_date_time` ON `reminder_occurrences` (`date`, `time`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_occurrences_planId` ON `reminder_occurrences` (`planId`)")
    }
}

/**
 * Migration from Database Schema Version 9 to Schema Version 10.
 * Adds 'plan_note_sections' and 'plan_steps' tables for Plan workspace notes and steps.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create plan_note_sections table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plan_note_sections` (
                `id` TEXT NOT NULL,
                `planId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL DEFAULT '',
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAtMs` INTEGER NOT NULL,
                `updatedAtMs` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`planId`) REFERENCES `plans`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_note_sections_planId` ON `plan_note_sections` (`planId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_note_sections_planId_sortOrder` ON `plan_note_sections` (`planId`, `sortOrder`)")

        // 2. Create plan_steps table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plan_steps` (
                `id` TEXT NOT NULL,
                `planId` TEXT NOT NULL,
                `text` TEXT NOT NULL,
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAtMs` INTEGER NOT NULL,
                `updatedAtMs` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`planId`) REFERENCES `plans`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_steps_planId` ON `plan_steps` (`planId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_steps_planId_sortOrder` ON `plan_steps` (`planId`, `sortOrder`)")
    }
}

/**
 * Migration from Database Schema Version 10 to Schema Version 11.
 * Adds 'snoozeCount' column to 'reminder_occurrences' table for multi-step snooze tracking.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `reminder_occurrences` ADD COLUMN `snoozeCount` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Migration from Database Schema Version 11 to Schema Version 12.
 * Adds Recurrence 2.0 configuration columns to 'reminders' table and converts legacy CUSTOM_DAYS to DAILY.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceDaysOfWeek` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceMonthlyMode` TEXT NOT NULL DEFAULT 'DAY_OF_MONTH'")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceYearlyMode` TEXT NOT NULL DEFAULT 'SPECIFIC_DATE'")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceOrdinal` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceWeekday` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `recurrenceYearlyMonth` INTEGER NOT NULL DEFAULT 0")

        // Legacy migration: Convert CUSTOM_DAYS to DAILY with interval
        db.execSQL("UPDATE `reminders` SET `recurrenceType` = 'DAILY' WHERE `recurrenceType` = 'CUSTOM_DAYS'")
    }
}

/**
 * Migration from Database Schema Version 12 to Schema Version 13.
 * Adds Plan Identity columns ('category' and 'categorySource') to 'plans' table and migrates legacy emoji icons.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add Plan Identity columns to 'plans' table
        db.execSQL("ALTER TABLE `plans` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'PERSONAL'")
        db.execSQL("ALTER TABLE `plans` ADD COLUMN `categorySource` TEXT NOT NULL DEFAULT 'AUTOMATIC'")

        // 2. Migrate legacy emoji/icon values to structured PlanCategory strings where possible
        db.execSQL("UPDATE `plans` SET `category` = 'WORK' WHERE `icon` LIKE '%💼%' OR `icon` LIKE '%🏢%' OR `icon` LIKE '%👔%'")
        db.execSQL("UPDATE `plans` SET `category` = 'GOALS' WHERE `icon` LIKE '%🎯%' OR `icon` LIKE '%🏆%' OR `icon` LIKE '%🚩%'")
        db.execSQL("UPDATE `plans` SET `category` = 'FINANCE' WHERE `icon` LIKE '%💰%' OR `icon` LIKE '%💵%' OR `icon` LIKE '%🏦%'")
        db.execSQL("UPDATE `plans` SET `category` = 'HOME' WHERE `icon` LIKE '%🏠%' OR `icon` LIKE '%🏡%' OR `icon` LIKE '%🧹%'")
        db.execSQL("UPDATE `plans` SET `category` = 'TRAVEL' WHERE `icon` LIKE '%✈%' OR `icon` LIKE '%🧳%' OR `icon` LIKE '%🗺%'")
        db.execSQL("UPDATE `plans` SET `category` = 'EVENTS' WHERE `icon` LIKE '%🎉%' OR `icon` LIKE '%🎂%' OR `icon` LIKE '%💍%'")
        db.execSQL("UPDATE `plans` SET `category` = 'SPIRITUAL' WHERE `icon` LIKE '%📖%' OR `icon` LIKE '%🙏%' OR `icon` LIKE '%⛪%'")
        db.execSQL("UPDATE `plans` SET `category` = 'CREATIVE' WHERE `icon` LIKE '%🎨%' OR `icon` LIKE '%✍%' OR `icon` LIKE '%🎵%'")
        db.execSQL("UPDATE `plans` SET `category` = 'SHOPPING' WHERE `icon` LIKE '%🛒%' OR `icon` LIKE '%🛍%'")
        db.execSQL("UPDATE `plans` SET `category` = 'HEALTH' WHERE `icon` LIKE '%🏋%' OR `icon` LIKE '%💪%' OR `icon` LIKE '%🏥%'")
        db.execSQL("UPDATE `plans` SET `category` = 'FAMILY' WHERE `icon` LIKE '%👨‍👩‍👧‍👦%' OR `icon` LIKE '%👶%' OR `icon` LIKE '%❤️%'")
        db.execSQL("UPDATE `plans` SET `category` = 'PROJECTS' WHERE `icon` LIKE '%📁%' OR `icon` LIKE '%💻%' OR `icon` LIKE '%⚙%'")
        db.execSQL("UPDATE `plans` SET `category` = 'PLANNING' WHERE `icon` LIKE '%📅%' OR `icon` LIKE '%📋%' OR `icon` LIKE '%🗓%'")
    }
}

/**
 * Migration from Database Schema Version 13 to Schema Version 14.
 * Adds Plan Color Identity columns ('color' and 'colorSource') to 'plans' table.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plans` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'SLATE'")
        db.execSQL("ALTER TABLE `plans` ADD COLUMN `colorSource` TEXT NOT NULL DEFAULT 'CATEGORY'")
    }
}

/**
 * The SQLite Room Database for persisting Reminders and Occurrences.
 */
@Database(
    entities = [Plan::class, Reminder::class, ReminderOccurrence::class, PlanNoteSection::class, PlanStep::class],
    version = 14,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
