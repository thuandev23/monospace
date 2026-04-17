package com.monospace.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations.
 * Đặt tách ra ngoài @Module để tránh KSP bug với anonymous object bên trong Hilt module.
 */
object DatabaseMigrations {

    /**
     * v1 → v2: Không có thay đổi schema thực sự (identityHash giống nhau).
     * Migration no-op để tránh fallbackToDestructiveMigration xóa data của user.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Schema không thay đổi giữa v1 và v2
        }
    }

    /**
     * v2 → v3: Thay thế is_completed BOOLEAN bằng task_status TEXT.
     * Mapping: is_completed=1 → DONE, is_completed=0 → NOT_DONE
     * Giữ lại cột is_completed vì SQLite không hỗ trợ DROP COLUMN trên API < 35.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE tasks ADD COLUMN task_status TEXT NOT NULL DEFAULT 'NOT_DONE'"
            )
            db.execSQL(
                "UPDATE tasks SET task_status = CASE WHEN is_completed = 1 THEN 'DONE' ELSE 'NOT_DONE' END"
            )
        }
    }

    /**
     * v3 → v4: Thêm sort_order vào bảng lists để hỗ trợ drag-to-reorder.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE lists ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v4 → v5: Recreate tasks and lists tables to add SQL DEFAULT clauses.
     * @ColumnInfo(defaultValue) was added to entities to make Room's schema match the actual DB,
     * but fresh installs at v3/v4 used CREATE TABLE without DEFAULT, causing schema mismatch.
     * Note: tasks may still have a legacy is_completed column (SQLite <35 can't DROP COLUMN),
     * so we use explicit column lists in INSERT...SELECT to avoid column-count errors.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Recreate tasks with DEFAULT 'NOT_DONE' on task_status
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `tasks_new` (
                    `id` TEXT NOT NULL,
                    `external_id` TEXT,
                    `external_source` TEXT,
                    `title` TEXT NOT NULL,
                    `notes` TEXT,
                    `task_status` TEXT NOT NULL DEFAULT 'NOT_DONE',
                    `priority` INTEGER NOT NULL,
                    `parent_task_id` TEXT,
                    `list_id` TEXT NOT NULL,
                    `sync_status` TEXT NOT NULL,
                    `start_date_time` INTEGER,
                    `end_date_time` INTEGER,
                    `is_all_day` INTEGER NOT NULL,
                    `reminder_value` INTEGER,
                    `reminder_unit` TEXT,
                    `reminder_time` TEXT,
                    `repeat_interval` INTEGER,
                    `repeat_unit` TEXT,
                    `repeat_days_of_week` TEXT,
                    `checksum` TEXT,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `server_updated_at` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`list_id`) REFERENCES `lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )"""
            )
            db.execSQL(
                """INSERT INTO `tasks_new` (
                    `id`, `external_id`, `external_source`, `title`, `notes`, `task_status`,
                    `priority`, `parent_task_id`, `list_id`, `sync_status`, `start_date_time`,
                    `end_date_time`, `is_all_day`, `reminder_value`, `reminder_unit`, `reminder_time`,
                    `repeat_interval`, `repeat_unit`, `repeat_days_of_week`, `checksum`,
                    `created_at`, `updated_at`, `server_updated_at`
                ) SELECT
                    `id`, `external_id`, `external_source`, `title`, `notes`, `task_status`,
                    `priority`, `parent_task_id`, `list_id`, `sync_status`, `start_date_time`,
                    `end_date_time`, `is_all_day`, `reminder_value`, `reminder_unit`, `reminder_time`,
                    `repeat_interval`, `repeat_unit`, `repeat_days_of_week`, `checksum`,
                    `created_at`, `updated_at`, `server_updated_at`
                FROM `tasks`"""
            )
            db.execSQL("DROP TABLE `tasks`")
            db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_list_id` ON `tasks` (`list_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sync_status` ON `tasks` (`sync_status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_external_id` ON `tasks` (`external_id`)")

            // ─── Recreate lists with DEFAULT 0 on sort_order
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `lists_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `external_id` TEXT,
                    `sync_status` TEXT NOT NULL,
                    `sort_order` INTEGER NOT NULL DEFAULT 0,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )"""
            )
            db.execSQL(
                """INSERT INTO `lists_new` (`id`, `name`, `external_id`, `sync_status`, `sort_order`, `created_at`, `updated_at`)
                   SELECT `id`, `name`, `external_id`, `sync_status`, `sort_order`, `created_at`, `updated_at` FROM `lists`"""
            )
            db.execSQL("DROP TABLE `lists`")
            db.execSQL("ALTER TABLE `lists_new` RENAME TO `lists`")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `focus_sessions` (
                    `id` TEXT NOT NULL,
                    `completed_at` INTEGER NOT NULL,
                    `duration_minutes` INTEGER NOT NULL,
                    `profile_id` TEXT,
                    PRIMARY KEY(`id`)
                )"""
            )
        }
    }
}
