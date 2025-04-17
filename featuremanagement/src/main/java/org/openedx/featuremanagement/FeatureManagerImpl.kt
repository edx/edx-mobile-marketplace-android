package org.openedx.featuremanagement

import org.openedx.core.feature.FeatureDecision
import org.openedx.core.feature.FeatureManagementService
import org.openedx.core.feature.FeatureManager
import org.openedx.core.feature.FeatureRequest

internal class FeatureManagerImpl(
    private val services: List<FeatureManagementService>
) : FeatureManager {

    override fun <T> getDecision(request: FeatureRequest<T>): FeatureDecision<T>? {
        return services.asSequence()
            .mapNotNull { it.getDecision(request) }
            .firstOrNull()
    }
}
