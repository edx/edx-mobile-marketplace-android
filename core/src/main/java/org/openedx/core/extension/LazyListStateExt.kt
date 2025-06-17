package org.openedx.core.extension

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState


/**
 * Smoothly scrolls the [LazyListState] to the specified item index with animation.
 *
 * Animates the scroll to [index] using [animateScrollBy] with a custom duration and easing,
 * followed by [index] for precise alignment. Requires a uniform [itemHeightPx] and a coroutine scope.
 *
 * @param index The target item index (0-based, must be valid).
 * @param itemHeightPx The item height in pixels (must be positive and accurate).
 * @param durationMillis Animation duration in milliseconds (default: 500).
 * @param offset Optional final scroll offset in pixels (default: 0).
 */
suspend fun LazyListState.smoothScrollToIndex(
    index: Int,
    itemHeightPx: Float,
    durationMillis: Int = 500,
    offset: Int = 0
) {
    // Calculate target offset
    val targetOffset = index * itemHeightPx
    val currentOffset = firstVisibleItemScrollOffset + (firstVisibleItemIndex * itemHeightPx)
    val distance = targetOffset - currentOffset

    // Smoothly animate to the target offset
    animateScrollBy(
        value = distance,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        )
    )

    // Ensure the exact item is scrolled
    animateScrollToItem(
        index = index,
        scrollOffset = offset
    )
}
