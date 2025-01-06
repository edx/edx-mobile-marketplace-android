package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.utils.TimeUtils
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.Pagination
import java.util.Date
import org.openedx.notifications.domain.model.NotificationContent as DomainNotificationContent
import org.openedx.notifications.domain.model.NotificationItem as DomainNotificationItem


data class InboxNotificationsResponse(
    @SerializedName("next") val next: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("count") val count: Int,
    @SerializedName("num_pages") val numPages: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("start") val start: Int,
    @SerializedName("results") val results: List<NotificationItem>,
) {
    fun mapToDomain(): InboxNotifications = InboxNotifications(
        pagination = Pagination(
            next = next.orEmpty(),
            previous = previous.orEmpty(),
            count = count,
            numPages = numPages,
            currentPage = currentPage,
            start = start,
        ),
        notifications = organizeNotificationsBySection()
    )

    private fun organizeNotificationsBySection(): Map<InboxSection, List<DomainNotificationItem>> {
        val currentDate = Date()
        val recentThresholdMillis = currentDate.time - DAY_IN_MILLIS
        val weekThresholdMillis = currentDate.time - WEEK_IN_MILLIS

        val notifications = results.map { it.mapToDomain() }

        return mapOf(
            InboxSection.RECENT to notifications.filter {
                (it.created?.time ?: 0L) >= recentThresholdMillis
            },
            InboxSection.THIS_WEEK to notifications.filter {
                val createdTime = it.created?.time ?: 0L
                createdTime in weekThresholdMillis until recentThresholdMillis
            },
            InboxSection.OLDER to notifications.filter {
                (it.created?.time ?: 0L) < weekThresholdMillis
            }
        )
    }

    companion object {
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val WEEK_IN_MILLIS = 7 * DAY_IN_MILLIS
    }
}

data class NotificationItem(
    @SerializedName("id") val id: Int,
    @SerializedName("app_name") val appName: String,
    @SerializedName("notification_type") val notificationType: String,
    @SerializedName("content_context") val contentContext: NotificationContent,
    @SerializedName("content") val content: String,
    @SerializedName("content_url") val contentUrl: String,
    @SerializedName("last_read") val lastRead: String?,
    @SerializedName("last_seen") val lastSeen: String?,
    @SerializedName("created") val created: String,
) {
    fun mapToDomain(): DomainNotificationItem = DomainNotificationItem(
        id = id,
        appName = appName,
        notificationType = notificationType,
        contentContext = contentContext.mapToDomain(),
        content = content,
        contentUrl = contentUrl,
        lastRead = TimeUtils.iso8601ToDate(lastRead ?: ""),
        lastSeen = TimeUtils.iso8601ToDate(lastSeen ?: ""),
        created = TimeUtils.iso8601ToDate(created),
    )
}

data class NotificationContent(
    @SerializedName("p") val paragraph: String,
    @SerializedName("strong") val strongText: String,
    @SerializedName("topic_id") val topicId: String?,
    @SerializedName("parent_id") val parentId: String?,
    @SerializedName("thread_id") val threadId: String?,
    @SerializedName("comment_id") val commentId: String?,
    @SerializedName("post_title") val postTitle: String,
    @SerializedName("course_name") val courseName: String,
    @SerializedName("replier_name") val replierName: String,
    @SerializedName("email_content") val emailContent: String,
) {
    fun mapToDomain(): DomainNotificationContent = DomainNotificationContent(
        paragraph = paragraph,
        strongText = strongText,
        topicId = topicId.orEmpty(),
        parentId = parentId.orEmpty(),
        threadId = threadId.orEmpty(),
        commentId = commentId.orEmpty(),
        postTitle = postTitle,
        courseName = courseName,
        replierName = replierName,
        emailContent = emailContent,
    )
}
