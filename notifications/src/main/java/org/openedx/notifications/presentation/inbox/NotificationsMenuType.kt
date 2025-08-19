package org.openedx.notifications.presentation.inbox

import androidx.annotation.StringRes
import org.openedx.notifications.R

enum class NotificationsMenuType(@field:StringRes val title: Int) {
    MARK_ALL_READ(R.string.notifications_menu_mark_all_read),
    SETTINGS(R.string.notifications_menu_settings),
}
