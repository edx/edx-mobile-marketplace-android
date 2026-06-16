package org.openedx.notifications.domain.model

import java.util.Date

/**
 * Represents a course progress event that triggers notifications
 */
data class CourseProgressEvent(
    val courseId: String,
    val courseName: String,
    val blockId: String,
    val blockName: String,
    val eventType: ProgressEventType,
    val progressPercentage: Double,
    val timestamp: Date = Date(),
    val notificationId: String? = null // Unique identifier to prevent duplicates
)

enum class ProgressEventType {
    UNIT_COMPLETED,
    SECTION_PROGRESS_THRESHOLD_25,
    SECTION_PROGRESS_THRESHOLD_50,
    SECTION_PROGRESS_THRESHOLD_75,
    SECTION_PROGRESS_THRESHOLD_100,
    COURSE_PROGRESS_THRESHOLD_25,
    COURSE_PROGRESS_THRESHOLD_50,
    COURSE_PROGRESS_THRESHOLD_75,
    COURSE_PROGRESS_THRESHOLD_100,
    INACTIVITY_REMINDER,  // User hasn't opened course in X days
    DUE_DATE_APPROACHING,
}

