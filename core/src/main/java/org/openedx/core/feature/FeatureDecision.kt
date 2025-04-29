package org.openedx.core.feature

/**
 * Represents the result of evaluating a feature toggle, experiment, or remote
 * configuration value.
 *
 * @property key       The unique identifier of the feature or experiment.
 * @property isEnabled True if the feature is enabled; false otherwise.
 * @property variation The variation key for A/B tests, or null for simple flags.
 * @property metadata  Any additional data returned by the SDK (e.g., variables).
 */
data class FeatureDecision(
    val key: String,
    val isEnabled: Boolean,
    val variation: String?,
    val metadata: Map<String, Any>,
) {
    fun getString(key: String, default: String = ""): String {
        return metadata[key] as? String ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return metadata[key] as? Int ?: default
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return metadata[key] as? Boolean ?: default
    }

    fun getDouble(key: String, default: Double = 0.0): Double {
        return metadata[key] as? Double ?: default
    }
}
