package org.openedx.app.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE download_model ADD COLUMN transcriptUrls TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE download_model ADD COLUMN transcriptPaths TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE download_model ADD COLUMN transcriptDownloadedStatus TEXT NOT NULL DEFAULT 'NOT_DOWNLOADED'")
        }
    }
}
