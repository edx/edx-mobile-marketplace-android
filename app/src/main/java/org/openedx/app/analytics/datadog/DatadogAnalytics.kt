package org.openedx.app.analytics.datadog

import android.app.Application
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.core.configuration.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openedx.app.BuildConfig
import org.openedx.app.analytics.Analytics
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.DatadogTrackingToggledEvent
import org.openedx.core.utils.Logger

class DatadogAnalytics(
    context: Application,
    private val corePreferences: CorePreferences,
    private val appNotifier: AppNotifier,
) : Analytics {

    private val logger = Logger(TAG)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        initializeDatadog(context)
        observeDatadogToggle()
    }

    private fun initializeDatadog(context: Application) {
        if (BuildConfig.DD_ENABLED
            && BuildConfig.DD_CLIENT_TOKEN.isNotEmpty()
            && BuildConfig.DD_APPLICATION_ID.isNotEmpty()) {
            val configuration = Configuration.Builder(
                clientToken = BuildConfig.DD_CLIENT_TOKEN,
                env = BuildConfig.DD_ENV,
                variant = BuildConfig.BUILD_TYPE
            )
                .useSite(DatadogSite.US1)
                .build()

            // Initialize with PENDING so no data is collected until we apply the user's preference.
            Datadog.initialize(
                context = context,
                configuration = configuration,
                trackingConsent = TrackingConsent.GRANTED
            )

            val rumConfig = RumConfiguration.Builder(BuildConfig.DD_APPLICATION_ID)
                .trackUserInteractions()
                .trackLongTasks()
                .trackNonFatalAnrs(enabled = true)
                .build()

            Rum.enable(rumConfig)
            logger.d { "Datadog SDK initialized with PENDING consent" }

            // Apply stored preference — defaults to true (ON) for fresh installs.
            val consent = if (corePreferences.isDatadogEnabled) {
                TrackingConsent.GRANTED
            } else {
                TrackingConsent.NOT_GRANTED
            }
            Datadog.setTrackingConsent(consent)
            logger.d { "Datadog consent set to: $consent" }
        }
    }

    private fun observeDatadogToggle() {
        if (!BuildConfig.DD_ENABLED) return
        appScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is DatadogTrackingToggledEvent) {
                    logger.d { "Datadog toggle event received: enabled=${event.enabled}" }
                    val consent = if (event.enabled) {
                        TrackingConsent.GRANTED
                    } else {
                        TrackingConsent.NOT_GRANTED
                    }
                    try {
                        Datadog.setTrackingConsent(consent)
                        logger.d { "Datadog consent updated to: $consent" }
                    } catch (e: Exception) {
                        logger.e(throwable = e)
                    }
                }
            }
        }
    }

    override fun logEvent(eventName: String, params: Map<String, Any?>) {
        logDatadogEvent(eventName, params)
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        logDatadogEvent(screenName, params)
    }

    override fun logUserId(userId: Long) {
        if (!BuildConfig.DD_ENABLED || !corePreferences.isDatadogEnabled) return
        try {
            Datadog.setUserInfo(
                userId.toString(),
                null,
                null
            )
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }

    private fun logDatadogEvent(eventName: String, attributes: Map<String, Any?> = emptyMap()) {
        if (!BuildConfig.DD_ENABLED || !corePreferences.isDatadogEnabled) return
        try {
            GlobalRumMonitor.get().addAction(
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