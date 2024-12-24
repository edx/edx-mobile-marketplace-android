package org.openedx.core.system.notifier

import org.openedx.core.system.notifier.app.AppEvent

sealed class PushEvent : AppEvent {
    data object RefreshBadgeCount : PushEvent()
}
