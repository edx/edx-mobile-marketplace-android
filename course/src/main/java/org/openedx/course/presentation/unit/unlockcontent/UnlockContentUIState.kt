package org.openedx.course.presentation.unit.unlockcontent

import org.openedx.core.exception.iap.IAPException
import org.openedx.core.presentation.global.ErrorType

sealed class UnlockContentUIState {
    data object Loading : UnlockContentUIState()
    data class ProductData(val formattedPrice: String) : UnlockContentUIState()
    data object Empty : UnlockContentUIState()  // No upgrade button in case of upgrade disabled
    data class Error(val errorType: ErrorType) : UnlockContentUIState()
}

sealed class UnlockContentUIAction {
    data object Loading : UnlockContentUIAction()
    data object FullScreenLoader : UnlockContentUIAction()
    data class Error(val iapException: IAPException) : UnlockContentUIAction()
    data object Clear : UnlockContentUIAction()
    data object None : UnlockContentUIAction()
}
