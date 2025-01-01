package org.openedx.notifications.data.api

object APIConstants {
    const val NOTIFICATION_COUNT = "/api/notifications/count/"
    const val NOTIFICATIONS_INBOX = "/api/notifications/"
    const val NOTIFICATIONS_SEEN = "/api/notifications/mark-seen/{app_name}/"
    const val NOTIFICATION_READ = "/api/notifications/read/"

    const val APP_NAME_DISCUSSION = "discussion"
}
