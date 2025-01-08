package org.openedx.notifications.domain.interactor

import org.openedx.notifications.data.repository.NotificationsRepository
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsCount

class NotificationsInteractor(private val repository: NotificationsRepository) {

    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return repository.getUnreadNotificationsCount()
    }

    suspend fun getInboxNotifications(page: Int): InboxNotifications {
        return repository.getInboxNotifications(page)
    }
}
