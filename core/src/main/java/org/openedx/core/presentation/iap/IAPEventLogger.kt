package org.openedx.core.presentation.iap

import com.android.billingclient.api.BillingClient
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isNull
import org.openedx.core.extension.isTrue
import org.openedx.core.extension.nonZero
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.utils.TimeUtils

class IAPEventLogger(
    private val analytics: IAPAnalytics,
    private val purchaseFlowData: PurchaseFlowData? = null,
    private val isSilentIAPFlow: Boolean? = null,
) {
    fun upgradeNowClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UPGRADE_NOW_CLICKED)
    }

    fun upgradeSuccessEvent() {
        val elapsedTime = TimeUtils.getCurrentTime() - (purchaseFlowData?.flowStartTime ?: 0L)
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_SUCCESS, buildMap {
            put(IAPAnalyticsKeys.ELAPSED_TIME.key, elapsedTime)
        })
    }

    private fun purchaseErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        })
    }

    private fun canceledByUserEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_CANCELED)
    }

    private fun courseUpgradeErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        })
    }

    private fun priceLoadErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PRICE_LOAD_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        })
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
        })
    }

    fun logRestorePurchasesClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_RESTORE_PURCHASE_CLICKED)
    }

    fun logUnfulfilledPurchaseInitiatedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UNFULFILLED_PURCHASE_INITIATED)
    }

    fun logGetHelpEvent() {
        logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            params = buildMap {
                put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_UNFULFILLED.action)
                put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_GET_HELP.action)
            })
    }

    fun logIAPCancelEvent() {
        logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            params = buildMap {
                put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_UNFULFILLED.action)
                put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_CLOSE.action)
            })
    }

    fun onRestorePurchaseCancel() {
        logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            params = buildMap {
                put(IAPAnalyticsKeys.ACTION.key, IAPAction.ACTION_CLOSE.action)
            })
    }

    fun loadIAPScreenEvent() {
        val event = IAPAnalyticsEvent.IAP_VALUE_PROP_VIEWED
        val params = buildMap {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            purchaseFlowData?.screenName?.takeIfNotEmpty()?.let { screenName ->
                put(IAPAnalyticsKeys.SCREEN_NAME.key, screenName)
            }
            putAll(getIAPEventParams())
        }
        analytics.logScreenEvent(screenName = event.eventName, params = params)
    }

    private fun getIAPEventParams(): Map<String, Any?> {
        if (purchaseFlowData.isNull() || purchaseFlowData?.courseId.isNullOrEmpty()) return emptyMap()

        return buildMap {
            purchaseFlowData?.apply {
                put(IAPAnalyticsKeys.COURSE_ID.key, courseId)
                put(
                    IAPAnalyticsKeys.PACING.key,
                    if (isSelfPaced.isTrue()) IAPAnalyticsKeys.SELF.key else IAPAnalyticsKeys.INSTRUCTOR.key
                )
                productInfo?.lmsUSDPrice?.nonZero()?.let { lmsUSDPrice ->
                    put(IAPAnalyticsKeys.LMS_USD_PRICE.key, lmsUSDPrice)
                }
                price.nonZero()?.let { localizedPrice ->
                    put(IAPAnalyticsKeys.LOCALIZED_PRICE.key, localizedPrice)
                }
                currencyCode.takeIfNotEmpty()?.let { currencyCode ->
                    put(IAPAnalyticsKeys.CURRENCY_CODE.key, currencyCode)
                }
                componentId?.takeIfNotEmpty()?.let { componentId ->
                    put(IAPAnalyticsKeys.COMPONENT_ID.key, componentId)
                }
                iapFlow?.let { iapFlow ->
                    put(IAPAnalyticsKeys.IAP_FLOW_TYPE.key, iapFlow.value)
                }
                put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
                screenName?.takeIfNotEmpty()?.let { screenName ->
                    put(IAPAnalyticsKeys.SCREEN_NAME.key, screenName)
                }
            }
        }
    }

    private fun getUnfulfilledIAPEventParams(): Map<String, Any?> {
        if (isSilentIAPFlow.isNull()) return emptyMap()

        return buildMap {
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
            purchaseFlowData?.screenName?.takeIfNotEmpty()?.let { screenName ->
                put(IAPAnalyticsKeys.SCREEN_NAME.key, screenName)
            }
            put(
                IAPAnalyticsKeys.IAP_FLOW_TYPE.key,
                if (isSilentIAPFlow.isTrue()) IAPFlow.SILENT.value else IAPFlow.RESTORE.value
            )
        }
    }

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: Map<String, Any?> = mutableMapOf(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(IAPAnalyticsKeys.NAME.key, event.biValue)
                putAll(params)
                putAll(getIAPEventParams())
                putAll(getUnfulfilledIAPEventParams())
            },
        )
    }
}
