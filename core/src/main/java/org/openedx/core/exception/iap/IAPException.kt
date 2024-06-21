package org.openedx.core.exception.iap

import org.json.JSONObject
import retrofit2.Response

/**
 *
 * Signals that the user unable to complete the in-app purchases follow it being not parsable or
 * incomplete according to what we expect.
 *
 * @param httpErrorCode stores the error codes can be either [BillingClient][com.android.billingclient.api.BillingClient]
 *                      OR http error codes for ecommerce end-points, and setting it up to `-1`
 *                      cause some at some service return error code `0`.
 * @param errorMessage stores the error messages received from BillingClient & ecommerce end-points.
 * */
class IAPException(val httpErrorCode: Int = -1, val errorMessage: String) :
    Exception(errorMessage)

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
