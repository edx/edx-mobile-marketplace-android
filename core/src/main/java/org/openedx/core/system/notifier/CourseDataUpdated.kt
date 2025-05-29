package org.openedx.core.system.notifier

sealed class CourseDataUpdated : IAPEvent {
    data class CourseEnrollmentDataUpdated(val courseId: String, val isVerifiedMode: Boolean) :
        CourseDataUpdated()

    data class CourseDashboardDataUpdate(val courseId: String, val isVerifiedMode: Boolean) :
        CourseDataUpdated()

    data object CourseUnitDataUpdate : CourseDataUpdated()
}
