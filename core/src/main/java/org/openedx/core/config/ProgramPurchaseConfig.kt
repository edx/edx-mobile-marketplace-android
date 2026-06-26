package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class ProgramPurchaseConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,
    @SerializedName("SKU")
    val sku: String = "",
    @SerializedName("PURCHASE_URL_HOST")
    val purchaseUrlHost: String = "",
    @SerializedName("PROGRAM_URL_PATH")
    val programUrlPath: String = ""
)
