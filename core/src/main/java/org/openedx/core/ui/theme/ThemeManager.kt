package org.openedx.core.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import org.openedx.core.domain.model.AppThemeMode

object ThemeManager {
    fun applyThemeMode(context: Context, mode: AppThemeMode) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val uiMode = mode.toUiModeManagerConstant()
                val ui = context.getSystemService(UiModeManager::class.java)
                ui?.setApplicationNightMode(uiMode)
            }

            else -> {
                val delegateMode = mode.toAppCompatNightConstant()
                AppCompatDelegate.setDefaultNightMode(delegateMode)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun AppThemeMode.toUiModeManagerConstant(): Int = when (this) {
        AppThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        AppThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        AppThemeMode.MATCH_DEVICE -> UiModeManager.MODE_NIGHT_CUSTOM
    }

    private fun AppThemeMode.toAppCompatNightConstant(): Int = when (this) {
        AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        AppThemeMode.MATCH_DEVICE -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
