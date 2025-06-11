package org.openedx.core.domain.model.iap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PurchaseFlowData(
    var iapFlow: IAPFlow? = null,
    var screenName: String? = null,
    var courseId: String? = null,
    var courseName: String? = null,
    var courseExpiresDate: String? = null,
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
        currencyCode = ""
        price = 0.0
        formattedPrice = null
        purchaseToken = null
        basketId = -1
        flowStartTime = 0
    }

    fun isSilentIAPFlow(): Boolean? {
        return when (iapFlow) {
            IAPFlow.SILENT -> {
                true
            }

            IAPFlow.RESTORE -> {
                false
            }

            else -> {
                null
            }
        }
    }
}

enum class IAPFlow(val value: String) {
    RESTORE("restore"),
    SILENT("silent"),
    USER_INITIATED("user_initiated"),
    TRACK_SELECTION("track_selection"),
    UNLOCK_COMPONENT_USER_INITIATED("unlock_component_user_initiated");

    fun value(): String {
        return this.name.lowercase()
    }
}

enum class IAPFlowSource(val screen: String) {
    COURSE_ENROLLMENT("course_enrollment"),
    COURSE_DASHBOARD("course_dashboard"),
    COURSE_COMPONENT("course_component"),
    PROFILE("profile"),
}
