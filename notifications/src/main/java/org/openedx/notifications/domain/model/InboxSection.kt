package org.openedx.notifications.domain.model

import org.openedx.notifications.R

enum class InboxSection(val titleResId: Int) {
    RECENT(R.string.notifications_recent),

    THIS_WEEK(R.string.notifications_this_week),

    OLDER(R.string.notifications_older);
}
