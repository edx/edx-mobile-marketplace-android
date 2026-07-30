package org.openedx.core.system

import org.openedx.core.config.Config
import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.presentation.SubscriptionAlertBannerViewModel
import org.openedx.core.utils.Logger

class AppSessionTracker(
    private val config: Config,
    private val storage: SubscriptionBannerStorage,
) {
    private val logger = Logger("AppSessionTracker")

    fun onAppForegrounded() {
        val bannerConfig = config.getSubscriptionBannerConfig()
        if (!bannerConfig.isEnabled) {
            logger.i { "Session not counted because banner is disabled in config" }
            return
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val nextSessionCount = storage.getSubscriptionBannerSessionCount() + 1
        storage.setSubscriptionBannerSessionCount(nextSessionCount)
        logger.i { "Session incremented to $nextSessionCount (maxSessions=$maxSessions)" }

        // Reset per-screen dismissal flags so the banner reappears each new session
        // (as long as sessionCount is within the allowed range).
        SubscriptionAlertBannerViewModel.Screen.entries.forEach { screen ->
            storage.setSubscriptionBannerDismissed(screen.key, false)
            logger.i { "Dismissed flag reset for screen=${screen.key} on new session $nextSessionCount" }
        }
    }
}
