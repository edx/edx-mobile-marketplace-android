package org.openedx.core.config

import com.google.gson.annotations.SerializedName

const val DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS = 4

data class SubscriptionBannerConfig(

    @SerializedName(value = "subscription_banner_enabled", alternate = ["SUBSCRIPTION_BANNER_ENABLED"])
    val isEnabled: Boolean = true,

    @SerializedName(value = "max_sessions", alternate = ["MAX_SESSIONS"])
    val maxSessions: Int = DEFAULT_SUBSCRIPTION_BANNER_MAX_SESSIONS,

    @SerializedName(value = "url", alternate = ["URL"])
    val url: String = ""
)
