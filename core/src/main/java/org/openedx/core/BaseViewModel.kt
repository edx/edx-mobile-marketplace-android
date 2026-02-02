package org.openedx.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import org.openedx.foundation.system.ResourceManager

open class BaseViewModel(resourceManager: ResourceManager) : ViewModel(), DefaultLifecycleObserver