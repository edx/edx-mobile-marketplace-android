package org.openedx.notifications.courseprogress.data

// CourseNotification.kt
data class CourseNotification(
    val courseId: String,
    val progressPercentage: Double = 0.0,
    val milestones: List<Milestone> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val totalNotificationCount: Int = 0,
    val notificationPreferences: NotificationPreferences = NotificationPreferences()
)

// Milestone.kt
data class Milestone(
    val id: String,
    val title: String,
    val achieved: Boolean = false,
    val achievedAt: String? = null
)

// NotificationPreferences.kt
data class NotificationPreferences(
    val progressUpdates: Boolean = true,
    val milestoneAlerts: Boolean = true,
    val courseReminders: Boolean = true,
    val pushNotifications: Boolean = true
)


