package org.openedx.discovery.domain.interactor

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.ProductDetails
import org.openedx.core.ApiConstants
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.data.repository.iap.ProgramPurchaseRepository
import org.openedx.core.data.storage.CorePreferences
import org.openedx.discovery.presentation.catalog.WebViewLink

class ProgramIAPInteractor(
    private val iapInteractor: IAPInteractor,
    private val programPurchaseConfigInteractor: ProgramPurchaseConfigInteractor,
    private val programPurchaseRepository: ProgramPurchaseRepository,
    private val billingProcessor: BillingProcessor,
    private val preferencesManager: CorePreferences
) {

    val isIAPEnabled: Boolean
        get() = programPurchaseConfigInteractor.isEnabled()

    /**
     * Parses the program purchase link and returns populated PurchaseFlowData.
     */
    fun parsePurchaseLink(
        rawLink: String,
        pathId: String
    ): PurchaseFlowData {
        val config = programPurchaseConfigInteractor.getConfig()

        if (!config.enabled) {
            throw IAPException(
                requestType = IAPRequestType.NO_SKU_CODE,
                httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                errorMessage = "Program purchase is disabled"
            )
        }

        val uri = runCatching {
            Uri.parse(rawLink.replace("+", "%2B"))
        }.getOrNull()

        val courseId = uri?.getQueryParameter(WebViewLink.Param.COURSE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.PATH_ID)
                ?.takeIf { it.isNotBlank() }
            ?: pathId

        if (courseId.isBlank()) {
            throw IAPException(
                requestType = IAPRequestType.NO_SKU_CODE,
                httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                errorMessage = "Program UUID missing"
            )
        }

        val price = uri?.getQueryParameter(WebViewLink.Param.PRICE)
            ?.toDoubleOrNull()
            ?: 0.0

        val title = uri?.getQueryParameter(WebViewLink.Param.TITLE)
            ?.takeIf { it.isNotBlank() }

        return PurchaseFlowData(
            courseId = courseId,
            courseName = title,
            iapFlow = IAPFlow.USER_INITIATED,
            screenName = IAPFlowSource.COURSE_ENROLLMENT.screen,
            productInfo = ProductInfo(
                storeSku = config.sku,
                lmsUSDPrice = price
            )
        )
    }

    suspend fun loadPrice(): ProductDetails.OneTimePurchaseOfferDetails {
        val sku = programPurchaseConfigInteractor.getSku()
        if (sku.isBlank()) {
            throw IAPException(
                requestType = IAPRequestType.NO_SKU_CODE,
                httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                errorMessage = "Program SKU not configured"
            )
        }
        return iapInteractor.loadPrice(sku)
    }

    suspend fun purchaseItem(
        activity: FragmentActivity,
        programUuid: String,
        productInfo: ProductInfo,
        purchaseListeners: BillingProcessor.PurchaseListeners
    ) {
        preferencesManager.user?.id?.let { id ->
            billingProcessor.setPurchaseListener(purchaseListeners)
            billingProcessor.purchaseItem(activity, id, programUuid, productInfo)
        }
    }

    suspend fun createProgramOrder(
        programUuid: String,
        currencyCode: String,
        price: Double,
        purchaseToken: String,
    ) {
        programPurchaseRepository.createProgramOrder(
            programUuid = programUuid,
            currencyCode = currencyCode,
            price = price,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR,
            purchaseToken = purchaseToken
        )
    }

    suspend fun consumePurchase(purchaseToken: String) {
        iapInteractor.consumePurchaseByToken(purchaseToken)
    }
}
