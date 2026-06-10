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
        val firebaseEventName = AnalyticsUtils.makeFirebaseAnalyticsKey(eventName)
        tracker.logEvent(
            firebaseEventName,
            AnalyticsUtils.formatFirebaseAnalyticsData(params)
        )
        logger.d {
            "Firebase Analytics log Event original=$eventName, firebase=$firebaseEventName, " +
                "attempts_to_purchase=${params["attempts_to_purchase"] ?: "not_set"}, params=$params"
        }
    }

    override fun logUserId(userId: Long) {
        tracker.setUserId(userId.toString())
        logger.d { "Firebase Analytics User Id log Event" }
    }

    private companion object {
        const val TAG = "FirebaseAnalytics"
    }
}
