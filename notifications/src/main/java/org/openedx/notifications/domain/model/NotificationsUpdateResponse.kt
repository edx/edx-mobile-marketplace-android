package org.openedx.notifications.domain.model

data class NotificationsUpdateResponse(
    val status: String,
    val updatedValue: Boolean,
    val notificationType: String,
    val channel: String,
    val app: String,
)
