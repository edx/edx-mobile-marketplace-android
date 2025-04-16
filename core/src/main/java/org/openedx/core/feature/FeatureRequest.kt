package org.openedx.core.feature

/**
 * Base sealed class representing a request for a feature decision.
 */
sealed class FeatureRequest<T> {
    abstract val key: String
}

/**
 * Request for a Boolean feature flag.
 */
data class FeatureFlagRequest(
    override val key: String,
) : FeatureRequest<Boolean>()

/**
 * Request for an A/B test decision.
 */
data class AbTestRequest(
    override val key: String,
) : FeatureRequest<String>()

/**
 * Request for a remote config value.
 *
 * @param key The primary key representing the remote config feature.
 * @param flagKey The secondary key representing the flag variant.
 */
data class RemoteConfigRequest(
    override val key: String,
    val flagKey: String,
) : FeatureRequest<String>()
