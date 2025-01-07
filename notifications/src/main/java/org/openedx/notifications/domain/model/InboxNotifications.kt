package org.openedx.notifications.domain.model

import java.util.Date

data class InboxNotifications(
    val pagination: Pagination,
    val notifications: Map<InboxSection, List<NotificationItem>>,
)

data class Pagination(
    val next: String,
    val previous: String,
    val count: Int,
    val numPages: Int,
    val currentPage: Int,
    val start: Int,
)

data class NotificationItem(
    val id: Int,
    val appName: String,
    val notificationType: String,
    val contentContext: NotificationContent,
    val content: String,
    val contentUrl: String,
    val lastRead: Date?,
    val lastSeen: Date?,
    val created: Date?,
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
    val emailContent: String,
    val authorName: String,
    val authorPronoun: String,
)
