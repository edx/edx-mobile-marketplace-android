package org.openedx.notifications.data.storage

import org.openedx.notifications.domain.model.NotificationsConfiguration

interface NotificationsPreferences {
    var notifications: NotificationsConfiguration
}
