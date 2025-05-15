package org.openedx.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.utils.Logger

object AppUpdateState {
    private val logger = Logger("AppUpdateState")

    var wasUpdateDialogDisplayed = false
    var wasUpdateDialogClosed = mutableStateOf(false)

    fun openPlayMarket(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
        } catch (e: ActivityNotFoundException) {
            logger.e(throwable = e)
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                )
            )
        }
    }

    data class AppUpgradeParameters(
        val appUpgradeEvent: AppUpgradeEvent? = null,
        val wasUpdateDialogClosed: Boolean = AppUpdateState.wasUpdateDialogClosed.value,
        val appUpgradeRecommendedDialog: () -> Unit = {},
        val onAppUpgradeRecommendedBoxClick: () -> Unit = {},
        val onAppUpgradeRequired: () -> Unit = {},
    )
}
