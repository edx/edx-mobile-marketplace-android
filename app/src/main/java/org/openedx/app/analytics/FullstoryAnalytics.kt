package org.openedx.app.analytics

import com.fullstory.FS
import com.fullstory.FSSessionData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.openedx.core.utils.Logger

class FullstoryAnalytics(isFirebaseEnabled: Boolean = false) : Analytics {

    private val logger = Logger(TAG)

    init {
        FS.setReadyListener { sessionData: FSSessionData ->
            val sessionUrl = sessionData.currentSessionURL
            logger.d { "FullStory Session URL is: $sessionUrl" }
            if (isFirebaseEnabled) {
                val instance = FirebaseCrashlytics.getInstance()
                instance.setCustomKey("fullstory_session_url", sessionUrl)
            }
        }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logger.d { "Page : $screenName $params" }
        FS.page(screenName, params).start()
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logger.d { "Event: $eventName $params" }
        FS.page(eventName, params).start()
    }

    override fun logUserId(userId: Long) {
        logger.d { "Identify: $userId" }
        FS.identify(
            userId.toString(), mapOf(
                DISPLAY_NAME to userId
            )
        )
    }

    private companion object {
        const val TAG = "FullstoryAnalytics"
        private const val DISPLAY_NAME = "displayName"
    }
}
