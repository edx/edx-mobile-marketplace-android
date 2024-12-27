package org.openedx.notifications.presentation.inbox

import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationItem

sealed class InboxUIState {

    data class Data(
        val notifications: Map<InboxSection, List<NotificationItem>>,
    ) : InboxUIState()

    data object Empty : InboxUIState()
    data object Error : InboxUIState()
    data object Loading : InboxUIState()
}
