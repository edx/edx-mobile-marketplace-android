package org.openedx.notifications.domain.model

data class NotificationsConfiguration(
    val discussionsPushEnabled: Boolean,
) {
    companion object {
        val default = NotificationsConfiguration(
            discussionsPushEnabled = false,
        )
    }
}
