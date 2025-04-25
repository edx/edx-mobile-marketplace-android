package org.openedx.course.data.storage

interface CoursePreferences {
    fun setCalendarSyncEventsDialogShown(courseName: String)
    fun isCalendarSyncEventsDialogShown(courseName: String): Boolean
    fun markPLSBannerDismissed(courseId: String, bannerType: String)
    fun canShowPLSBanner(courseId: String, bannerType:String): Boolean
}
