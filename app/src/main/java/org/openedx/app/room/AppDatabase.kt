package org.openedx.app.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.discovery.EnrolledCourseEntity
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.course.data.storage.CourseConverter
import org.openedx.course.data.storage.CourseDao
import org.openedx.dashboard.data.DashboardDao
import org.openedx.discovery.data.converter.DiscoveryConverter
import org.openedx.discovery.data.model.room.CourseEntity
import org.openedx.discovery.data.storage.DiscoveryDao

const val DATABASE_VERSION = 2
const val DATABASE_NAME = "OpenEdX_db"

@Database(
    entities = [
        CourseEntity::class,
        EnrolledCourseEntity::class,
        CourseStructureEntity::class,
        DownloadModelEntity::class,
        CourseEnrollmentDetailsEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(DiscoveryConverter::class, CourseConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun courseDao(): CourseDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun downloadDao(): DownloadDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS course_enrollment_details_table")
        // Create the new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS course_enrollment_details_table (
                id TEXT NOT NULL PRIMARY KEY,
                courseUpdates TEXT NOT NULL,
                courseHandouts TEXT NOT NULL,
                discussionUrl TEXT NOT NULL,
                hasUnmetPrerequisites INTEGER NOT NULL,
                isTooEarly INTEGER NOT NULL,
                isStaff INTEGER NOT NULL,
                auditAccessExpires TEXT,
                hasAccess INTEGER NOT NULL,
                errorCode TEXT,
                developerMessage TEXT,
                userMessage TEXT,
                additionalContextUserMessage TEXT,
                userFragment TEXT,
                certificateURL TEXT,
                created TEXT,
                mode TEXT,
                isActive INTEGER NOT NULL,
                upgradeDeadline TEXT,
                name TEXT NOT NULL,
                number TEXT NOT NULL,
                org TEXT NOT NULL,
                start TEXT,
                startDisplay TEXT NOT NULL,
                startType TEXT NOT NULL,
                end TEXT,
                isSelfPaced INTEGER NOT NULL,
                bannerImage_uri TEXT,
                bannerImage_uriAbsolute TEXT,
                courseImage_uri TEXT,
                courseImage_name TEXT,
                courseVideo_uri TEXT,
                image_large TEXT,
                image_small TEXT,
                image_raw TEXT,
                facebook TEXT NOT NULL,
                twitter TEXT NOT NULL,
                courseAbout TEXT NOT NULL
            )
        """.trimIndent()
        )
    }
}
