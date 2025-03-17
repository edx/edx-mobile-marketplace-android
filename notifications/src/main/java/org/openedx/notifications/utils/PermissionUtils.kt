package org.openedx.notifications.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat

object PermissionUtils {

    fun requestNotificationPermission(
        activity: Activity,
        permissionLauncher: ActivityResultLauncher<String>,
        onSystemDialogShown: () -> Unit,
        onRationaleShown: () -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                onRationaleShown()
            } else {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                onSystemDialogShown()
            }
        } else {
            onRationaleShown()
        }
    }

    fun navigateToNotificationSettings(context: Context) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${context.packageName}")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
