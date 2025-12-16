package org.openedx.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.utils.Logger

object AppUpdateState {
    private val logger = Logger("AppUpdateState")

    var wasUpdateDialogDisplayed = false
    var wasUpgradeDialogClosed = mutableStateOf(false)
    var lastAppUpgradeEvent: AppUpgradeEvent? = null

    fun openPlayMarket(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=${context.packageName}".toUri()
                )
            )
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                )
            )
        }
    }

    data class AppUpgradeParameters(
        val appUpgradeEvent: AppUpgradeEvent? = null,
        val wasUpgradeDialogClosed: Boolean = AppUpdateState.wasUpgradeDialogClosed.value,
        val appUpgradeRecommendedDialog: () -> Unit = {},
        val onAppUpgradeRecommendedBoxClick: () -> Unit = {},
        val onAppUpgradeRequired: () -> Unit = {},
    )
}
