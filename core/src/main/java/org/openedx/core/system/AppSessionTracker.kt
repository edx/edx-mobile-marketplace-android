package org.openedx.core.system

import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage

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


        val nextSessionCount = storage.getSubscriptionBannerSessionCount() + 1
        storage.setSubscriptionBannerSessionCount(nextSessionCount)
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
    }
}
