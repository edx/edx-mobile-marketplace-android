package org.openedx.core.feature

/**
 * Centralized registry of all feature and experiment keys used in the app.
 * Using a value class ensures compile-time safety and IDE autocompletion.
 */
object FeatureKeys {
    val DemoFeature = FeatureRequest("demo_feature_key")
}
