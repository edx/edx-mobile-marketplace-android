package org.openedx.core.system

import android.content.Context
import androidx.fragment.app.FragmentManager

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int

    suspend fun markNotificationAsRead(notificationId: Int)

    fun showNotificationsPrimer(context: Context, fragmentManager: FragmentManager)

    fun logNotificationPermissionStatusEvent(context: Context)

    fun logNotificationBellClickedEvent(hasUnreadNotifications: Boolean)

    fun logNotificationReceivedEvent(data: Map<String, String>)

    fun logNotificationTappedEvent(data: Map<String, String>)
}

class DummyPushManager : PushGlobalManager {
    override suspend fun getUnreadNotificationsCount() = 0
    override suspend fun markNotificationAsRead(notificationId: Int) {}
    override fun showNotificationsPrimer(context: Context, fragmentManager: FragmentManager) {}
    override fun logNotificationPermissionStatusEvent(context: Context) {}
    override fun logNotificationBellClickedEvent(hasUnreadNotifications: Boolean) {}
    override fun logNotificationReceivedEvent(data: Map<String, String>) {}
    override fun logNotificationTappedEvent(data: Map<String, String>) {}
}
