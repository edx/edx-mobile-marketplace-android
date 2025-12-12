package org.openedx.featuremanagement

import android.content.Context
import com.optimizely.ab.android.sdk.OptimizelyClient
import com.optimizely.ab.android.sdk.OptimizelyManager
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.extension.isNotNull
import org.openedx.core.feature.FeatureDecision
import org.openedx.core.feature.FeatureRequest
import org.openedx.core.feature.FeatureService
import java.util.concurrent.TimeUnit

/**
 * An Optimizely-based implementation of [FeatureService].
 */
internal class OptimizelyFeatureService(
    context: Context,
    config: Config,
    private val preferences: CorePreferences,
) : FeatureService {

    private val optimizely: OptimizelyClient

    init {
        val manager = OptimizelyManager.builder()
           // .withSDKKey(config.getOptimizelyConfig().sdkKey)
            .withDatafileDownloadInterval(15, TimeUnit.MINUTES)
            .withEventDispatchInterval(15, TimeUnit.MINUTES)
            .build(context)
        manager.initialize(context, null)
        optimizely = manager.optimizely
    }

    override fun evaluate(request: FeatureRequest): FeatureDecision? {
        val userId = preferences.user?.id?.toString() ?: return null
        val userContext = optimizely.createUserContext(userId)
        val decision = userContext?.decide(request.key) ?: return null
        val safeMetadata = decision.variables
            .toMap()
            ?.filterValues { it.isNotNull() }
            ?: emptyMap()

        return FeatureDecision(
            key = request.key,
            isEnabled = decision.enabled,
            variation = decision.variationKey,
            metadata = safeMetadata
        )
    }
}
