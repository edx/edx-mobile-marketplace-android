package org.openedx.whatsnew

import android.content.Context
import com.google.gson.Gson
import org.openedx.core.config.Config
import org.openedx.core.domain.model.Version
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.whatsnew.data.model.WhatsNewItem
import org.openedx.whatsnew.data.storage.WhatsNewPreferences

class WhatsNewManager(
    private val context: Context,
    private val config: Config,
    private val whatsNewPreferences: WhatsNewPreferences,
    private val appData: AppData
) : WhatsNewGlobalManager {
    fun getNewestData(): org.openedx.whatsnew.domain.model.WhatsNewItem {
        val jsonString = context.resources.openRawResource(R.raw.whats_new)
            .bufferedReader()
            .use { it.readText() }
        val whatsNewListData = Gson().fromJson(jsonString, Array<WhatsNewItem>::class.java)
        return whatsNewListData[0].mapToDomain(context)
    }

    override fun shouldShowWhatsNew(): Boolean {
        return try {
            val dataVersion = Version(getNewestData().version)
            val appVersion = Version(appData.versionName)
            if (whatsNewPreferences.lastWhatsNewVersion.isEmpty()) {
                appVersion.hasSameMajorMinorVersion(dataVersion)
            } else {
                val lastWhatsNewVersion = Version(whatsNewPreferences.lastWhatsNewVersion)
                lastWhatsNewVersion.isMinorVersionsDiff(appVersion) &&
                        appVersion.hasSameMajorMinorVersion(dataVersion)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
