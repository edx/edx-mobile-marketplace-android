package org.openedx.notifications.presentation

interface NotificationsAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class NotificationsAnalyticsEvent(val eventName: String, val biValue: String) {
    DISCUSSION_PERMISSION_TOGGLE(
        eventName = "Notification:Discussion Permission Toggle",
        biValue = "edx.bi.app.notification.discussion.permission.toggle"
    )
}

enum class NotificationsAnalyticsKey(val key: String) {
    NAME("name"),
    ACTION("action"),
    CATEGORY("category"),
    NOTIFICATIONS("notifications"),
}
