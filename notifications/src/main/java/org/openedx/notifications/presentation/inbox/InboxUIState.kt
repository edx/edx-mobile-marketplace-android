package org.openedx.notifications.presentation.inbox

import org.openedx.notifications.domain.model.NotificationItem

sealed class InboxUIState {

    data class Data(
        val notifications: List<NotificationItem>
    ) : InboxUIState()

    data object Empty : InboxUIState()
    data object Error : InboxUIState()
    data object Loading : InboxUIState()
}
