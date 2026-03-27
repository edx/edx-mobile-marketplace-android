package org.openedx.app

import android.app.Application
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.ui.BrazeDeeplinkHandler
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.Rum
import com.datadog.android.rum.RumConfiguration
import com.google.firebase.FirebaseApp
import io.branch.referral.Branch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.openedx.app.deeplink.BranchBrazeDeeplinkHandler
import org.openedx.app.di.appModule
import org.openedx.app.di.networkingModule
import org.openedx.app.di.screenModule
import org.openedx.core.config.Config
import org.openedx.featuremanagement.di.FeatureModuleProvider
import org.openedx.notifications.di.NotificationsModuleProvider
import com.datadog.android.core.configuration.Configuration
import org.openedx.app.data.storage.PreferencesManager
import kotlin.getValue
class OpenEdXApp : Application() {

    private val config by inject<Config>()
    private val corePreferences by inject<PreferencesManager>()

    override fun onCreate() {
        super.onCreate()
        initializeKoinModules()
        val config: Config by inject()
        val corePreferences: PreferencesManager by inject()

        if (corePreferences.isDatadogEnabled) {
            initializeDatadog()
        }

        if (config.getFirebaseConfig().enabled) {
            FirebaseApp.initializeApp(this)
        }

        if (config.getBranchConfig().enabled) {
            if (BuildConfig.DEBUG) {
                Branch.enableTestMode()
                Branch.enableLogging()
            }
            Branch.expectDelayedSessionInitialization(true)
            Branch.getAutoInstance(this)
        }

        if (config.getBrazeConfig().isEnabled && config.getFirebaseConfig().enabled) {
            val isCloudMessagingEnabled = config.getFirebaseConfig().isCloudMessagingEnabled &&
                    config.getBrazeConfig().isPushNotificationsEnabled

            val brazeConfig = BrazeConfig.Builder()
                .setIsFirebaseCloudMessagingRegistrationEnabled(isCloudMessagingEnabled)
                .setFirebaseCloudMessagingSenderIdKey(config.getFirebaseConfig().projectNumber)
                .setHandlePushDeepLinksAutomatically(true)
                .setIsFirebaseMessagingServiceOnNewTokenRegistrationEnabled(true)
                .build()
            Braze.configure(this, brazeConfig)

            if (config.getBranchConfig().enabled) {
                BrazeDeeplinkHandler.setBrazeDeeplinkHandler(BranchBrazeDeeplinkHandler())
            }
        }
    }
    private fun initializeDatadog(){

        if (!corePreferences.isDatadogEnabled) {
            return
        }
        if (BuildConfig.DD_CLIENT_TOKEN.isNotEmpty() && BuildConfig.DD_APPLICATION_ID.isNotEmpty()
        ) {

            val configuration = Configuration.Builder(
                clientToken = BuildConfig.DD_CLIENT_TOKEN,
                env = BuildConfig.DD_ENV,
                variant = BuildConfig.BUILD_TYPE
            )
                .useSite(DatadogSite.US1)
                .build()

            Datadog.initialize(
                context = this,
                configuration = configuration,
                trackingConsent = TrackingConsent.GRANTED
            )

            val rumConfig = RumConfiguration.Builder(
                BuildConfig.DD_APPLICATION_ID
            )
                .trackUserInteractions()
                .trackLongTasks()
                .trackNonFatalAnrs(enabled = true)
                .build()

            Rum.enable(rumConfig)
        }
    }
    private fun initializeKoinModules() {
        startKoin {
            androidContext(this@OpenEdXApp)
            modules(
                appModule,
                networkingModule,
                screenModule
            )
        }

        val koinModules = listOfNotNull(
            NotificationsModuleProvider()
                .takeIf { config.isPushNotificationsEnabled() }
                ?.getModules(),
            FeatureModuleProvider()
                .takeIf { config.getOptimizelyConfig().enabled }
                ?.getModules()
        ).flatten()
        loadKoinModules(koinModules)
    }
}
