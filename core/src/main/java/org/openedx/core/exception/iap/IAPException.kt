package org.openedx.core.exception.iap

import android.text.TextUtils
import org.json.JSONObject
import org.openedx.core.presentation.iap.IAPRequestType
import retrofit2.Response
import java.util.Locale

/**
 *
 * Signals that the user unable to complete the in-app purchases follow it being not parsable or
 * incomplete according to what we expect.
 *
 * @param requestType stores the request type for exception occurs.
 * @param httpErrorCode stores the error codes can be either [BillingClient][com.android.billingclient.api.BillingClient]
 *                      OR http error codes for ecommerce end-points, and setting it up to `-1`
 *                      cause some at some service return error code `0`.
 * @param errorMessage stores the error messages received from BillingClient & ecommerce end-points.
 * */
class IAPException(
    val requestType: IAPRequestType = IAPRequestType.UNKNOWN,
    val httpErrorCode: Int = -1,
    val errorMessage: String
) : Exception(errorMessage) {

    /**
     * Returns a StringBuilder containing the formatted error message.
     * i.e Error: error_endpoint-error_code-error_message
     *
     * @return Formatted error message.
     */
    fun getFormattedErrorMessage(): String {
        val body = StringBuilder()
        if (requestType == IAPRequestType.UNKNOWN) {
            return body.toString()
        }
        body.append(String.format("%s", requestType.request))
        // change the default value to -1 cuz in case of BillingClient return errorCode 0 for price load.
        if (httpErrorCode == -1) {
            return body.toString()
        }
        body.append(String.format(Locale.ENGLISH, "-%d", httpErrorCode))
        if (!TextUtils.isEmpty(errorMessage)) body.append(String.format("-%s", errorMessage))
        return body.toString()
    }
}

/**
 * Attempts to extract error message from api responses and fails gracefully if unable to do so.
 *
 * @return extracted text message; null if no message was received or was unable to parse it.
 */
fun <T> Response<T>.getMessage(): String {
    if (isSuccessful) return message()
    return try {
        val errors = JSONObject(errorBody()?.string() ?: "{}")
        errors.optString("error")
    } catch (ex: Exception) {
        ""
    }
}
