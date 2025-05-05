package org.openedx.course.extension

import androidx.media3.common.MediaItem

fun MediaItem.matches(other: MediaItem): Boolean {
    return this.localConfiguration?.uri == other.localConfiguration?.uri
}
