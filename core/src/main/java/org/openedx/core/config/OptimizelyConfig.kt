package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class OptimizelyConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("SDK_KEY")
    val sdkKey: String = "",
)
