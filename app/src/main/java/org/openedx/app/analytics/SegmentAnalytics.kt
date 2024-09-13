package org.openedx.app.analytics

import android.content.Context
import com.segment.analytics.kotlin.core.BaseEvent
import com.segment.analytics.kotlin.core.ScreenEvent
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.EventPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.destinations.braze.BrazeDestination
import com.segment.analytics.kotlin.destinations.firebase.FirebaseDestination
import org.openedx.app.BuildConfig
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger
import com.segment.analytics.kotlin.android.Analytics as SegmentAnalyticsBuilder
import com.segment.analytics.kotlin.core.Analytics as SegmentTracker

class SegmentAnalytics(context: Context, config: Config) : Analytics {

    private val logger = Logger(TAG)

    // Create an analytics client with the given application context and Segment write key.
    private val tracker: SegmentTracker =
        SegmentAnalyticsBuilder(config.getSegmentConfig().segmentWriteKey, context) {
            // Automatically track Lifecycle events
            trackApplicationLifecycleEvents = true
            flushAt = 20
            flushInterval = 30
        }

    init {
        if (config.getFirebaseConfig().isSegmentAnalyticsSource()) {
            tracker.add(plugin = FirebaseDestination(context = context))

            // Override the default event plugin to format the event and properties
            // according to Firebase Analytics guidelines
            tracker.find(FirebaseDestination::class)?.add(object : EventPlugin {
                override lateinit var analytics: SegmentTracker
                override val type = Plugin.Type.Before

                override fun track(payload: TrackEvent): BaseEvent {
                    return payload.apply {
                        this.event = AnalyticsUtils.makeFirebaseAnalyticsKey(this.event)
                        properties =
                            AnalyticsUtils.formatFirebaseAnalyticsDataForSegment(properties)
                    }
                }

                override fun screen(payload: ScreenEvent): BaseEvent {
                    return payload.apply {
                        name = AnalyticsUtils.makeFirebaseAnalyticsKey(name)
                    }
                }
            })
        }

        if (config.getFirebaseConfig()
                .isSegmentAnalyticsSource() && config.getBrazeConfig().isEnabled
        ) {
            tracker.add(plugin = BrazeDestination(context))
        }
        SegmentTracker.debugLogsEnabled = BuildConfig.DEBUG
        logger.d { "Segment Analytics Builder Initialised" }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Screen Event: $screenName + $params" }
        tracker.screen(screenName, params)
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logger.d { "Segment Analytics log Event $eventName: $params" }
        tracker.track(eventName, params)
    }

    override fun logUserId(userId: Long) {
        logger.d { "Segment Analytics User Id log Event: $userId" }
        tracker.identify(userId.toString())
    }

    private companion object {
        const val TAG = "SegmentAnalytics"
    }
}
