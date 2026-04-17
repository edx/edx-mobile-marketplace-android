package org.openedx.app.analytics

import android.content.Context
import com.braze.Braze
import org.openedx.core.utils.Logger

class BrazeProvider(private val context: Context) : Analytics {

    private val logger = Logger(TAG)

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        // Braze does not require screen event tracking here
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        // Braze does not require generic event tracking here
    }

    override fun logUserId(userId: Long) {
        Braze.getInstance(context).changeUser(userId.toString())
        logger.d { "Braze changeUser: $userId" }
    }

    private companion object {
        const val TAG = "BrazeAnalytics"
    }
}
