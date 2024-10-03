package org.openedx.core.system.notifier

data class UpdateCourseData(
    val isCourseDashboard: Boolean = false,
    val isExpiredCoursePurchase: Boolean = false,
) : IAPEvent
