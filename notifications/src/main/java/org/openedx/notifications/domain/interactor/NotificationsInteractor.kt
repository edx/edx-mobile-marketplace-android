package org.openedx.notifications.domain.interactor

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.openedx.core.extension.isNull
import org.openedx.core.utils.addDays
import org.openedx.notifications.data.repository.NotificationsRepository
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.model.InboxNotifications
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsCount
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration
import org.openedx.notifications.domain.model.NotificationsUpdateResponse
import java.util.Date

class NotificationsInteractor(
    private val context: Context,
    private val repository: NotificationsRepository,
    private val preferences: NotificationsPreferences,
) {
    suspend fun getUnreadNotificationsCount(): NotificationsCount {
        return repository.getUnreadNotificationsCount()
    }

    suspend fun getInboxNotifications(page: Int): InboxNotifications {
        return repository.getInboxNotifications(page)
    }

    suspend fun markNotificationsAsSeen(): Boolean {
        return repository.markNotificationsAsSeen()
    }

    suspend fun markNotificationAsRead(notificationId: Int): Boolean {
        return repository.markNotificationAsRead(notificationId = notificationId)
    }

    suspend fun fetchNotificationsConfiguration(): NotificationsConfiguration {
        return repository.fetchNotificationsConfiguration()
    }

    suspend fun updateNotificationsConfiguration(
        isDiscussionPushEnabled: Boolean,
    ): NotificationsUpdateResponse {
        return repository.updateNotificationsConfiguration(
            isDiscussionPushEnabled = isDiscussionPushEnabled,
        )
    }

    suspend fun markAllNotificationsAsRead(): Boolean {
        return repository.markNotificationAsRead(notificationId = null)
    }

    fun shouldShowNotificationsPrimer(): Boolean {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            preferences.primer = NotificationsPrimerConfiguration()
            return false
        }

        val primerConfig = preferences.primer

        // If the user has dismissed the primer PRIMER_MAX_DISMISSAL_COUNT times, stop showing it
        if (primerConfig.dismissalCount >= PRIMER_MAX_DISMISSAL_COUNT) {
            return false
        }

        val now = Date()
        val nextPrimer = primerConfig.nextPrimer

        if (nextPrimer.isNull()) {
            configureNotificationsPrimer(
                nextPrimer = now.addDays(PRIMER_INITIAL_RESHOW_DAYS),
                dismissalCount = 1,
            )
            return true
        }

        if (now.after(nextPrimer)) {
            when (primerConfig.dismissalCount) {
                1 -> {
                    configureNotificationsPrimer(
                        nextPrimer = now.addDays(PRIMER_FOLLOWUP_RESHOW_DAYS),
                        dismissalCount = 2,
                    )
                    return true
                }

                2 -> {
                    configureNotificationsPrimer(
                        nextPrimer = null,
                        dismissalCount = 3,
                    )
                    return true
                }

                else -> {
                    return false
                }
            }
        }

        // Primer is scheduled but not yet due
        return false
    }

    private fun configureNotificationsPrimer(nextPrimer: Date?, dismissalCount: Int) {
        preferences.primer = NotificationsPrimerConfiguration(
            nextPrimer = nextPrimer,
            dismissalCount = dismissalCount
        )
    }

    companion object {
        const val PRIMER_MAX_DISMISSAL_COUNT = 3
        const val PRIMER_INITIAL_RESHOW_DAYS = 7
        const val PRIMER_FOLLOWUP_RESHOW_DAYS = 30
    }
}
