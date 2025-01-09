package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.notifications.domain.model.NotificationsConfiguration

data class NotificationsConfiguration(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: NotificationData,
) {
    fun mapToDomain(): NotificationsConfiguration {
        return NotificationsConfiguration(
            discussionsPushEnabled = data.discussion
                .notificationTypes[CORE_NOTIFICATION_TYPE]?.push ?: false
        )
    }

    companion object {
        const val CORE_NOTIFICATION_TYPE = "core"
    }
}

data class NotificationData(
    @SerializedName("discussion") val discussion: NotificationCategory,
)

data class NotificationCategory(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("notification_types") val notificationTypes: Map<String, NotificationType>,
    @SerializedName("core_notification_types") val coreNotificationTypes: List<String>,
)

data class NotificationType(
    @SerializedName("push") val push: Boolean,
)
