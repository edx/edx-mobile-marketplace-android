package org.openedx.core.feature

/**
 * Wrapper for passing a feature key to a [FeatureService].
 * Allows future extension for user attributes or context.
 *
 * @property key The feature identifier to evaluate.
 */
@JvmInline
value class FeatureRequest(
    val key: String,
)

/**
 * Centralized registry of all feature and experiment keys used in the app.
 * Using a value class ensures compile-time safety and IDE autocompletion.
 */
object FeatureRequests {
    val DemoFeature = FeatureRequest("demo_feature_key")
    val ValuePropCertificatePreview = FeatureRequest("value_prop_certificate_preview")
    val CertificatePreviewEnabled = FeatureRequest("certificate_preview_enabled")
}
