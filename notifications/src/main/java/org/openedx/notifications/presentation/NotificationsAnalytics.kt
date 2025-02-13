package org.openedx.notifications.presentation

interface NotificationsAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class NotificationsAnalyticsEvent(val eventName: String, val biValue: String) {
    DISCUSSION_PERMISSION_TOGGLE(
        eventName = "Notification:Discussion Permission Toggle",
        biValue = "edx.bi.app.notification.discussion.permission.toggle"
    ),
    NOTIFICATION_INBOX_VIEW(
        eventName = "Notification:Notification Inbox",
        biValue = "edx.bi.app.notification.inbox"
    ),
    NOTIFICATION_ITEM_TAPPED(
        eventName = "Notification:Notification Tapped",
        biValue = "edx.bi.app.notification.tapped"
    ),
}

enum class NotificationsAnalyticsKey(val key: String) {
    NAME("name"),
    ACTION("action"),
    CATEGORY("category"),
    NOTIFICATIONS("notifications"),
    NOTIFICATION_CATEGORY("notification_category"),
    DISCUSSION("discussion"),
    NOTIFICATION_TYPE("notification_type"),
}
