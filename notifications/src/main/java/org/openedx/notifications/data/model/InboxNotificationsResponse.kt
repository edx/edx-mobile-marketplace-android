package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.Pagination
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
            next = next ?: "",
            previous = previous ?: "",
            count = count,
            numPages = numPages,
            currentPage = currentPage,
            start = start
        ),
        notifications = results.map { it.mapToDomain() }
    )
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
        lastRead = lastRead ?: "",
        lastSeen = lastSeen ?: "",
        created = created,
    )
}

data class NotificationContent(
    @SerializedName("p") val paragraph: String,
    @SerializedName("strong") val strongText: String,
    @SerializedName("topic_id") val topicId: String,
    @SerializedName("parent_id") val parentId: String?,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("comment_id") val commentId: String,
    @SerializedName("post_title") val postTitle: String,
    @SerializedName("course_name") val courseName: String,
    @SerializedName("replier_name") val replierName: String,
    @SerializedName("email_content") val emailContent: String
) {
    fun mapToDomain(): DomainNotificationContent = DomainNotificationContent(
        paragraph = paragraph,
        strongText = strongText,
        topicId = topicId,
        parentId = parentId ?: "",
        threadId = threadId,
        commentId = commentId,
        postTitle = postTitle,
        courseName = courseName,
        replierName = replierName,
        emailContent = emailContent,
    )
}
