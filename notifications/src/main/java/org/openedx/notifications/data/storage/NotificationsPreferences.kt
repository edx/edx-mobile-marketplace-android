package org.openedx.notifications.data.storage

import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration

interface NotificationsPreferences {
    var notifications: NotificationsConfiguration
    var primer: NotificationsPrimerConfiguration
}
