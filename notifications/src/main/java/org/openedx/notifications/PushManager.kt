package org.openedx.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import org.openedx.core.extension.isNull
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.utils.addDays
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration
import org.openedx.notifications.presentation.primer.NotificationsPrimerDialogFragment
import java.util.Date

class PushManager(
    private val interactor: NotificationsInteractor,
    private val preferences: NotificationsPreferences,
) : PushGlobalManager {

    override suspend fun getUnreadNotificationsCount(): Int {
        return interactor.getUnreadNotificationsCount().discussion
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

    override suspend fun markNotificationAsRead(notificationId: Int) {
        interactor.markNotificationAsRead(notificationId)
    }

    companion object {
        const val PRIMER_MAX_DISMISSAL_COUNT = 3
        const val PRIMER_INITIAL_RESHOW_DAYS = 7
        const val PRIMER_FOLLOWUP_RESHOW_DAYS = 30
    }
}
