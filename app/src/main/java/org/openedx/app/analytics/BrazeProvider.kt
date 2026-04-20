package org.openedx.app.analytics

import android.content.Context
import com.braze.Braze
import org.openedx.app.system.push.PushTokenRegistrar
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.utils.Logger

class BrazeProvider(
    private val context: Context,
    private val preferences: CorePreferences,
    private val pushTokenRegistrar: PushTokenRegistrar,
) : Analytics {

    private val logger = Logger(TAG)

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        // Braze does not require screen event tracking here
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        // Braze does not require generic event tracking here
    }

    override fun logUserId(userId: Long) {
        Braze.getInstance(context).changeUser(userId.toString())

        val pushToken = preferences.pushToken
        if (pushToken.isNotEmpty()) {
            // Re-register token after user change so token is associated with signed-in user.
            pushTokenRegistrar.register(pushToken)
        }

        logger.d { "Braze changeUser: $userId" }
    }

    private companion object {
        const val TAG = "BrazeAnalytics"
    }
}
