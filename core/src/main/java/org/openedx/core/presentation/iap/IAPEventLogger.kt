package org.openedx.core.presentation.iap

import com.android.billingclient.api.BillingClient
import org.openedx.core.data.storage.IAPPreferences
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
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
    var isSilentIAPFlow: Boolean? = null,
    var purchaseFlowData: PurchaseFlowData? = null,
    private val iapPreferences: IAPPreferences? = null
) {
    fun upgradeNowClickedEvent(showCertificatePreview: Boolean = false) {
        val courseId = purchaseFlowData?.courseId
        val attemptsToPurchase = incrementAndGetAttempts(courseId)
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = attemptsToPurchase
        if (showCertificatePreview) {
            params[IAPAnalyticsKeys.SHOW_CERTIFICATE_PREVIEW.key] = "true"
        }
        params[IAPAnalyticsKeys.COURSE_ID.key] = courseId
        logIAPEvent(IAPAnalyticsEvent.IAP_UPGRADE_NOW_CLICKED, params)
    }

    fun upgradeSuccessEvent(clearAttempts: Boolean = true) {
        val elapsedTime = TimeUtils.getCurrentTime() - (purchaseFlowData?.flowStartTime ?: 0L)
        val courseId = purchaseFlowData?.courseId
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ELAPSED_TIME.key] = elapsedTime
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = getAttempts(courseId)
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_SUCCESS, params)
        if (clearAttempts) {
            clearAttempts(courseId)
        }
    }

    private fun purchaseErrorEvent(error: String) {
        val courseId = purchaseFlowData?.courseId
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR.key] = error
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = getAttempts(courseId)
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_ERROR, params)
    }

    private fun canceledByUserEvent() {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = getAttempts(purchaseFlowData?.courseId)
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_CANCELED, params)
    }

    private fun courseUpgradeErrorEvent(error: String) {
        val courseId = purchaseFlowData?.courseId
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR.key] = error
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = getAttempts(courseId)
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_ERROR, params)
    }

    private fun priceLoadErrorEvent(error: String) {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR.key] = error
        logIAPEvent(IAPAnalyticsEvent.IAP_PRICE_LOAD_ERROR, params)
    }

    fun logExceptionEvent(iapException: IAPException) {
        val feedbackErrorMessage = iapException.getFormattedErrorMessage()
        when (iapException.requestType) {
            IAPRequestType.PAYMENT_SDK_CODE -> {
                if (BillingClient.BillingResponseCode.USER_CANCELED == iapException.httpErrorCode) {
                    canceledByUserEvent()
                } else {
                    purchaseErrorEvent(feedbackErrorMessage)
                }
            }

            IAPRequestType.PRICE_CODE,
            IAPRequestType.NO_SKU_CODE -> {
                priceLoadErrorEvent(feedbackErrorMessage)
            }

            else -> {
                courseUpgradeErrorEvent(feedbackErrorMessage)
            }
        }
    }

    fun logIAPErrorActionEvent(alertType: String, action: String) {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR_ALERT_TYPE.key] = alertType
        params[IAPAnalyticsKeys.ERROR_ACTION.key] = action
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, params)
    }

    fun logRestorePurchasesClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_RESTORE_PURCHASE_CLICKED)
    }

    fun logUnfulfilledPurchaseInitiatedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UNFULFILLED_PURCHASE_INITIATED)
    }

    fun logGetHelpEvent() {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR_ALERT_TYPE.key] = IAPAction.ACTION_UNFULFILLED.action
        params[IAPAnalyticsKeys.ERROR_ACTION.key] = IAPAction.ACTION_GET_HELP.action
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, params)
    }

    fun logIAPCancelEvent() {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ERROR_ALERT_TYPE.key] = IAPAction.ACTION_UNFULFILLED.action
        params[IAPAnalyticsKeys.ERROR_ACTION.key] = IAPAction.ACTION_CLOSE.action
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, params)
    }

    fun onRestorePurchaseCancel() {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.ACTION.key] = IAPAction.ACTION_CLOSE.action
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, params)
    }

    fun loadIAPScreenEvent() {
        val event = if (purchaseFlowData?.screenName == IAPFlowSource.TRACK_SELECTION.screen) {
            IAPAnalyticsEvent.IAP_TRACK_SELECTION_VIEWED
        } else {
            IAPAnalyticsEvent.IAP_VALUE_PROP_VIEWED
        }
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.NAME.key] = event.biValue
        params.putAll(getIAPEventParams())
        analytics.logScreenEvent(screenName = event.eventName, params = params)
    }

    fun logContinueToFreeTrackClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_CONTINUE_WITH_FREE_TRACK_CLICKED)
    }

    fun onCertificatePreviewShown(courseId: String?) {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.SHOW_CERTIFICATE_PREVIEW.key] = "true"
        params[IAPAnalyticsKeys.COURSE_ID.key] = courseId
        logIAPEvent(IAPAnalyticsEvent.IAP_CERT_PREVIEW_SHOWN, params)
    }

    fun onCertificatePreviewPurchased(courseId: String?, price: Double) {
        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.SHOW_CERTIFICATE_PREVIEW.key] = "true"
        params[IAPAnalyticsKeys.ATTEMPTS_TO_PURCHASE.key] = getAttempts(courseId)
        params[IAPAnalyticsKeys.COURSE_ID.key] = courseId
        params[IAPAnalyticsKeys.LMS_USD_PRICE.key] = price
        logIAPEvent(IAPAnalyticsEvent.IAP_CERT_PREVIEW_PURCHASED, params)
        clearAttempts(courseId)
    }

    private fun incrementAndGetAttempts(courseId: String?): Int {
        if (courseId.isNullOrEmpty()) return 0
        return iapPreferences?.incrementPreviewCount(courseId) ?: 0
    }

    private fun getAttempts(courseId: String?): Int {
        if (courseId.isNullOrEmpty()) return 0
        return iapPreferences?.getPreviewCount(courseId) ?: 0
    }

    private fun clearAttempts(courseId: String?) {
        if (courseId.isNullOrEmpty()) return
        iapPreferences?.clearPreviewCount(courseId)
    }

    private fun getIAPEventParams(): Map<String, Any?> {
        if (purchaseFlowData.isNull() || purchaseFlowData?.courseId.isNullOrEmpty()) {
            return emptyMap()
        }

        val params = mutableMapOf<String, Any?>()
        purchaseFlowData?.apply {
            params[IAPAnalyticsKeys.COURSE_ID.key] = courseId
            params[IAPAnalyticsKeys.PACING.key] =
                if (isSelfPaced.isTrue()) IAPAnalyticsKeys.SELF.key else IAPAnalyticsKeys.INSTRUCTOR.key
            productInfo?.lmsUSDPrice?.nonZero()?.let { lmsUSDPrice ->
                params[IAPAnalyticsKeys.LMS_USD_PRICE.key] = lmsUSDPrice
            }
            price.nonZero()?.let { localizedPrice ->
                params[IAPAnalyticsKeys.LOCALIZED_PRICE.key] = localizedPrice
            }
            currencyCode.takeIfNotEmpty()?.let { code ->
                params[IAPAnalyticsKeys.CURRENCY_CODE.key] = code
            }
            componentId?.takeIfNotEmpty()?.let { component ->
                params[IAPAnalyticsKeys.COMPONENT_ID.key] = component
            }
            iapFlow?.let { flow ->
                params[IAPAnalyticsKeys.IAP_FLOW_TYPE.key] = flow.value
            }
            params[IAPAnalyticsKeys.CATEGORY.key] = IAPAnalyticsKeys.IN_APP_PURCHASES.key
            screenName?.takeIfNotEmpty()?.let { screen ->
                params[IAPAnalyticsKeys.SCREEN_NAME.key] = screen
            }
        }
        return params
    }

    private fun getUnfulfilledIAPEventParams(): Map<String, Any?> {
        if (isSilentIAPFlow.isNull()) {
            return emptyMap()
        }

        val params = mutableMapOf<String, Any?>()
        params[IAPAnalyticsKeys.CATEGORY.key] = IAPAnalyticsKeys.IN_APP_PURCHASES.key
        purchaseFlowData?.screenName?.takeIfNotEmpty()?.let { screen ->
            params[IAPAnalyticsKeys.SCREEN_NAME.key] = screen
        }
        params[IAPAnalyticsKeys.IAP_FLOW_TYPE.key] =
            if (isSilentIAPFlow.isTrue()) IAPFlow.SILENT.value else IAPFlow.RESTORE.value
        return params
    }

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: Map<String, Any?> = emptyMap()
    ) {
        val mergedParams = mutableMapOf<String, Any?>()
        mergedParams[IAPAnalyticsKeys.NAME.key] = event.biValue
        mergedParams.putAll(params)
        mergedParams.putAll(getIAPEventParams())
        mergedParams.putAll(getUnfulfilledIAPEventParams())

        analytics.logEvent(event.eventName, mergedParams)
    }
}
