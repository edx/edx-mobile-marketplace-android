package org.openedx.profile.presentation.appearance

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AppThemeMode
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileAnalyticsEvent
import org.openedx.profile.presentation.ProfileAnalyticsKey

class AppearanceSettingsViewModel(
    private val preferences: CorePreferences,
    private val analytics: ProfileAnalytics,
) : BaseViewModel() {

    private val _appThemeMode = MutableStateFlow(AppThemeMode.MATCH_DEVICE)
    val appThemeMode: StateFlow<AppThemeMode>
        get() = _appThemeMode

    init {
        _appThemeMode.value = preferences.appThemeMode
    }

    fun setAppThemeMode(context: Context, newMode: AppThemeMode) {
        val previousMode = _appThemeMode.value
        if (previousMode == newMode) return

        ThemeManager.applyThemeMode(context, newMode)

        preferences.appThemeMode = newMode
        _appThemeMode.value = newMode

        logAppThemeModeChangedEvent(previousMode, newMode)
    }

    private fun logAppThemeModeChangedEvent(
        previousMode: AppThemeMode,
        currentMode: AppThemeMode,
    ) {
        val event = ProfileAnalyticsEvent.APP_THEME_CHANGED
        analytics.logEvent(
            event.eventName,
            mapOf(
                ProfileAnalyticsKey.NAME.key to event.biValue,
                ProfileAnalyticsKey.CATEGORY.key to ProfileAnalyticsKey.PROFILE.key,
                ProfileAnalyticsKey.PREVIOUS_MODE.key to previousMode.value,
                ProfileAnalyticsKey.NEW_MODE.key to currentMode.value,
            )
        )
    }
}
