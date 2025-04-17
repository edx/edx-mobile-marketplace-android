package org.openedx.core.feature

interface FeatureManager {
    /**
     * Evaluates a feature decision based on the provided typed [FeatureRequest].
     *
     * @param request The feature request to evaluate.
     * @return The feature decision, or null if not available.
     */
    fun <T> getDecision(request: FeatureRequest<T>): FeatureDecision<T>?
}
