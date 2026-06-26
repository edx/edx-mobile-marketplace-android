package org.openedx.core.data.repository.iap

import org.openedx.core.data.api.iap.InAppPurchasesApi
import org.openedx.core.domain.model.iap.CreateOrderResponse
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.exception.iap.getMessage
import org.openedx.core.presentation.iap.IAPRequestType

class ProgramPurchaseRepository(private val api: InAppPurchasesApi) {

    suspend fun createProgramOrder(
        programUuid: String,
        currencyCode: String,
        price: Double,
        paymentProcessor: String,
        purchaseToken: String,
    ): CreateOrderResponse {
        val response = api.createProgramOrder(
            programUuid = programUuid,
            currencyCode = currencyCode,
            price = price,
            paymentProcessor = paymentProcessor,
            purchaseToken = purchaseToken
        )
        if (response.isSuccessful) {
            response.body()?.run {
                return mapToDomain()
            }
        }
        throw IAPException(
            requestType = IAPRequestType.CREATE_ORDER_CODE,
            httpErrorCode = response.code(),
            errorMessage = response.getMessage()
        )
    }
}
