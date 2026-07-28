package org.openedx.core.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

interface NotificationDisplayManager {
    fun areNotificationsEnabled(context: Context): Boolean

    fun showNotification(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String?,
        content: String,
        smallIconResId: Int,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
    )

    fun cancelAll()

    fun cancel(notificationId: Int)
}

class NotificationDisplayManagerImpl(
    private val notificationManager: NotificationManager,
) : NotificationDisplayManager {

    override fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    override fun showNotification(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String?,
        content: String,
        smallIconResId: Int,
        priority: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIconResId)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    override fun cancelAll() {
        notificationManager.cancelAll()
    }

    override fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}