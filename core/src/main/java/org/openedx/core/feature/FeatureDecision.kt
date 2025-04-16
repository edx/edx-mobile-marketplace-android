package org.openedx.core.feature

/**
 * Represents the result of evaluating a feature decision.
 */
sealed class FeatureDecision<T> {
    abstract val key: String
    abstract val value: T?
    abstract val metadata: Map<String, Any>
}

/**
 * Decision for a feature flag.
 */
data class FeatureFlagDecision(
    override val key: String,
    private val enabled: Boolean,
    override val metadata: Map<String, Any> = emptyMap(),
) : FeatureDecision<Boolean>() {

    override val value: Boolean
        get() = enabled
}

/**
 * Decision for an A/B test.
 */
data class AbTestDecision(
    override val key: String,
    private val variationKey: String,
    override val metadata: Map<String, Any> = emptyMap(),
) : FeatureDecision<String>() {

    override val value: String
        get() = variationKey
}


/**
 * Decision for a remote config value.
 */
data class RemoteConfigDecision(
    override val key: String,
    val flagKey: String,
    private val data: String,
    override val metadata: Map<String, Any> = emptyMap(),
) : FeatureDecision<String>() {

    override val value: String
        get() = data
}
