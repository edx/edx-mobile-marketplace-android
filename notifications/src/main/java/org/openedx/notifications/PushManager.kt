package org.openedx.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import org.json.JSONObject
import org.openedx.core.extension.isNotNullOrEmpty
import org.openedx.core.extension.isNull
import org.openedx.core.extension.toMap
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.utils.Logger
import org.openedx.core.utils.addDays
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey
import org.openedx.notifications.presentation.primer.NotificationsPrimerDialogFragment
import java.util.Date

class PushManager(
    private val interactor: NotificationsInteractor,
    private val preferences: NotificationsPreferences,
    private val analytics: NotificationsAnalytics,
) : PushGlobalManager {
    private val logger = Logger(TAG)

    override suspend fun getUnreadNotificationsCount(): Int {
        return interactor.getUnreadNotificationsCount().discussion
    }

    override suspend fun markNotificationAsRead(notificationId: Int) {
        interactor.markNotificationAsRead(notificationId)
    }

    override fun showNotificationsPrimer(
        context: Context,
        fragmentManager: FragmentManager,
    ) {
        if (shouldShowNotificationsPrimer(context)) {
            NotificationsPrimerDialogFragment().show(
                fragmentManager,
                NotificationsPrimerDialogFragment::class.simpleName
            )
        }
    }

    private fun shouldShowNotificationsPrimer(context: Context): Boolean {
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

    override fun logNotificationBellClickedEvent(hasUnreadNotifications: Boolean) {
        val event = NotificationsAnalyticsEvent.NOTIFICATION_BELL_CLICKED
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(NotificationsAnalyticsKey.UNREAD_NOTIFICATIONS.key, hasUnreadNotifications)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
            }
        )
    }

    override fun logNotificationReceivedEvent(data: Map<String, String>) {
        val extras = data[PARAM_EXTRA] ?: return
        val params = try {
            JSONObject(extras)
                .toMap()
                .takeIf { it[PARAM_NOTIFICATION_DOMAIN] == NotificationsAnalyticsKey.DISCUSSION.key }
                ?: return
        } catch (e: Exception) {
            logger.e(
                throwable = IllegalArgumentException("$EXCEPTION_MESSAGE $data", e),
                submitCrashReport = true
            )
            return
        }

        val event = NotificationsAnalyticsEvent.NOTIFICATION_DISCUSSION_PUSH_RECEIVED
        analytics.logEvent(
            event = event.eventName,
            params = buildAnalyticsParams(event, params)
        )
    }

    override fun logNotificationTappedEvent(data: Map<String, String>) {
        if (data[PARAM_NOTIFICATION_DOMAIN] != NotificationsAnalyticsKey.DISCUSSION.key) return

        val event = NotificationsAnalyticsEvent.NOTIFICATION_DISCUSSION_PUSH_TAPPED
        analytics.logEvent(
            event = event.eventName,
            params = buildAnalyticsParams(event, data)
        )
    }

    private fun buildAnalyticsParams(
        event: NotificationsAnalyticsEvent,
        data: Map<String, String>
    ): Map<String, String?> = buildMap {
        put(NotificationsAnalyticsKey.NAME.key, event.biValue)
        put(NotificationsAnalyticsKey.CATEGORY.key, NotificationsAnalyticsKey.NOTIFICATIONS.key)
        putAll(data)
    }.filterValues { it.isNotNullOrEmpty() }

    companion object {
        const val TAG = "PushManager"

        const val PRIMER_MAX_DISMISSAL_COUNT = 3
        const val PRIMER_INITIAL_RESHOW_DAYS = 7
        const val PRIMER_FOLLOWUP_RESHOW_DAYS = 30

        const val PARAM_EXTRA = "extra"
        const val PARAM_NOTIFICATION_DOMAIN = "notification_domain"

        const val EXCEPTION_MESSAGE = "Failed to parse notification data:"
    }
}
