package org.openedx.notifications

import org.openedx.core.system.PushGlobalManager
import org.openedx.notifications.domain.interactor.NotificationsInteractor

class PushManager(private val interactor: NotificationsInteractor) : PushGlobalManager {

    override suspend fun getUnreadNotificationsCount(): Int {
        return interactor.getUnreadNotificationsCount().discussion
    }
}
