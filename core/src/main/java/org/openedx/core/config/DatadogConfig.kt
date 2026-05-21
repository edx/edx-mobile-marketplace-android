package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class DatadogConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName(value = "CLIENT_TOKEN", alternate = ["DATADOG_CLIENT_TOKEN"])
    val clientToken: String = "",

    @SerializedName(value = "ENVIRONMENT", alternate = ["DATADOG_ENVIRONMENT"])
    val environment: String = "",

    @SerializedName(value = "APPLICATION_ID", alternate = ["DATADOG_APPLICATION_ID"])
    val applicationId: String = "",
)