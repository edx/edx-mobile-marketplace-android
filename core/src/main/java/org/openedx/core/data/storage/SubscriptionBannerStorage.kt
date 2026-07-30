package org.openedx.core.data.storage

interface SubscriptionBannerStorage {
    fun getSubscriptionBannerSessionCount(): Int
    fun setSubscriptionBannerSessionCount(value: Int)
    fun isSubscriptionBannerDismissed(screenKey: String): Boolean
    fun setSubscriptionBannerDismissed(screenKey: String, dismissed: Boolean)
}

