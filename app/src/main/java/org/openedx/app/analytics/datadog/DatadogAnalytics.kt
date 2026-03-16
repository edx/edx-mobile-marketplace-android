package org.openedx.app.analytics.datadog

import com.datadog.android.Datadog
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumActionType
import org.openedx.app.analytics.Analytics

class DatadogAnalytics : Analytics {

    override fun logEvent(event: String, params: Map<String, Any?>) {
        logDatadogEvent(event, params)
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logDatadogEvent(screenName, params)
    }

    override fun logUserId(userId: Long) {
        try {
            Datadog.setUserInfo(
                userId.toString(),
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logDatadogEvent(eventName: String, attributes: Map<String, Any?> = emptyMap()) {
        try {
            GlobalRumMonitor.get().addAction(
                RumActionType.CUSTOM,
                eventName,
                attributes.filterValues { it != null }.mapValues { it.value as Any }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}