package org.openedx.app.system.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.concurrent.TimeUnit

class CourseAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(courseId: String, courseName: String) {
        val intent = Intent(context, CourseReminderReceiver::class.java).apply {
            putExtra(CourseReminderReceiver.EXTRA_COURSE_ID, courseId)
            putExtra(CourseReminderReceiver.EXTRA_COURSE_NAME, courseName)
        }

        // Use courseId's hashcode as a unique requestCode for the PendingIntent
        val requestCode = courseId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel(courseId: String) {
        val intent = Intent(context, CourseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            courseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}