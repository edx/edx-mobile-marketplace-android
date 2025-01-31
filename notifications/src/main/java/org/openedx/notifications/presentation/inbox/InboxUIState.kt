package org.openedx.notifications.presentation.inbox

import org.openedx.core.presentation.global.FullScreenState
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationItem

sealed class InboxUIState {

    data class Data(
        val notifications: Map<InboxSection, List<NotificationItem>>,
    ) : InboxUIState()

    data object Loading : InboxUIState()

    data class Fallback(
        val state: FullScreenState,
    ) : InboxUIState()
}
