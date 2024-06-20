package org.openedx.core.presentation.iap

sealed class IAPUIState {
    data class ProductData(val courseName: String, val formattedPrice: String) : IAPUIState()
    data object PurchaseProduct : IAPUIState()
    data object PurchasesFulfillmentCompleted : IAPUIState()
    data object FakePurchasesFulfillmentCompleted : IAPUIState()
    data object CourseDataUpdated : IAPUIState()
    data class Loading(val courseName: String, val loaderType: IAPLoaderType) : IAPUIState()
    data class Error(
        val courseName: String,
        val requestType: Int = -1,
        val feedbackErrorMessage: String = ""
    ) : IAPUIState()

    data object Clear : IAPUIState()
}

enum class IAPLoaderType {
    PRICE, PURCHASE_FLOW, FULL_SCREEN, RESTORE_PURCHASES
}

enum class IAPFlow {
    RESTORE,
    SILENT,
    USER_INITIATED;

    fun value(): String {
        return this.name.lowercase()
    }
}
