package org.openedx.app.system.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.openedx.app.system.push.CourseNotificationHelper

class CourseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val courseId = intent.getStringExtra(EXTRA_COURSE_ID)
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME)

        if (courseId.isNullOrEmpty() || courseName.isNullOrEmpty()) {
            return
        }

        val title = "Return to your course!"
        val message = "You have unsaved progress in $courseName. Tap to continue."

        CourseNotificationHelper.showCourseProgressNotification(
            context,
            title,
            message,
            courseId,
            isReminder = false // This is an immediate notification, not a reminder
        )
    }

    companion object {
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_NAME = "course_name"
    }
}