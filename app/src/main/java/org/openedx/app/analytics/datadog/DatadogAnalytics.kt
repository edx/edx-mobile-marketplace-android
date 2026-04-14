package org.openedx.app.analytics.datadog

import com.datadog.android.Datadog
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumActionType
import org.openedx.app.analytics.Analytics
import org.openedx.core.DatadogConsentManager
import org.openedx.core.utils.Logger

class DatadogAnalytics : Analytics , DatadogConsentManager {

    private val logger = Logger(TAG)

    override fun logEvent(event: String, params: Map<String, Any?>) {
        logDatadogEvent(event, params)
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logDatadogEvent(screenName, params)
    }

    override fun logUserId(userId: Long) {
        try {
            if (!Datadog.isInitialized()) return

            Datadog.setUserInfo(
                userId.toString(),
                null,
                null
            )
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }

    override fun setTrackingConsent(enabled: Boolean) {
        try {
            Datadog.setTrackingConsent(
                if (enabled) TrackingConsent.GRANTED
                else TrackingConsent.NOT_GRANTED
            )
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }

    private fun logDatadogEvent(
        eventName: String,
        attributes: Map<String, Any?> = emptyMap()
    ) {
        try {
            if (!Datadog.isInitialized()) return

            val rum = GlobalRumMonitor.get()

            rum.addAction(
                RumActionType.CUSTOM,
                eventName,
                attributes
            )
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }
    companion object {
        private const val TAG = "DatadogAnalytics"
    }
}