package org.openedx.notifications.data.repository

import org.openedx.core.extension.isNotNull
import org.openedx.notifications.data.api.APIConstants
import org.openedx.notifications.data.api.NotificationsApi
import org.openedx.notifications.data.model.MarkNotificationReadBody
import org.openedx.notifications.data.model.NotificationsUpdateBody
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsCount
import org.openedx.notifications.domain.model.NotificationsUpdateResponse

class NotificationsRepository(
    private val api: NotificationsApi,
) {
    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return api.getUnreadNotificationsCount().mapToDomain()
    }

    suspend fun getInboxNotifications(page: Int): InboxNotifications {
        return api.getInboxNotifications(
            appName = APIConstants.APP_NAME_DISCUSSION,
            page = page
        ).mapToDomain()
    }

    suspend fun markNotificationsAsSeen(): Boolean {
        return api.markNotificationsAsSeen(
            appName = APIConstants.APP_NAME_DISCUSSION,
        ).message.isNotNull()
    }

    suspend fun markNotificationAsRead(notificationId: Int?): Boolean {
        return api.markNotificationAsRead(
            MarkNotificationReadBody(
                appName = APIConstants.APP_NAME_DISCUSSION,
                notificationId = notificationId,
            )
        ).message.isNotNull()
    }

    suspend fun fetchNotificationsConfiguration(): NotificationsConfiguration {
        return api.fetchNotificationsConfiguration().mapToDomain()
    }

    suspend fun updateNotificationsConfiguration(
        isDiscussionPushEnabled: Boolean,
    ): NotificationsUpdateResponse {
        return api.updateNotificationsConfiguration(
            NotificationsUpdateBody(
                notificationApp = APIConstants.APP_NAME_DISCUSSION,
                notificationType = APIConstants.NOTIFICATION_TYPE,
                notificationChannel = APIConstants.NOTIFICATION_CHANNEL,
                value = isDiscussionPushEnabled,
            )
        ).mapToDomain()
    }
}
