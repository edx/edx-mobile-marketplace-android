package org.openedx.core.extension

import org.json.JSONObject

fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    keys().forEach { key ->
        map[key] = optString(key)
    }
    return map
}
