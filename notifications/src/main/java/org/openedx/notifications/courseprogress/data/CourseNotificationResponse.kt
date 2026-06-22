package org.openedx.notifications.courseprogress.data


import com.google.gson.annotations.SerializedName

data class CourseNotificationResponse(
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("progress_percentage")
    val progressPercentage: Double = 0.0,
    @SerializedName("unread_count")
    val unreadCount: Int = 0,
    @SerializedName("total_count")
    val totalCount: Int = 0,
    @SerializedName("milestones")
    val milestones: List<MilestoneResponse> = emptyList(),
    @SerializedName("preferences")
    val preferences: PreferencesResponse = PreferencesResponse()
) {
    fun mapToDomain() = CourseNotification(
        courseId = courseId,
        progressPercentage = progressPercentage.coerceIn(0.0, 100.0),
        unreadNotificationCount = unreadCount,
        totalNotificationCount = totalCount,
        milestones = milestones.map { it.mapToDomain() },
        notificationPreferences = preferences.mapToDomain()
    )

    fun mapToRoomEntity() = CourseNotificationEntity(
        courseId = courseId,
        progressPercentage = progressPercentage,
        unreadNotificationCount = unreadCount,
        totalNotificationCount = totalCount,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

data class MilestoneResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("achieved")
    val achieved: Boolean = false,
    @SerializedName("achieved_at")
    val achievedAt: String? = null
) {
    fun mapToDomain() = org.openedx.notifications.courseprogress.data.Milestone(
        id = id,
        title = title,
        achieved = achieved,
        achievedAt = achievedAt
    )
}

data class PreferencesResponse(
    @SerializedName("progress_updates")
    val progressUpdates: Boolean = true,
    @SerializedName("milestone_alerts")
    val milestoneAlerts: Boolean = true,
    @SerializedName("course_reminders")
    val courseReminders: Boolean = true,
    @SerializedName("push_notifications")
    val pushNotifications: Boolean = true
) {
    fun mapToDomain() = org.openedx.notifications.courseprogress.data.NotificationPreferences(
        progressUpdates = progressUpdates,
        milestoneAlerts = milestoneAlerts,
        courseReminders = courseReminders,
        pushNotifications = pushNotifications
    )
}

