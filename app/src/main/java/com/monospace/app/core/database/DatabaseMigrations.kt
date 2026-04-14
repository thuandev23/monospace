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
}
