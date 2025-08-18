package org.openedx.core

import java.util.Locale

object AppDataConstants {
    const val USER_MIN_YEAR = 13
    const val USER_MAX_YEAR = 77
    const val DEFAULT_MIME_TYPE = "image/jpeg"
    val defaultLocale: Locale = Locale.Builder().setLanguage("en").build()

    const val VIDEO_FORMAT_M3U8 = ".m3u8"
    const val VIDEO_FORMAT_MP4 = ".mp4"
    const val VIDEO_NORMAL_SPEED = 1f
    const val VIDEO_DOUBLE_SPEED = 2f

    // Equal 1GB
    const val DOWNLOADS_CONFIRMATION_SIZE = 1024 * 1024 * 1024L

    // Initial retry count that controls whether the enrollment mode transition is attempted
    // immediately or after the base delay, depending on the current count.
    const val ENROLLMENT_MODE_RETRY_INITIAL_COUNT = 1

    // Max retry attempts allowed for checking enrollment mode transition after purchase.
    const val ENROLLMENT_MODE_RETRY_THRESHOLD = 3

    // Base delay in milliseconds for each enrollment mode retry attempt.
    const val ENROLLMENT_MODE_RETRY_BASE_DELAY_MS = 2500L
}
