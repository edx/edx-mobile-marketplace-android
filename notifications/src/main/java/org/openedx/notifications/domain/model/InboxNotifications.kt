package org.openedx.notifications.domain.model

data class InboxNotifications(
    val pagination: Pagination,
    val notifications: List<NotificationItem>
)

data class Pagination(
    val next: String,
    val previous: String,
    val count: Int,
    val numPages: Int,
    val currentPage: Int,
    val start: Int
)

data class NotificationItem(
    val id: Int,
    val appName: String,
    val notificationType: String,
    val contentContext: NotificationContent,
    val content: String,
    val contentUrl: String,
    val lastRead: String,
    val lastSeen: String,
    val created: String
)

data class NotificationContent(
    val paragraph: String,
    val strongText: String,
    val topicId: String,
    val parentId: String,
    val threadId: String,
    val commentId: String,
    val postTitle: String,
    val courseName: String,
    val replierName: String,
    val emailContent: String
)
