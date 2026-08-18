package org.openedx.core.data.storage

/**
 * Persistence contract for IAP-specific counters and state.
 * Keeps preview-counter storage out of the presentation layer.
 */
interface IAPPreferences {
    /**
     * Increments the certificate-preview exposure count for [courseId] and
     * returns the new value.
     */
    fun incrementPreviewCount(courseId: String): Int

    /** Returns the current certificate-preview exposure count for [courseId]. */
    fun getPreviewCount(courseId: String): Int

    /** Clears the certificate-preview exposure count for [courseId]. */
    fun clearPreviewCount(courseId: String)
}

