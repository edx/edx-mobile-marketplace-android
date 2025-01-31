package org.openedx.core.system

import androidx.fragment.app.FragmentManager

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int

    fun showNotificationsPrimer(fragmentManager: FragmentManager)
}
