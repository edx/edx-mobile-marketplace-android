package org.openedx.core.module.subscriptionBanner

import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage

class SubscriptionAlertBanner(
    private val corePreferences: CorePreferences,
    private val storage: SubscriptionBannerStorage,
) {
    enum class Screen(val key: String) {
        DISCOVERY("discovery"),
        PROFILE("profile"),
    }

    fun isBannerVisible(screen: Screen): Boolean {
        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) {
            return false
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val dismissCount = storage.getSubscriptionBannerDismissCount(screen.key)
        if (dismissCount >= maxSessions) {
            return false
        }

        val isDismissed = storage.isSubscriptionBannerDismissed(screen.key)
        if (isDismissed) {
            return false
        }

        val visible = true
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