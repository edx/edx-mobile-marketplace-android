package org.openedx.notifications.domain.interactor

import org.openedx.notifications.data.repository.NotificationsRepository
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsCount
import org.openedx.notifications.domain.model.NotificationsUpdateResponse

class NotificationsInteractor(
    private val repository: NotificationsRepository,
) {
    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return repository.getUnreadNotificationsCount()
    }

    suspend fun getInboxNotifications(page: Int): InboxNotifications {
        return repository.getInboxNotifications(page)
    }

    suspend fun markNotificationsAsSeen(): Boolean {
        return repository.markNotificationsAsSeen()
    }

    suspend fun markNotificationAsRead(notificationId: Int): Boolean {
        return repository.markNotificationAsRead(notificationId = notificationId)
    }

    suspend fun fetchNotificationsConfiguration(): NotificationsConfiguration {
        return repository.fetchNotificationsConfiguration()
    }

    suspend fun updateNotificationsConfiguration(
        isDiscussionPushEnabled: Boolean,
    ): NotificationsUpdateResponse {
        return repository.updateNotificationsConfiguration(
            isDiscussionPushEnabled = isDiscussionPushEnabled,
        )
    }

    suspend fun markAllNotificationsAsRead(): Boolean {
        return repository.markNotificationAsRead(notificationId = null)
    }
}
