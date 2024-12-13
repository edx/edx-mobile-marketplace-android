package org.openedx.notifications.data.repository

import org.openedx.notifications.data.api.NotificationsApi
import org.openedx.notifications.domain.model.NotificationsCount

class NotificationsRepository(val api: NotificationsApi) {
    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return api.getUnreadNotificationsCount().mapToDomain()
    }
}
