package org.openedx.course.presentation.unit.unlockcontent

import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.presentation.global.ErrorType

sealed class UnlockContentUIState {
    data object Loading : UnlockContentUIState()
    data class ProductData(val formattedPrice: String) : UnlockContentUIState()
    data class Error(val errorType: ErrorType) : UnlockContentUIState()
}

sealed class UnlockContentUIAction {
    data object Loading : UnlockContentUIAction()
    data class UpgradeButton(val formattedPrice: String) : UnlockContentUIAction()
    data class FullScreenLoader(val purchaseFlowData: PurchaseFlowData) : UnlockContentUIAction()
    data class Error(val iapException: IAPException) : UnlockContentUIAction()
    data object Clear : UnlockContentUIAction()
    data object None : UnlockContentUIAction()
}
