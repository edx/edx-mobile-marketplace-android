package org.openedx.core.domain.model.iap

import org.openedx.core.domain.ProductInfo

data class PurchaseFlowData(
    val screenName: String? = null,
    val courseId: String? = null,
    val courseName: String? = null,
    val isSelfPaced: Boolean? = null,
    val componentId: String? = null,
    val productInfo: ProductInfo? = null,
) {
    var currencyCode: String = ""
    var price: Double = 0.0
    var formattedPrice: String? = null
    var purchaseToken: String? = null
    var basketId: Long = -1

    var flowStartTime: Long = 0
}
