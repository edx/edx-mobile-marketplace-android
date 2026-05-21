package org.openedx.core.system.notifier.app

data class DatadogTrackingToggledEvent(
    val enabled: Boolean,
) : AppEvent

