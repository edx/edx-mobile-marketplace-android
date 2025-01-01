package org.openedx.notifications.data.model

import com.google.gson.annotations.SerializedName

data class NotificationsMarkResponse(
    @SerializedName("message")
    val message: String,
)
