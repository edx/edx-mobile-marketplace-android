package org.openedx.notifications.presentation.settings

sealed class NotificationsSettingsUiState {
    data class Configuration(
        val showPermissionRequestDialog: Boolean = false,
        val discussionsPushEnabled: Boolean = false,
    ) : NotificationsSettingsUiState()
}
