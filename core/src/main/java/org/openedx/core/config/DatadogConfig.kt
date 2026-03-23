package org.openedx.core.config

data class DatadogConfig(
    val ENABLED: Boolean = false,
    val CLIENT_TOKEN: String = "",
    val ENVIRONMENT: String = ""
)