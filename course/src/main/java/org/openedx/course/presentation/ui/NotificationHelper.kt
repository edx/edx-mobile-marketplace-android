import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.openedx.app.AppActivity
import org.openedx.app.deeplink.DeepLink
import org.openedx.core.R as CoreR

object NotificationHelper {

    private const val CHANNEL_ID = "course_reminders"
    private var notificationId = 1

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        courseId: String? = null  // ← ADD PARAMETER
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Course Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // ← ADD: Create intent with deep link
        val intent = Intent(context, AppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (courseId != null) {
                putExtra(DeepLink.Keys.NOTIFICATION_TYPE.value, "course_progress")
                putExtra(DeepLink.Keys.COURSE_ID.value, courseId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(CoreR.drawable.core_ic_forward)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)  // ← ADD: Set click handler
            .build()

        notificationManager.notify(notificationId++, notification)
    }
}