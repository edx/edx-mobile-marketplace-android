package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class RecaptchaConfig(
    @SerializedName("ENABLED")
    val isEnabled: Boolean = false,

    @SerializedName("SITE_KEY")
    val siteKey: String = "",
)
