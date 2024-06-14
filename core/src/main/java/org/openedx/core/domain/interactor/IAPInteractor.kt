package org.openedx.core.domain.interactor

import android.text.TextUtils
import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.openedx.core.ApiConstants
import org.openedx.core.data.repository.iap.IAPRepository
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.getAuditCourses
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.decodeToLong
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount

class IAPInteractor(
    private val billingProcessor: BillingProcessor,
    private val repository: IAPRepository,
) {

    suspend fun loadPrice(productId: String): ProductDetails.OneTimePurchaseOfferDetails {
        val response =
            billingProcessor.querySyncDetails(productId)
        val productDetail = response.productDetailsList?.firstOrNull()
        val billingResult = response.billingResult
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetail?.oneTimePurchaseOfferDetails != null) {
            return productDetail.oneTimePurchaseOfferDetails!!
        } else {
            throw IAPException(
                httpErrorCode = billingResult.responseCode,
                errorMessage = billingResult.debugMessage
            )
        }
    }

    suspend fun addToBasketAndCheckout(courseSku: String): Long {
        val basketResponse = repository.addToBasket(courseSku)
        repository.proceedCheckout(basketResponse.basketId)
        return basketResponse.basketId
    }

    suspend fun purchaseItem(
        activity: FragmentActivity,
        id: Long,
        productInfo: ProductInfo,
        purchaseListeners: BillingProcessor.PurchaseListeners,
    ) {
        billingProcessor.setPurchaseListener(purchaseListeners)
        billingProcessor.purchaseItem(activity, id, productInfo)
    }

    suspend fun consumePurchase(purchaseToken: String) {
        val result = billingProcessor.consumePurchase(purchaseToken)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw IAPException(result.responseCode, result.debugMessage)
        }
    }

    suspend fun executeOrder(
        basketId: Long,
        purchaseToken: String,
        price: Double,
        currencyCode: String
    ) {
        repository.executeOrder(
            basketId = basketId,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR,
            purchaseToken = purchaseToken,
            price = price,
            currencyCode = currencyCode,
        )
    }

    suspend fun processUnfulfilledPurchase(courses: List<EnrolledCourse>, userId: Long): Boolean {
        val auditCourses = courses.getAuditCourses()
        val purchases = billingProcessor.queryPurchases()
        val userPurchases =
            purchases.filter { it.accountIdentifiers?.obfuscatedAccountId?.decodeToLong() == userId }
                .filter {
                    auditCourses.any { courseItem -> courseItem.productInfo?.courseSku == it.getCourseSku() }
                }
        if (userPurchases.isNotEmpty()) {
            startUnfulfilledVerification(userPurchases)
            return true
        } else {
            purchases.forEach {
                billingProcessor.consumePurchase(it.purchaseToken)
            }
        }
        return false
    }

    private suspend fun startUnfulfilledVerification(userPurchases: List<Purchase>) {
        userPurchases.forEach { purchase ->
            val productDetail =
                billingProcessor.querySyncDetails(purchase.products.first()).productDetailsList?.firstOrNull()
            productDetail?.oneTimePurchaseOfferDetails?.takeIf {
                TextUtils.isEmpty(purchase.getCourseSku()).not()
            }?.let { oneTimeProductDetails ->
                val basketId = addToBasketAndCheckout(purchase.getCourseSku()!!)
                executeOrder(
                    basketId = basketId,
                    purchaseToken = purchase.purchaseToken,
                    price = oneTimeProductDetails.getPriceAmount(),
                    currencyCode = oneTimeProductDetails.priceCurrencyCode,
                )
                consumePurchase(purchase.purchaseToken)
            }
        }
    }
}
