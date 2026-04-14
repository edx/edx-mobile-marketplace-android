package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class DatadogConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = true,

    @SerializedName("CLIENT_TOKEN")
    val clientToken: String = "",

    @SerializedName("ENVIRONMENT")
    val environment: String = ""
)