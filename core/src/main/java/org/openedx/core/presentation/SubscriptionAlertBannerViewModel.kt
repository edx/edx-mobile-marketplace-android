package org.openedx.core.presentation

import org.openedx.core.config.Config
import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.utils.Logger

class SubscriptionAlertBannerViewModel(
    private val config: Config,
    private val storage: SubscriptionBannerStorage,
) {

    private val logger = Logger("SubscriptionBanner")

    enum class Screen(val key: String) {
        DISCOVERY("discovery"),
        PROFILE("profile"),
    }

    fun isBannerVisible(screen: Screen): Boolean {
        val bannerConfig = config.getSubscriptionBannerConfig()
        if (!bannerConfig.isEnabled) {
            logger.i { "Banner disabled from config: screen=${screen.key}" }
            return false
        }

        val isDismissed = storage.isSubscriptionBannerDismissed(screen.key)
        if (isDismissed) {
            logger.i { "Banner dismissed for screen=${screen.key}" }
            return false
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
        val sessionCount = storage.getSubscriptionBannerSessionCount()
        val visible = sessionCount in 1..maxSessions

        logger.i {
            "Banner decision: screen=${screen.key}, enabled=${bannerConfig.isEnabled}, maxSessions=$maxSessions, sessionCount=$sessionCount, visible=$visible"
        }
        return visible
    }

    fun dismiss(screen: Screen) {
        storage.setSubscriptionBannerDismissed(screen.key, true)
    }

    fun getBannerUrl(): String = config.getSubscriptionBannerConfig().url
}
