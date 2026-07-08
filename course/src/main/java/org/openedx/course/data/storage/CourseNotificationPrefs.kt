package org.openedx.course.data.storage

import android.content.Context

class CourseNotificationPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("course_notification_prefs", Context.MODE_PRIVATE)

    // Flag for "Course Started"
    fun isCourseStartedNotified(courseId: String): Boolean {
        return prefs.getBoolean("notified_started_$courseId", false)
    }

    fun setCourseStartedNotified(courseId: String) {
        prefs.edit().putBoolean("notified_started_$courseId", true).apply()
    }

    // Flags for "Module/Lesson Completed"
    fun isBlockCompletionNotified(blockId: String): Boolean {
        return prefs.getBoolean("notified_completed_$blockId", false)
    }

    fun setBlockCompletionNotified(blockId: String) {
        prefs.edit().putBoolean("notified_completed_$blockId", true).apply()
    }
}