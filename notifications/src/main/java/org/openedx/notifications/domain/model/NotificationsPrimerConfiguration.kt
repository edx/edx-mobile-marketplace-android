package org.openedx.notifications.domain.model

import java.util.Date

data class NotificationsPrimerConfiguration(
    var nextPrimer: Date? = null,
    var dismissalCount: Int = 0,
)
