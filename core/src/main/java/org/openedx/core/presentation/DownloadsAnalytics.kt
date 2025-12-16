package org.openedx.core.presentation

interface DownloadsAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class DownloadsAnalyticsEvent(val eventName: String, val biValue: String) {
    DOWNLOAD_COURSE_CLICKED(
        "Downloads:Download Course Clicked",
        "edx.bi.app.downloads.downloadCourseClicked"
    ),
    CANCEL_DOWNLOAD_CLICKED(
        "Downloads:Cancel Download Clicked",
        "edx.bi.app.downloads.cancelDownloadClicked"
    ),
    REMOVE_DOWNLOAD_CLICKED(
        "Downloads:Remove Download Clicked",
        "edx.bi.app.downloads.removeDownloadClicked"
    ),
    DOWNLOAD_CONFIRMED(
        "Downloads:Download Confirmed",
        "edx.bi.app.downloads.downloadConfirmed"
    ),
    DOWNLOAD_CANCELLED(
        "Downloads:Download Cancelled",
        "edx.bi.app.downloads.downloadCancelled"
    ),
    DOWNLOAD_REMOVED(
        "Downloads:Download Removed",
        "edx.bi.app.downloads.downloadRemoved"
    ),
    DOWNLOAD_ERROR(
        "Downloads:Download Error",
        "edx.bi.app.downloads.downloadError"
    ),
    DOWNLOAD_COMPLETED(
        "Downloads:Download Completed",
        "edx.bi.app.downloads.downloadCompleted"
    ),
    DOWNLOAD_STARTED(
        "Downloads:Download Started",
        "edx.bi.app.downloads.downloadStarted"
    ),
}

enum class DownloadsAnalyticsKey(val key: String) {
    NAME("name"),
}
