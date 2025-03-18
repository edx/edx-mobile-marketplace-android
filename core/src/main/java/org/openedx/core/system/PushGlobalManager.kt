package org.openedx.core.system

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int

    suspend fun markNotificationAsRead(notificationId: Int)

    fun showNotificationsPrimer(context: Context, fragmentManager: FragmentManager)

    fun requestNotificationPermission(
        activity: Activity,
        permissionLauncher: ActivityResultLauncher<String>,
        onSystemDialogShown: () -> Unit = {},
        onRationaleShown: () -> Unit = {},
    )

    fun logNotificationPermissionStatusEvent(context: Context)

    fun logNotificationBellClickedEvent(hasUnreadNotifications: Boolean)

    fun logNotificationReceivedEvent(data: Map<String, String>)

    fun logNotificationTappedEvent(data: Map<String, String>)
}
