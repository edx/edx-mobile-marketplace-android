package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.notifications.domain.model.NotificationsCount

data class NotificationsCountResponse(
    @SerializedName("show_notifications_tray")
    var showNotificationsTray: Boolean,
    @SerializedName("count")
    var count: Int,
    @SerializedName("count_by_app_name")
    var countByAppName: CountByAppNameModel,
    @SerializedName("notification_expiry_days")
    var notificationExpiryDays: Int,
    @SerializedName("is_new_notification_view_enabled")
    var isNewNotificationViewEnabled: Boolean,
) {
    fun mapToDomain(): NotificationsCount {
        return NotificationsCount(
            discussion = countByAppName.discussion,
        )
    }
}
