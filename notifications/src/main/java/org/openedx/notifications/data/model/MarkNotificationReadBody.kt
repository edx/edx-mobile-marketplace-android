package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName

data class MarkNotificationReadBody(
    @SerializedName("notification_id")
    val notificationId: Int,
)
