package org.openedx.core.presentation.iap

import com.android.billingclient.api.BillingClient
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.nonZero
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.utils.TimeUtils

class IAPEventLogger(
    private val analytics: IAPAnalytics,
    private val purchaseFlowData: PurchaseFlowData,
) {
    fun upgradeNowClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UPGRADE_NOW_CLICKED)
    }

    fun upgradeSuccessEvent() {
        val elapsedTime = TimeUtils.getCurrentTime() - purchaseFlowData.flowStartTime
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_SUCCESS, buildMap {
            put(IAPAnalyticsKeys.ELAPSED_TIME.key, elapsedTime)
        }.toMutableMap())
    }

    private fun purchaseErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    private fun canceledByUserEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_CANCELED)
    }

    private fun courseUpgradeErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    private fun priceLoadErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PRICE_LOAD_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    fun logExceptionEvent(iapException: IAPException) {
        val feedbackErrorMessage: String = iapException.getFormattedErrorMessage()
        when (iapException.requestType) {
            IAPRequestType.PAYMENT_SDK_CODE -> {
                if (BillingClient.BillingResponseCode.USER_CANCELED == iapException.httpErrorCode) {
                    canceledByUserEvent()
                } else {
                    purchaseErrorEvent(feedbackErrorMessage)
                }
            }

            IAPRequestType.PRICE_CODE,
            IAPRequestType.NO_SKU_CODE,
            -> {
                priceLoadErrorEvent(feedbackErrorMessage)
            }

            else -> {
                courseUpgradeErrorEvent(feedbackErrorMessage)
            }
        }
    }

    fun logIAPErrorActionEvent(alertType: String, action: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, buildMap {
            put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, alertType)
            put(IAPAnalyticsKeys.ERROR_ACTION.key, action)
        }.toMutableMap())
    }

    fun loadIAPScreenEvent() {
        val event = IAPAnalyticsEvent.IAP_VALUE_PROP_VIEWED
        val params = buildMap {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            purchaseFlowData.screenName?.takeIfNotEmpty()?.let { screenName ->
                put(IAPAnalyticsKeys.SCREEN_NAME.key, screenName)
            }
            putAll(getIAPEventParams())
        }
        analytics.logScreenEvent(screenName = event.eventName, params = params)
    }

    private fun getIAPEventParams(): MutableMap<String, Any?> {
        return buildMap {
            purchaseFlowData.takeIf { it.courseId.isNullOrBlank().not() }?.let {
                put(IAPAnalyticsKeys.COURSE_ID.key, purchaseFlowData.courseId)
                put(
                    IAPAnalyticsKeys.PACING.key,
                    if (purchaseFlowData.isSelfPaced == true) IAPAnalyticsKeys.SELF.key else IAPAnalyticsKeys.INSTRUCTOR.key
                )
            }
            purchaseFlowData.productInfo?.lmsUSDPrice?.nonZero()?.let { lmsUSDPrice ->
                put(IAPAnalyticsKeys.LMS_USD_PRICE.key, lmsUSDPrice)
            }
            purchaseFlowData.price.nonZero()?.let { localizedPrice ->
                put(IAPAnalyticsKeys.LOCALIZED_PRICE.key, localizedPrice)
            }
            purchaseFlowData.currencyCode.takeIfNotEmpty()?.let { currencyCode ->
                put(IAPAnalyticsKeys.CURRENCY_CODE.key, currencyCode)
            }
            purchaseFlowData.componentId?.takeIf { it.isNotBlank() }?.let { componentId ->
                put(IAPAnalyticsKeys.COMPONENT_ID.key, componentId)
            }
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
        }.toMutableMap()
    }

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: MutableMap<String, Any?> = mutableMapOf(),
    ) {
        params.apply {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            putAll(getIAPEventParams())
        }
        analytics.logIAPEvent(
            event = event,
            params = params,
            screenName = purchaseFlowData.screenName.orEmpty()
        )
    }
}
