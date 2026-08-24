package org.openedx.core.exception


fun Float.safeDivBy(divisor: Float): Float = try {
    var result = this / divisor
    if (result.isNaN()) {
        result = 0f
    }
    result
} catch (_: ArithmeticException) {
    0f
}
