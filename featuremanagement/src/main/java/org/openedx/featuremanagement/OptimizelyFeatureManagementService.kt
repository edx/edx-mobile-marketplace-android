@file:Suppress("UNCHECKED_CAST")

package org.openedx.featuremanagement

import android.content.Context
import com.optimizely.ab.OptimizelyUserContext
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.feature.AbTestDecision
import org.openedx.core.feature.AbTestRequest
import org.openedx.core.feature.FeatureDecision
import org.openedx.core.feature.FeatureFlagDecision
import org.openedx.core.feature.FeatureFlagRequest
import org.openedx.core.feature.FeatureManagementService
import org.openedx.core.feature.FeatureRequest
import org.openedx.core.feature.RemoteConfigDecision
import org.openedx.core.feature.RemoteConfigRequest
import java.util.concurrent.TimeUnit

/**
 * An Optimizely-based implementation of [FeatureManagementService].
 *
 * This class handles SDK initialization, user identification, and evaluation of decisions for
 * feature flags, A/B tests, and remote config values—all determined by a typed request object.
 */
internal class OptimizelyFeatureManagementService(
    private val context: Context,
    private val config: Config,
    private val preferences: CorePreferences,
) : FeatureManagementService {

    private lateinit var optimizely: OptimizelyClient

    init {
        initialize()
    }

    private fun initialize() {
        val optimizelyManager = OptimizelyManager.builder()
            .withSDKKey(config.getOptimizelyConfig().sdkKey)
            .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
            .withEventDispatchInterval(15L, TimeUnit.MINUTES)
            .build(context)
        optimizelyManager.initialize(context, null)
        optimizely = optimizelyManager.optimizely
    }

    override fun <T> getDecision(request: FeatureRequest<T>): FeatureDecision<T>? {
        val userId = preferences.user?.id?.toString() ?: return null
        val userContext = optimizely.createUserContext(userId) ?: return null

        return when (request) {
            is FeatureFlagRequest -> {
                handleFeatureFlagRequest(
                    userContext,
                    request
                ) as FeatureDecision<T>
            }

            is AbTestRequest -> {
                handleAbTestRequest(
                    userContext,
                    request
                ) as FeatureDecision<T>
            }

            is RemoteConfigRequest -> {
                handleRemoteConfigRequest(
                    userContext,
                    request
                ) as FeatureDecision<T>
            }
        }
    }

    private fun handleFeatureFlagRequest(
        userContext: OptimizelyUserContext,
        request: FeatureFlagRequest,
    ): FeatureFlagDecision {
        val decision = userContext.decide(request.key)
        return FeatureFlagDecision(
            key = request.key,
            enabled = decision.enabled
        )
    }

    private fun handleAbTestRequest(
        userContext: OptimizelyUserContext,
        request: AbTestRequest
    ): AbTestDecision {
        val decision = userContext.decide(request.key)
        return AbTestDecision(
            key = request.key,
            variationKey = decision.variationKey ?: "",
        )
    }

    private fun handleRemoteConfigRequest(
        userContext: OptimizelyUserContext,
        request: RemoteConfigRequest
    ): RemoteConfigDecision {
        val decision = userContext.decide(request.key)
        val configValue = decision.variables.getValue(request.flagKey, String::class.java) ?: ""
        return RemoteConfigDecision(
            key = request.key,
            flagKey = request.flagKey,
            data = configValue
        )
    }
}
