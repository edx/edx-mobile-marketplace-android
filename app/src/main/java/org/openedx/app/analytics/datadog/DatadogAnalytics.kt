package org.openedx.app.analytics.datadog
import android.app.Application
import android.webkit.WebView
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumConfiguration
import com.datadog.android.webview.WebViewTracking
import com.datadog.android.core.configuration.Configuration
import androidx.core.net.toUri
import androidx.privacysandbox.tools.core.model.Type
import com.datadog.android.event.EventMapper
import com.datadog.android.rum.model.ResourceEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openedx.app.BuildConfig
import org.openedx.app.analytics.Analytics
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.WebViewTrackingAnalytics
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.DatadogTrackingToggledEvent
import org.openedx.core.utils.Logger

class DatadogAnalytics(
    context: Application,
    private val config: Config,
    private val corePreferences: CorePreferences,
    private val appNotifier: AppNotifier,
) : Analytics, WebViewTrackingAnalytics {

    private val logger = Logger(TAG)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        if (config.getDatadogConfig().enabled) {
            initializeDatadog(context)
        }
        observeDatadogToggle()
    }

    private fun initializeDatadog(context: Application) {
        val datadogConfig = config.getDatadogConfig()
        if (datadogConfig.clientToken.isNotEmpty() && datadogConfig.applicationId.isNotEmpty()) {
            val configuration = Configuration.Builder(
                clientToken = datadogConfig.clientToken,
                env = datadogConfig.environment,
                variant = BuildConfig.BUILD_TYPE
            )
                .useSite(DatadogSite.US1)
                .build()

            // Initialize with PENDING so no data is collected until we apply the user's preference.
            Datadog.initialize(
                context = context,
                configuration = configuration,
                trackingConsent = TrackingConsent.PENDING
            )

            val rumConfig = RumConfiguration.Builder(datadogConfig.applicationId)
                .trackUserInteractions()
                .trackLongTasks()
                .trackNonFatalAnrs(enabled = true)
                .setResourceEventMapper(object : EventMapper<ResourceEvent> {
                    override fun map(event: ResourceEvent): ResourceEvent? {
                        val url = event.resource.url
                        return if (url.contains("prod-discovery.edx-cdn.org")) {
                            null // Drop these
                        } else {
                            event // Keep others
                        }
                    }
                })
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
        if (!config.getDatadogConfig().enabled) return
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
        if (!config.getDatadogConfig().enabled || !corePreferences.isDatadogEnabled) return
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

    override fun enableWebViewTracking(webView: WebView, url: String) {
        if (!config.getDatadogConfig().enabled || !corePreferences.isDatadogEnabled) return

        val host = url.toUri().host ?: return
        try {
            WebViewTracking.enable(webView, listOf(host))
        } catch (e: Exception) {
            logger.e(throwable = e)
        }
    }

    private fun logDatadogEvent(eventName: String, attributes: Map<String, Any?> = emptyMap()) {
        if (!config.getDatadogConfig().enabled || !corePreferences.isDatadogEnabled) return
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