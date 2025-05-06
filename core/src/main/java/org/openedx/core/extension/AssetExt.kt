package org.openedx.core.extension

import android.content.res.AssetManager
import android.util.Log
import org.openedx.core.utils.Logger
import java.io.BufferedReader

fun AssetManager.readAsText(fileName: String): String? {
    return try {
        open(fileName).bufferedReader().use(BufferedReader::readText)
    } catch (e: Exception) {
        Log.e("AssetExt", "Unable to load file $fileName from assets")
        Logger("AssetManagerExt").e(throwable = e, metadata = mapOf("filename" to fileName))
        null
    }
}
