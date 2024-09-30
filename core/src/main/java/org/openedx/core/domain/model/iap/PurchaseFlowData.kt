package org.openedx.core.domain.model.iap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PurchaseFlowData(
    var iapFlow: IAPFlow? = null,
    var screenName: String? = null,
    var courseId: String? = null,
    var courseName: String? = null,
    var isSelfPaced: Boolean? = null,
    var componentId: String? = null,
    var productInfo: ProductInfo? = null,
) : Parcelable {

    var currencyCode: String = ""
    var price: Double = 0.0
    var formattedPrice: String? = null
    var purchaseToken: String? = null
    var basketId: Long = -1

    var flowStartTime: Long = 0

    fun reset() {
        iapFlow = null
        screenName = null
        courseId = null
        courseName = null
        isSelfPaced = null
        componentId = null
        productInfo = null
        currencyCode = ""
        price = 0.0
        formattedPrice = null
        purchaseToken = null
        basketId = -1
        flowStartTime = 0
    }
}

enum class IAPFlow(val value: String) {
    RESTORE("restore"),
    SILENT("silent"),
    USER_INITIATED("user_initiated");

    fun value(): String {
        return this.name.lowercase()
    }
}
