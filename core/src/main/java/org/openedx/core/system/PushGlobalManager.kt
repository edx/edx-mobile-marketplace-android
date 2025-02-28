package org.openedx.core.system

import android.content.Context
import androidx.fragment.app.FragmentManager

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int

    suspend fun markNotificationAsRead(notificationId: Int)

    fun showNotificationsPrimer(context: Context, fragmentManager: FragmentManager)

    fun logNotificationBellClickedEvent(hasUnreadNotifications: Boolean)

    fun logNotificationReceivedEvent(data: Map<String, String>)

    fun logNotificationTappedEvent(data: Map<String, String>)
}
