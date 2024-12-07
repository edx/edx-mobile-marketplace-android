package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class AppAnalyticsEvent(val eventName: String, val biValue: String) {
    LAUNCH(
        "Launch",
        "edx.bi.app.launch"
    ),
    LEARN(
        "MainDashboard:Learn",
        "edx.bi.app.main_dashboard.learn"
    ),
    DISCOVER(
        "MainDashboard:Discover",
        "edx.bi.app.main_dashboard.discover"
    ),
    PROFILE(
        "MainDashboard:Profile",
        "edx.bi.app.main_dashboard.profile"
    ),
    NOTIFICATION_PERMISSION(
        "Notification:Setting Permission Status",
        "edx.bi.app.notification.permission_settings.status"
    )
}

enum class AppAnalyticsKey(val key: String) {
    NAME("name"),
    STATUS("status"),
}

enum class PermissionStatus(val status: String) {
    DENIED("denied"),
    AUTHORIZED("authorized"),
}
