package org.openedx.core.system

import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.presentation.SubscriptionAlertBannerViewModel
import org.openedx.core.utils.Logger

class AppSessionTracker(
    private val corePreferences: CorePreferences,
    private val storage: SubscriptionBannerStorage,
) {
    private val logger = Logger("AppSessionTracker")
    private var isAppInForeground = false

    fun onAppForegrounded() {
        if (isAppInForeground) {
            logger.i { "Foreground event ignored because app is already in foreground" }
            return
        }

        isAppInForeground = true

        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) {
            logger.i { "Session not counted because banner is disabled in config" }
            return
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        // Reset per-screen dismiss flags for the new app session.
        SubscriptionAlertBannerViewModel.Screen.entries.forEach { screen ->
            storage.setSubscriptionBannerDismissed(screen.key, false)
            logger.i { "Screen=${screen.key}: dismiss flag reset for new app session" }
        }

        // Global app session counter.
        val nextSessionCount = storage.getSubscriptionBannerSessionCount() + 1
        storage.setSubscriptionBannerSessionCount(nextSessionCount)
        logger.i { "App session incremented to $nextSessionCount (maxSessions=$maxSessions)" }
    }

    fun onAppBackgrounded() {
        if (isAppInForeground) {
            logger.i { "App moved to background" }
        }
        isAppInForeground = false
    }
}
