package org.openedx.core.feature

@JvmInline
value class FeatureKey(val key: String)

/**
 * Centralized registry of all feature and experiment keys used in the app.
 * Using a value class ensures compile-time safety and IDE autocompletion.
 */
object FeatureKeys {
    val DemoFeature = FeatureKey("demo_feature_key")
}
