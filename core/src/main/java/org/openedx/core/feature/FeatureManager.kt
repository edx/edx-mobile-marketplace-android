package org.openedx.core.feature

/**
 * High-level API for consumers to request feature decisions.
 * Internally it delegates to one or more FeatureService implementations.
 */
interface FeatureManager {
    /**
     * Synchronously returns a feature decision or null if unavailable.
     *
     * @param featureKey The feature identifier (use FeatureKeys).
     * @return The feature decision, or null if not available.
     */
    fun getDecision(featureKey: FeatureKey): FeatureDecision?
}
