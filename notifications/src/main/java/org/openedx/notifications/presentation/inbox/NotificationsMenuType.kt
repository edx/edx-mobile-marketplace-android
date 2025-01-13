package org.openedx.notifications.presentation.inbox

import androidx.annotation.StringRes
import org.openedx.notifications.R

enum class NotificationsMenuType(@StringRes val title: Int) {
    MARK_ALL_READ(R.string.notifications_menu_mark_all_read),
    NOTIFICATION_SETTINGS(R.string.notifications_menu_settings),
}
