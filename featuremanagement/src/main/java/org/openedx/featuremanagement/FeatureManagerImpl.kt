package org.openedx.featuremanagement

import org.openedx.core.feature.FeatureDecision
import org.openedx.core.feature.FeatureManager
import org.openedx.core.feature.FeatureRequest
import org.openedx.core.feature.FeatureService

internal class FeatureManagerImpl(
    private val services: List<FeatureService>
) : FeatureManager {

    /**
     * Aggregates multiple FeatureService instances, returning the first available decision.
     */
    override fun getDecision(featureRequest: FeatureRequest): FeatureDecision? {
        return services.asSequence()
            .mapNotNull { it.evaluate(featureRequest) }
            .firstOrNull()
    }
}
