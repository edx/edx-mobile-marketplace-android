package org.openedx.notifications.courseprogress.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "course_notification")
data class CourseNotificationEntity(
    @PrimaryKey
    val courseId: String,
    val progressPercentage: Double = 0.0,
    val unreadNotificationCount: Int = 0,
    val totalNotificationCount: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    fun mapToDomain(
        milestones: List<MilestoneEntity> = emptyList(),
        preferences: NotificationPreferencesEntity? = null
    ) = CourseNotification(
        courseId = courseId,
        progressPercentage = progressPercentage,
        unreadNotificationCount = unreadNotificationCount,
        totalNotificationCount = totalNotificationCount,
        milestones = milestones.map { it.mapToDomain() },
        notificationPreferences = preferences?.mapToDomain() ?: NotificationPreferences()
    )
}

@Entity(
    tableName = "course_milestone",
    foreignKeys = [
        ForeignKey(
            entity = CourseNotificationEntity::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class MilestoneEntity(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val title: String,
    val achieved: Boolean = false,
    val achievedAt: String? = null
) {
    fun mapToDomain() = Milestone(
        id = id,
        title = title,
        achieved = achieved,
        achievedAt = achievedAt
    )
}

@Entity(
    tableName = "notification_preferences",
    foreignKeys = [
        ForeignKey(
            entity = CourseNotificationEntity::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotificationPreferencesEntity(
    @PrimaryKey
    val courseId: String,
    val progressUpdates: Boolean = true,
    val milestoneAlerts: Boolean = true,
    val courseReminders: Boolean = true,
    val pushNotifications: Boolean = true
) {
    fun mapToDomain() = NotificationPreferences(
        progressUpdates = progressUpdates,
        milestoneAlerts = milestoneAlerts,
        courseReminders = courseReminders,
        pushNotifications = pushNotifications
    )
}

