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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL("DROP TABLE IF EXISTS course_enrollment_details_table")

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS course_enrollment_details_table (
                id TEXT PRIMARY KEY NOT NULL,
                developerMessage TEXT,
                hasAccess INTEGER,
                errorCode TEXT,
                certificateURL TEXT,
                isActive INTEGER NOT NULL,
                auditAccessExpires TEXT,
                upgradeDeadline TEXT,
                isTooEarly INTEGER NOT NULL,
                userMessage TEXT,
                mode TEXT,
                number TEXT NOT NULL,
                twitter TEXT NOT NULL,
                courseModes TEXT,
                courseHandouts TEXT NOT NULL,
                end TEXT,
                startDisplay TEXT NOT NULL,
                discussionUrl TEXT NOT NULL,
                additionalContextUserMessage TEXT,
                isStaff INTEGER NOT NULL,
                org TEXT NOT NULL,
                userFragment TEXT,
                created TEXT,
                facebook TEXT NOT NULL,
                start TEXT,
                startType TEXT NOT NULL,
                bannerImage TEXT,
                courseImage TEXT,
                courseVideo TEXT,
                image TEXT,
                courseAbout TEXT NOT NULL,
                isSelfPaced INTEGER NOT NULL,
                courseUpdates TEXT NOT NULL,
                hasUnmetPrerequisites INTEGER NOT NULL,
                name TEXT NOT NULL
            )
        """
            )

            database.execSQL(
                """
    CREATE TABLE IF NOT EXISTS course_modes_table (
        course_id TEXT NOT NULL,
        slug TEXT,
        sku TEXT,
        androidSku TEXT,
        iosSku TEXT,
        minPrice REAL,
        storeSku TEXT,
        PRIMARY KEY(course_id, slug),
        FOREIGN KEY(course_id) REFERENCES course_enrollment_details_table(id) ON DELETE CASCADE
    )
    """
            )
        }
    }
}
