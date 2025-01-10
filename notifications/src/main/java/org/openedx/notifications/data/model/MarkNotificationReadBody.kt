package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName

data class MarkNotificationReadBody(
    @SerializedName("app_name")
    val appName: String,
    @SerializedName("notification_id")
    val notificationId: Int?,
)
