package org.openedx.core.module.subscriptionBanner

import org.openedx.core.config.DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage

class SubscriptionAlertBanner(
    private val corePreferences: CorePreferences,
    private val storage: SubscriptionBannerStorage,
) {
    data class BannerTelemetry(
        val sessionCount: Int,
        val maxSessions: Int,
    )

    enum class Screen(val key: String) {
        DISCOVERY("discovery"),
        PROFILE("profile"),
    }

    fun isBannerVisible(screen: Screen): Boolean {
        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) {
            return false
        }

        if (storage.isSubscriptionBannerDismissed(screen.key)) {
            return false
        }

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)

        val currentAppSession = storage.getSubscriptionBannerSessionCount().coerceAtLeast(1)
        val lastSeenAppSession = storage.getSubscriptionBannerScreenLastSeenAppSession(screen.key)

        val effectiveSessions = if (currentAppSession > lastSeenAppSession) {
            val updated = shownSessions + 1
            storage.setSubscriptionBannerScreenSessionCount(screen.key, updated)
            storage.setSubscriptionBannerScreenLastSeenAppSession(screen.key, currentAppSession)
            updated
        } else {
            shownSessions
        }

        return effectiveSessions <= maxSessions
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

    fun getTelemetry(screen: Screen): BannerTelemetry {
        val maxSessions = corePreferences.appConfig.subscriptionBannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
        return BannerTelemetry(
            sessionCount = storage.getSubscriptionBannerScreenSessionCount(screen.key),
            maxSessions = maxSessions,
        )
    }

    fun getBannerUrl(): String = corePreferences.appConfig.subscriptionBannerConfig.url
}