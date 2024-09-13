package org.openedx.app.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import org.openedx.core.utils.Logger

class FirebaseAnalytics(context: Context) : Analytics {

    private val logger = Logger(TAG)
    private val tracker: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        tracker.logEvent(
            AnalyticsUtils.makeFirebaseAnalyticsKey(screenName),
            AnalyticsUtils.formatFirebaseAnalyticsData(params)
        )
        logger.d { "Firebase Analytics log Screen Event: $screenName + $params" }
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        tracker.logEvent(
            AnalyticsUtils.makeFirebaseAnalyticsKey(eventName),
            AnalyticsUtils.formatFirebaseAnalyticsData(params)
        )
        logger.d { "Firebase Analytics log Event $eventName: $params" }
    }

    override fun logUserId(userId: Long) {
        tracker.setUserId(userId.toString())
        logger.d { "Firebase Analytics User Id log Event" }
    }

    private companion object {
        const val TAG = "FirebaseAnalytics"
    }
}
