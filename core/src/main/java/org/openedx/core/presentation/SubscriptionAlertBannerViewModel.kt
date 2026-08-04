package org.openedx.core.presentation

import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.utils.Logger

class SubscriptionAlertBannerViewModel(
    private val corePreferences: CorePreferences,
    private val storage: SubscriptionBannerStorage,
) {

    private val logger = Logger("SubscriptionBanner")

    enum class Screen(val key: String) {
        DISCOVERY("discovery"),
        PROFILE("profile"),
    }

    fun isBannerVisible(screen: Screen): Boolean {
        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) {
            logger.i { "Banner disabled from config: screen=${screen.key}" }
            return false
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val appSessionCount = storage.getSubscriptionBannerSessionCount()
        if (appSessionCount <= 0) {
            logger.i { "Banner hidden: screen=${screen.key} app session not started yet" }
            return false
        }

        val dismissCount = storage.getSubscriptionBannerDismissCount(screen.key)
        if (dismissCount >= maxSessions) {
            logger.i { "Banner hidden: screen=${screen.key} reached dismiss limit ($dismissCount/$maxSessions)" }
            return false
        }

        val isDismissed = storage.isSubscriptionBannerDismissed(screen.key)
        if (isDismissed) {
            logger.i { "Banner dismissed for screen=${screen.key}" }
            return false
        }

        val visible = true
        logger.i {
            "Banner decision: screen=${screen.key}, enabled=${bannerConfig.isEnabled}, appSessionCount=$appSessionCount, maxDismisses=$maxSessions, dismissCount=$dismissCount, visible=$visible"
        }
        return visible
    }

    fun dismiss(screen: Screen) {
        val maxDismisses = corePreferences.appConfig.subscriptionBannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
        val currentCount = storage.getSubscriptionBannerDismissCount(screen.key)
        if (currentCount < maxDismisses) {
            storage.setSubscriptionBannerDismissCount(screen.key, currentCount + 1)
        }
        storage.setSubscriptionBannerDismissed(screen.key, true)
    }

    fun getBannerUrl(): String = corePreferences.appConfig.subscriptionBannerConfig.url
}
