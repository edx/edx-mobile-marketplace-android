package org.openedx.app.analytics

import android.os.Bundle
import com.segment.analytics.kotlin.core.Properties
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AnalyticsUtils {
    fun makeFirebaseAnalyticsKey(value: String): String {
        return value.replace(Regex("[:\\- ]+"), "_").take(40)
    }

    fun formatFirebaseAnalyticsData(input: Map<String, Any?>): Bundle {
        val bundle = Bundle()
        input.forEach { (key, value) ->
            val formattedKey = makeFirebaseAnalyticsKey(key)
            when (value) {
                null -> Unit
                is String -> bundle.putString(formattedKey, value.take(100))
                is Int -> bundle.putLong(formattedKey, value.toLong())
                is Long -> bundle.putLong(formattedKey, value)
                is Float -> bundle.putDouble(formattedKey, value.toDouble())
                is Double -> bundle.putDouble(formattedKey, value)
                is Boolean -> bundle.putLong(formattedKey, if (value) 1L else 0L)
                else -> bundle.putString(formattedKey, value.toString().take(100))
            }
        }
        return bundle
    }

    fun formatFirebaseAnalyticsDataForSegment(properties: Map<String, Any?>): Properties {
        return buildJsonObject {
            for ((key, value) in properties) {
                val formattedKey = makeFirebaseAnalyticsKey(key)
                when (value) {
                    null -> Unit
                    is String -> put(formattedKey, value.take(100))
                    is Int -> put(formattedKey, value)
                    is Long -> put(formattedKey, value)
                    is Float -> put(formattedKey, value)
                    is Double -> put(formattedKey, value)
                    is Boolean -> put(formattedKey, value)
                    else -> put(formattedKey, value.toString().take(100))
                }
            }
        }
    }
}
