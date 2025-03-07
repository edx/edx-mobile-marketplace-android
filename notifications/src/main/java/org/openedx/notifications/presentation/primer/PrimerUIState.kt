package org.openedx.notifications.presentation.primer

sealed class PrimerUIState {
    data object ShowDialog : PrimerUIState()
    data object HideDialog : PrimerUIState()
    data object DismissDialog : PrimerUIState()
    data object ShowRationaleDialog : PrimerUIState()
    data object Loading: PrimerUIState()
}
