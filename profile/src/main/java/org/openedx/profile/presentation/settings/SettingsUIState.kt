package org.openedx.profile.presentation.settings

import org.openedx.profile.domain.model.Configuration

sealed class SettingsUIState {
    data class Data(
        val configuration: Configuration,
        val isDatadogEnabled: Boolean
    ) : SettingsUIState()

    object Loading : SettingsUIState()
}
