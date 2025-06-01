package org.openedx.core.domain.interactor

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.openedx.core.ApiConstants
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.repository.iap.IAPRepository
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.toIAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseId
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.module.billing.getUserId
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.EmailUtil
import org.openedx.core.utils.Logger
import org.openedx.core.utils.TimeUtils

class IAPInteractor(
    private val appData: AppData,
    private val billingProcessor: BillingProcessor,
    private val config: Config,
    private val repository: IAPRepository,
    private val preferencesManager: CorePreferences,
    private val resourceManager: ResourceManager,
) {
    private val logger = Logger(TAG)
    private val iapConfig
        get() = preferencesManager.appConfig.iapConfig
    val isIAPEnabled
        get() = iapConfig.isEnabled
    val isUpgradeEnabled
        get() = iapConfig.isUpgradeEnabled(appData.versionName)

    fun showFeedbackScreen(context: Context, message: String) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            subject = context.getString(R.string.core_error_upgrading_course_in_app),
            feedback = message,
            appVersion = appData.versionName
        )
    }

    suspend fun loadPrice(productId: String): ProductDetails.OneTimePurchaseOfferDetails {
        val response = billingProcessor.querySyncDetails(productId)
        val productDetails = response.productDetailsList?.firstOrNull()?.oneTimePurchaseOfferDetails
        val billingResult = response.billingResult

        if (billingResult.responseCode == BillingResponseCode.OK) {
            if (productDetails != null) {
                return productDetails
            } else {
                throw IAPException(
                    requestType = IAPRequestType.NO_SKU_CODE,
                    httpErrorCode = billingResult.responseCode,
                    errorMessage = billingResult.debugMessage
                )
            }
        } else {
            throw IAPException(
                requestType = IAPRequestType.PRICE_CODE,
                httpErrorCode = billingResult.responseCode,
                errorMessage = billingResult.debugMessage
            )
        }
    }

    suspend fun purchaseItem(
        activity: FragmentActivity,
        courseId: String,
        productInfo: ProductInfo,
        purchaseListeners: BillingProcessor.PurchaseListeners,
    ) {
        preferencesManager.user?.id?.let { id ->
            billingProcessor.setPurchaseListener(purchaseListeners)
            billingProcessor.purchaseItem(activity, id, courseId, productInfo)
        }
    }

    suspend fun createOrder(
        courseId: String,
        currencyCode: String,
        price: Double,
        purchaseToken: String,
    ) {
        repository.createOrder(
            courseId = courseId,
            currencyCode = currencyCode,
            price = price,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR,
            purchaseToken = purchaseToken
        )
    }

    suspend fun consumePurchaseByToken(purchaseToken: String) {
        val result = billingProcessor.consumePurchase(purchaseToken)
        if (result.responseCode != BillingResponseCode.OK) {
            throw IAPException(
                requestType = IAPRequestType.CONSUME_CODE,
                httpErrorCode = result.responseCode,
                errorMessage = result.debugMessage
            )
        }
    }

    suspend fun consumePurchaseByCourseId(enrolledCourseId: String) {
        val purchases = billingProcessor.queryPurchases()
        val purchasedCourse = purchases.firstOrNull { purchase ->
            val userAccountId = purchase.getUserId()
            val courseId = purchase.getCourseId()

            userAccountId == preferencesManager.user?.id && courseId == enrolledCourseId
        }
        purchasedCourse?.purchaseToken?.let {
            consumePurchaseByToken(it)
        }
    }

    suspend fun processUnfulfilledPurchase(
        userId: Long,
        enrolledCourses: List<EnrolledCourse>,
        verificationInitiated: (PurchaseFlowData) -> Unit = {},
    ): PurchaseFlowData? {
        val purchases = billingProcessor.queryPurchases()
        val userPurchases = purchases.filter { purchase ->
            val userAccountId = purchase.getUserId()
            val courseId = purchase.getCourseId()

            userAccountId == userId && enrolledCourses.any { enrolledCourse ->
                courseId == enrolledCourse.course.id
            }
        }
        if (userPurchases.isNotEmpty()) {
            userPurchases.first().let { purchase ->
                val courseVerified = enrolledCourses.find { enrolledCourse ->
                    enrolledCourse.course.id == purchase.getCourseId()
                }
                courseVerified?.let {
                    val productDetails =
                        billingProcessor.querySyncDetails(purchase.products[0]).productDetailsList?.firstOrNull()
                    val purchaseProductFlow = PurchaseFlowData(
                        courseId = courseVerified.course.id,
                        isSelfPaced = courseVerified.course.isSelfPaced,
                        productInfo = courseVerified.productInfo,
                    ).apply {
                        this.purchaseToken = purchase.purchaseToken
                        productDetails?.oneTimePurchaseOfferDetails?.let {
                            this.price = it.getPriceAmount()
                            this.currencyCode = it.priceCurrencyCode
                        }
                        this.flowStartTime = TimeUtils.getCurrentTime()
                    }
                    verificationInitiated(purchaseProductFlow)
                    startUnfulfilledVerification(courseVerified.course.id, purchase)
                    return purchaseProductFlow
                }
            }
        } else {
            purchases.forEach {
                billingProcessor.consumePurchase(it.purchaseToken)
            }
        }
        return null
    }

    private suspend fun startUnfulfilledVerification(courseId: String, userPurchase: Purchase) {
        val productDetail =
            billingProcessor.querySyncDetails(userPurchase.products.first()).productDetailsList?.firstOrNull()
        productDetail?.oneTimePurchaseOfferDetails?.takeIf {
            userPurchase.getCourseId().isNullOrEmpty().not()
        }?.let { oneTimeProductDetails ->
            createOrder(
                courseId = courseId,
                currencyCode = oneTimeProductDetails.priceCurrencyCode,
                price = oneTimeProductDetails.getPriceAmount(),
                purchaseToken = userPurchase.purchaseToken,
            )
        }
    }

    suspend fun detectUnfulfilledPurchase(
        enrolledCourses: List<EnrolledCourse>,
        verificationInitiated: (PurchaseFlowData) -> Unit,
        onSuccess: (PurchaseFlowData) -> Unit,
        onFailure: (IAPException) -> Unit,
    ) {
        if (isUpgradeEnabled) {
            preferencesManager.user?.id?.let { userId ->
                runCatching {
                    processUnfulfilledPurchase(userId, enrolledCourses, verificationInitiated)
                }.onSuccess { purchaseFlowData ->
                    purchaseFlowData?.let {
                        onSuccess(purchaseFlowData)
                    }
                }.onFailure {
                    logger.e(throwable = it)
                    onFailure(
                        it.toIAPException(
                            requestType = IAPRequestType.UNFULFILLED_CODE,
                            defaultMessage = resourceManager.getString(R.string.core_error_unknown_error)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "IAPInteractor"
    }
}
