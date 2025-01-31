package org.openedx.notifications.presentation.inbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import org.openedx.core.presentation.global.FullScreenState
import org.openedx.notifications.R

object InboxFullScreenState {
    val Empty = FullScreenState(
        imageVector = Icons.Outlined.Notifications,
        titleResId = R.string.notifications_no_notifications_yet,
        descriptionResId = R.string.notifications_no_notifications_yet_description
    )
    val NetworkError = FullScreenState.NetworkError
    val ServerError = FullScreenState.ServerError
}
