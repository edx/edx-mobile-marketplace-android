package org.openedx.notifications.data.repository

import org.openedx.core.extension.isNotNull
import org.openedx.notifications.data.api.APIConstants
import org.openedx.notifications.data.api.NotificationsApi
import org.openedx.notifications.data.model.MarkNotificationReadBody
import org.openedx.notifications.data.model.NotificationsUpdateBody
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsCount

class NotificationsRepository(
    private val api: NotificationsApi,
    private val preference: NotificationsPreferences,
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
        val response = api.fetchNotificationsConfiguration().mapToDomain()
        updateNotificationsPreference(response.discussionsPushEnabled)
        return response
    }

    suspend fun updateNotificationsConfiguration(
        isDiscussionPushEnabled: Boolean,
    ): NotificationsConfiguration {
        val response = api.updateNotificationsConfiguration(
            NotificationsUpdateBody(
                notificationApp = APIConstants.APP_NAME_DISCUSSION,
                notificationType = APIConstants.NOTIFICATION_TYPE,
                notificationChannel = APIConstants.NOTIFICATION_CHANNEL,
                value = isDiscussionPushEnabled,
            )
        ).mapToDomain()
        updateNotificationsPreference(response.discussionsPushEnabled)
        return response
    }

    private fun updateNotificationsPreference(isDiscussionPushEnabled: Boolean) {
        preference.notifications = NotificationsConfiguration(isDiscussionPushEnabled)
    }
}
