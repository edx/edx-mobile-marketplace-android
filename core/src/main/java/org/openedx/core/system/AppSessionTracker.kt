package org.openedx.core.system

import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.presentation.SubscriptionAlertBannerViewModel

class AppSessionTracker(
    private val corePreferences: CorePreferences,
    private val storage: SubscriptionBannerStorage,
) {
    private var isAppInForeground = false

    fun onAppForegrounded() {
        if (isAppInForeground) {
            return
        }

        isAppInForeground = true

        val bannerConfig = corePreferences.appConfig.subscriptionBannerConfig
        if (!bannerConfig.isEnabled) {
            return
        }

        val nextSessionCount = storage.getSubscriptionBannerSessionCount() + 1
        storage.setSubscriptionBannerSessionCount(nextSessionCount)

        SubscriptionAlertBannerViewModel.Screen.entries.forEach { screen ->
            storage.setSubscriptionBannerDismissed(screen.key, false)
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
    }
}
