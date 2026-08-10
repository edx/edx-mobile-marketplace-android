package org.openedx.core.data.storage

interface SubscriptionBannerStorage {
    fun getSubscriptionBannerSessionCount(): Int
    fun setSubscriptionBannerSessionCount(value: Int)
    fun getSubscriptionBannerScreenSessionCount(screenKey: String): Int
    fun setSubscriptionBannerScreenSessionCount(screenKey: String, value: Int)
    fun getSubscriptionBannerScreenLastSeenAppSession(screenKey: String): Int
    fun setSubscriptionBannerScreenLastSeenAppSession(screenKey: String, value: Int)
    fun getSubscriptionBannerDismissCount(screenKey: String): Int
    fun setSubscriptionBannerDismissCount(screenKey: String, value: Int)
    fun isSubscriptionBannerDismissed(screenKey: String): Boolean
    fun setSubscriptionBannerDismissed(screenKey: String, dismissed: Boolean)
}

