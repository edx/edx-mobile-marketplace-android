package org.openedx.app.data.networking

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.openedx.core.data.model.ErrorResponse
import org.openedx.core.system.EdxError
import org.openedx.core.utils.Logger

class HandleErrorInterceptor(
    private val gson: Gson
) : Interceptor {
    private val logger = Logger(TAG)

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val responseCode = response.code
        if (responseCode in 400..500 && response.body != null) {
            val jsonStr = response.peekBody(Long.MAX_VALUE).string()

            try {
                val errorResponse = gson.fromJson(jsonStr, ErrorResponse::class.java)
                if (errorResponse?.error != null) {
                    when (errorResponse.error) {
                        ERROR_INVALID_GRANT -> {
                            throw EdxError.InvalidGrantException(
                                errorResponse.errorDescription ?: ""
                            )
                        }

                        ERROR_USER_NOT_ACTIVE -> {
                            throw EdxError.UserNotActiveException(
                                errorResponse.errorDescription ?: ""
                            )
                        }

                        else -> {
                            return response
                        }
                    }
                } else if (errorResponse?.errorDescription != null) {
                    throw EdxError.ValidationException(errorResponse.errorDescription ?: "")
                }
            } catch (e: JsonSyntaxException) {
                logger.e(throwable = e, metadata = mapOf("json" to jsonStr))
                return response
            }
        }

        return response
    }

    companion object {
        const val ERROR_INVALID_GRANT = "invalid_grant"
        const val ERROR_USER_NOT_ACTIVE = "user_not_active"
        const val TAG = "HandleErrorInterceptor"
    }
}
