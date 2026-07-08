package org.openedx.app.system.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import org.openedx.app.AppActivity
import org.openedx.app.deeplink.DeepLink // Keep this import
import org.openedx.app.deeplink.DeepLinkType
import org.openedx.core.R as CoreR

object CourseNotificationHelper {

    private const val CHANNEL_ID = "course_progress_notifications"

    fun showCourseProgressNotification(
        context: Context,
        title: String,
        message: String,
        courseId: String,
        isReminder: Boolean // This flag will now be used
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Course Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = SystemClock.uptimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(CoreR.drawable.core_ic_forward)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        if (isReminder) {
            val intent = Intent(context, AppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                // Reference the inner enum through the outer class
                putExtra(DeepLink.Keys.NOTIFICATION_TYPE.value, DeepLinkType.COURSE_DASHBOARD.type)
                putExtra(DeepLink.Keys.COURSE_ID.value, courseId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}