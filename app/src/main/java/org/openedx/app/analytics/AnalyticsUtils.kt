package org.openedx.app.analytics

import android.os.Bundle
import com.braze.support.toBundle
import com.segment.analytics.kotlin.core.Properties
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AnalyticsUtils {
    fun makeFirebaseAnalyticsKey(value: String): String {
        return value.replace(Regex("[:\\- ]+"), "_").take(40)
    }

    fun formatFirebaseAnalyticsData(input: Map<String, Any?>): Bundle {
        return input.entries.associate { (key, value) ->
            // Format the key
            val formattedKey = makeFirebaseAnalyticsKey(key)
            // Truncate the string converted value to 100 characters
            val formattedValue = value.toString().take(100)
            // Return a Pair of the formatted key and value
            formattedKey to formattedValue
        }.toBundle()
    }

    fun formatFirebaseAnalyticsDataForSegment(properties: Properties): Properties {
        return buildJsonObject {
            for ((key, value) in properties) {
                put(
                    makeFirebaseAnalyticsKey(key),
                    value.toString().take(100)
                )
            }
        }
    }
}
