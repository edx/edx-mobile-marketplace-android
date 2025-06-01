package org.openedx.core.system.notifier

data class UpdateCourseData(
    val courseId: String,
    val isFromValueProp: Boolean = false,
    val isExpiredCoursePurchase: Boolean = false,
) : IAPEvent
