package org.openedx.core.module.subscriptionBanner

import android.util.Log
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

//    fun isBannerVisible(screen: Screen): Boolean {
//        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
//        if (!bannerConfig.isEnabled) {
//            return false
//        }
//
//        if (corePreferences.user == null) {
//            return false
//        }
//
//        if (storage.isSubscriptionBannerDismissed(screen.key)) {
//            return false
//        }
//
//        val maxSessions = bannerConfig.maxSessions
//            .takeIf { it > 0 }
//            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
//
//        Log.d("SubscriptionAlertBanner", "isBannerVisible: screen=${screen.key}, maxSessions=$maxSessions")
//
//        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)
//
//        // If banner has already been shown maxSessions times, hide it permanently
//        if (shownSessions >= maxSessions) {
//            Log.d("SubscriptionAlertBanner", "isBannerVisible: screen=${screen.key}, shownSessions=$shownSessions, maxSessions=$maxSessions, returning false")
//            return false
//        }
//
//        // Always show banner unless we've reached max sessions
//        return true
//    }


    fun isBannerVisible(screen: Screen): Boolean {
        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) return false
        if (corePreferences.user == null) return false
        if (storage.isSubscriptionBannerDismissed(screen.key)) return false

        val maxSessions = bannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)
        val currentAppSession = storage.getSubscriptionBannerSessionCount()
        val maxReachedSession = storage.getSubscriptionBannerMaxReachedSession(screen.key)

        // Hide only if max was reached in a PREVIOUS session
        if (shownSessions >= maxSessions && maxReachedSession < currentAppSession) {
            Log.d("SubscriptionAlertBanner", "isBannerVisible: returning false - maxSessions reached")
            return false
        }

        return true
    }

    fun recordScreenVisit(screen: Screen) {
        val maxSessions = corePreferences.appConfig.subscriptionBannerConfig.maxSessions
            .takeIf { it > 0 }
            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS

        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)
        val currentAppSession = storage.getSubscriptionBannerSessionCount()
        val lastTrackedSession = storage.getSubscriptionBannerScreenLastSeenAppSession(screen.key)

        Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, shownSessions=$shownSessions, lastTrackedSession=$lastTrackedSession, currentAppSession=$currentAppSession")

        if (lastTrackedSession < currentAppSession && shownSessions < maxSessions) {
            storage.setSubscriptionBannerScreenLastSeenAppSession(screen.key, currentAppSession)
            val updated = shownSessions + 1
            Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, updated=$updated")
            storage.setSubscriptionBannerScreenSessionCount(screen.key, updated)

            // Mark when max sessions is reached
            if (updated >= maxSessions) {
                storage.setSubscriptionBannerMaxReachedSession(screen.key, currentAppSession)
            }
        } else if (lastTrackedSession == currentAppSession) {
            Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, already visited in this app session (lastTracked=$lastTrackedSession, current=$currentAppSession)")
        }
    }


//    fun recordScreenVisit(screen: Screen) {
//        val maxSessions = corePreferences.appConfig.subscriptionBannerConfig.maxSessions
//            .takeIf { it > 0 }
//            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
//
//        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)
//
//        // Only increment if we haven't reached max sessions
//        if (shownSessions < maxSessions) {
//            val updated = shownSessions + 1
//            Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, shownSessions=$shownSessions, updated=$updated")
//            storage.setSubscriptionBannerScreenSessionCount(screen.key, updated)
//        }
//    }

//    fun recordScreenVisit(screen: Screen) {
//        val maxSessions = corePreferences.appConfig.subscriptionBannerConfig.maxSessions
//            .takeIf { it > 0 }
//            ?: DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS
//
//        val shownSessions = storage.getSubscriptionBannerScreenSessionCount(screen.key)
//
//        // Only increment if we haven't reached max sessions
//        if (shownSessions < maxSessions) {
//            val currentAppSession = storage.getSubscriptionBannerSessionCount()
//            val lastTrackedSession = storage.getSubscriptionBannerScreenLastSeenAppSession(screen.key)
//                Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, shownSessions=$shownSessions, lastTrackedSession=$lastTrackedSession, currentAppSession=$currentAppSession")
//            // Only increment if this is a new app session (app was backgrounded and foregrounded again)
//            if (lastTrackedSession < currentAppSession) {
//                storage.setSubscriptionBannerScreenLastSeenAppSession(screen.key, currentAppSession)
//                val updated = shownSessions + 1
//                Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, shownSessions=$shownSessions, updated=$updated, appSession=$currentAppSession")
//                storage.setSubscriptionBannerScreenSessionCount(screen.key, updated)
//            } else {
//                Log.d("SubscriptionAlertBanner", "recordScreenVisit: screen=${screen.key}, already visited in this app session (lastTracked=$lastTrackedSession, current=$currentAppSession)")
//            }
//        }
//    }



    fun dismiss(screen: Screen) {
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