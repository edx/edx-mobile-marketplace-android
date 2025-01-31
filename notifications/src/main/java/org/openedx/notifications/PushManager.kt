package org.openedx.notifications

import androidx.fragment.app.FragmentManager
import org.openedx.core.system.PushGlobalManager
import org.openedx.notifications.domain.interactor.NotificationsInteractor

class PushManager(private val interactor: NotificationsInteractor) : PushGlobalManager {

    override suspend fun getUnreadNotificationsCount(): Int {
        return interactor.getUnreadNotificationsCount().discussion
    }

    override fun showNotificationsPrimer(fragmentManager: FragmentManager) {
        if (interactor.shouldShowNotificationsPrimer()) {

        }
    }
}
