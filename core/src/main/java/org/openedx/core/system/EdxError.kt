package org.openedx.core.system

import java.io.IOException

sealed class EdxError(error: String) : IOException(error) {
    class InvalidGrantException(val error: String) : EdxError(error)
    class UserNotActiveException(val error: String) : EdxError(error)
    class ValidationException(val error: String) : EdxError(error)
    data class UnknownException(val error: String) : EdxError(error)
}
