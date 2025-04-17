package org.openedx.core.feature

/**
 * A vendor-agnostic interface for feature management.
 */
interface FeatureManagementService {
    /**
     * Evaluates a feature decision based on the provided typed [FeatureRequest].
     */
    fun <T> getDecision(request: FeatureRequest<T>): FeatureDecision<T>?
}
