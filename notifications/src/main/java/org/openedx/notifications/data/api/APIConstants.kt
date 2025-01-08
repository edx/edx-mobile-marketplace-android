package org.openedx.notifications.data.api

object APIConstants {
    const val NOTIFICATIONS_COUNT = "/api/notifications/count/"
    const val NOTIFICATIONS_INBOX = "/api/notifications/"
    const val NOTIFICATIONS_SEEN = "/api/notifications/mark-seen/{app_name}/"
    const val NOTIFICATION_READ = "/api/notifications/read/"

    const val NOTIFICATIONS_CONFIGURATION = "/api/notifications/configurations/"
    const val NOTIFICATION_UPDATE_CONFIGURATION = "/api/notifications/preferences/update-all/"

    const val APP_NAME_DISCUSSION = "discussion"
    const val NOTIFICATION_TYPE = "core"
    const val NOTIFICATION_CHANNEL = "push"
}
