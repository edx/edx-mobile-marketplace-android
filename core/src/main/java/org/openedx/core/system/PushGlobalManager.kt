package org.openedx.core.system

import android.content.Context
import androidx.fragment.app.FragmentManager

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int

    fun showNotificationsPrimer(context: Context, fragmentManager: FragmentManager)
}
