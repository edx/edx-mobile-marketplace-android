package org.openedx.notifications.data.repository

import org.openedx.notifications.data.api.APIConstants
import org.openedx.notifications.data.api.NotificationsApi
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsCount

class NotificationsRepository(private val api: NotificationsApi) {
    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return api.getUnreadNotificationsCount().mapToDomain()
    }

    suspend fun getInboxNotifications(page: Int): InboxNotifications {
        return api.getInboxNotifications(
            appName = APIConstants.APP_NAME_DISCUSSION,
            page = page
        ).mapToDomain()
    }
}
