package org.openedx.core.system.notifier

data class UpdateCourseData(
    val isPurchasedFromCourseDashboard: Boolean = false,
    val isExpiredCoursePurchase: Boolean = false,
) : IAPEvent
