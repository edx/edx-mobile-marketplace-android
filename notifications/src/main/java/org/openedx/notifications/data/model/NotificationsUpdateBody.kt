package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.notifications.domain.model.NotificationsUpdateResponse

data class NotificationsUpdateBody(
    @SerializedName("notification_app") val notificationApp: String,
    @SerializedName("notification_type") val notificationType: String,
    @SerializedName("notification_channel") val notificationChannel: String,
    @SerializedName("value") val value: Boolean,
)

data class NotificationsUpdateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: NotificationUpdateData,
) {
    fun mapToDomain(): NotificationsUpdateResponse {
        return NotificationsUpdateResponse(
            status = status,
            updatedValue = data.updatedValue,
            notificationType = data.notificationType,
            channel = data.channel,
            app = data.app
        )
    }
}

data class NotificationUpdateData(
    @SerializedName("updated_value") val updatedValue: Boolean,
    @SerializedName("notification_type") val notificationType: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("app") val app: String,
)
