package org.openedx.notifications.presentation

interface NotificationsAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class NotificationsAnalyticsEvent(val eventName: String, val biValue: String) {
    PUSH_NOTIFICATIONS_SETTINGS(
        eventName = "Notification:Push Notifications Settings",
        biValue = "edx.bi.app.notification.push_notifications_settings"
    ),
    DISCUSSION_PREFERENCE_TOGGLE(
        eventName = "Notification:Discussion Preference Toggle",
        biValue = "edx.bi.app.notification.preference.discussion.toggle"
    ),
    PUSH_PREFERENCES_BATCH_TOGGLE_STATE(
        eventName = "Notification:Preferences Toggle Batch State",
        biValue = "edx.bi.app.notification.preference.batch.toggle"
    ),
    SYSTEM_PERMISSION_DIALOG_VIEWED(
        eventName = "Notification:System Permission Dialog Viewed",
        biValue = "edx.bi.app.notification.system.permission.dialog.viewed"
    ),
    SYSTEM_PERMISSION_DIALOG_ACTION(
        eventName = "Notification:System Permission Dialog Action",
        biValue = "edx.bi.app.notification.system.permission.dialog.action"
    ),
    APP_PERMISSION_RATIONALE_DIALOG_VIEWED(
        eventName = "Notification:App Permission Rationale Dialog Viewed",
        biValue = "edx.bi.app.notification.app.permission.dialog.viewed"
    ),
    APP_PERMISSION_RATIONALE_DIALOG_ACTION(
        eventName = "Notification:App Permission Rationale Dialog Action",
        biValue = "edx.bi.app.notification.app.permission.dialog.action"
    ),
    NOTIFICATION_BELL_CLICKED(
        eventName = "Notification:Bell Clicked",
        biValue = "edx.bi.app.notification.bell.clicked"
    ),
    NOTIFICATION_INBOX_VIEW(
        eventName = "Notification:Inbox",
        biValue = "edx.bi.app.notification.inbox"
    ),
    NOTIFICATION_INBOX_ITEM_CLICKED(
        eventName = "Notification:Inbox Item Clicked",
        biValue = "edx.bi.app.notification.inbox.item.clicked"
    ),
    INBOX_MENU_CLICKED(
        eventName = "Notification:Inbox Menu Clicked",
        biValue = "edx.bi.app.notification.inbox.menu.clicked"
    ),
    INBOX_MARK_ALL_READ_CLICKED(
        eventName = "Notification:Mark All Read Clicked",
        biValue = "edx.bi.app.notification.inbox.mark_all_read.clicked"
    ),
    INBOX_PUSH_NOTIFICATIONS_SETTINGS_CLICKED(
        eventName = "Notification:Push Notifications Setting Clicked",
        biValue = "edx.bi.app.notification.inbox.push_notifications_setting.clicked"
    ),
    DISCUSSION_PRIMER_VIEWED(
        eventName = "Notification:Discussion Primer Viewed",
        biValue = "edx.bi.app.notification.primer.discussion.viewed"
    ),
    DISCUSSION_PRIMER_ACTION(
        eventName = "Notification:Discussion Primer Action",
        biValue = "edx.bi.app.notification.primer.discussion.action"
    ),
    NOTIFICATION_DISCUSSION_PUSH_RECEIVED(
        eventName = "Notification:Discussion Push Received",
        biValue = "edx.bi.app.notification.push.discussion.received"
    ),
    NOTIFICATION_DISCUSSION_PUSH_TAPPED(
        eventName = "Notification:Discussion Push Tapped",
        biValue = "edx.bi.app.notification.push.discussion.tapped"
    ),
    NOTIFICATION_PERMISSION_STATUS(
        "Notification:Setting Permission Status",
        "edx.bi.app.notification.permission_settings.status"
    )
}

enum class NotificationsAnalyticsKey(val key: String) {
    NAME("name"),
    ACTION("action"),
    CATEGORY("category"),
    ALLOW("allow"),
    DONT_ALLOW("dont_allow"),
    CONTINUE("continue"),
    CANCEL("cancel"),
    NOTIFICATIONS("notifications"),
    NOTIFICATION_DOMAIN("notification_domain"),
    DISCUSSION("discussion"),
    DISCUSSIONS_ACTIVITY("discussions_activity"),
    NOTIFICATION_TYPE("notification_type"),
    NOTIFICATION_ID("notification_id"),
    UNREAD_NOTIFICATIONS("unread_notifications"),
    NOTIFY_ME("notify_me"),
    NO_THANKS("no_thanks"),
    PRIMER_DIALOG_FREQUENCY("dialog_frequency"),
    COURSE_ID("course_id"),
    TOPIC_ID("topic_id"),
    THREAD_ID("thread_id"),
    RESPONSE_ID("response_id"),
    COMMENT_ID("comment_id"),
    SOURCE("source"),
    DISCUSSION_PRIMER("discussion_primer"),
    PUSH_SETTINGS("push_settings"),
    STATUS("status"),
}

enum class PermissionStatus(val status: String) {
    DENIED("denied"),
    AUTHORIZED("authorized")
}
