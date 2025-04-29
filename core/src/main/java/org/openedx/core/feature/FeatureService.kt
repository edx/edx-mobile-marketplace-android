package org.openedx.core.feature

/**
 * A vendor-agnostic interface for feature management.
 */
interface FeatureService {
    /**
     * Evaluates a feature decision based on the provided typed [FeatureRequest].
     */
    fun evaluate(request: FeatureRequest): FeatureDecision?
}
