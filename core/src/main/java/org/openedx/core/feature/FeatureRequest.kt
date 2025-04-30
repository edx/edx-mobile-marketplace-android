package org.openedx.core.feature

/**
 * Wrapper for passing a feature key to a FeatureService.
 * Allows future extension for user attributes or context.
 *
 * @property key The feature identifier to evaluate.
 */
@JvmInline
value class FeatureRequest(
    val key: String,
)
