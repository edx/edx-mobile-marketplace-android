package org.openedx.notifications.presentation

import androidx.fragment.app.FragmentManager

interface NotificationsRouter {
    fun navigateToPushNotificationsSettings(fm: FragmentManager)
}
