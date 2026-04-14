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
}
